-- Phase 21: Supabase public.users.role remains the sole LMS designation source.
-- The Supabase Table Editor can assign roles; signed-in clients must not use
-- a role update to become admin/head, edit their own role or cross schools.
-- Privileged SQL Editor / secret service-role provisioning is deliberate.
create or replace function app_private.control_user_designation()
returns trigger language plpgsql security definer
set search_path = ''
as $function$
declare
 v_actor uuid := auth.uid();
 v_actor_role public.user_role;
 v_actor_school uuid;
 v_actor_active boolean;
 v_actor_deleted timestamptz;
 v_jwt_role text := nullif(current_setting('request.jwt.claim.role',true),'');
 v_trusted boolean;
begin
 -- SQL Editor (postgres) or a verified server-side service-role client only.
 -- The ordinary browser uses a signed-in user JWT and goes through checks.
 v_trusted := v_actor is null and (
   coalesce(v_jwt_role='service_role',false) or
   (coalesce(current_setting('role',true),'')='service_role' and session_user='authenticator') or
   (session_user in ('postgres','supabase_admin')
     and coalesce(current_setting('role',true),'none') in ('none','postgres'))
 );
 if v_actor is not null then
   select role,school_id,is_active,deleted_at
   into v_actor_role,v_actor_school,v_actor_active,v_actor_deleted
   from public.users where id=v_actor;
 end if;
 if tg_op='INSERT' then
   if not v_trusted and (
      v_actor is null or v_actor_role is distinct from 'admin'::public.user_role
      or v_actor_active is distinct from true or v_actor_deleted is not null
      or new.school_id is distinct from v_actor_school
      or new.role in ('admin','head')
   ) then
     raise exception 'Only the school administrator may provision ordinary school roles'
       using errcode='42501';
   end if;
   return new;
 end if;

 if new.role is not distinct from old.role then return new; end if;
 if old.school_id is distinct from new.school_id or old.id is distinct from new.id then
   raise exception 'Role updates cannot change school or user identity'
     using errcode='23514';
 end if;
 if not v_trusted and (
   v_actor is null or v_actor_role is distinct from 'admin'::public.user_role
   or v_actor_active is distinct from true or v_actor_deleted is not null
   or v_actor_school is distinct from old.school_id
   or v_actor=old.id or old.role in ('admin','head')
   or new.role in ('admin','head') or new.deleted_at is not null
 ) then
   raise exception 'This designation change requires a school administrator or trusted provisioning'
     using errcode='42501';
 end if;

 if old.role='admin' and old.is_active and old.deleted_at is null
  and not exists (
   select 1 from public.users u where u.school_id=old.school_id
    and u.id<>old.id and u.role='admin'
    and u.is_active and u.deleted_at is null
  ) then
   raise exception 'Assign another active administrator before changing the last administrator'
     using errcode='23514';
 end if;

 -- Preserve history; refuse to silently transfer a teacher's live classes.
 -- An authenticated school admin must clear teaching duties first.
 -- Trusted provisioning can revoke/restore a role immediately, without
 -- retaining teacher access while the account has a different role.
 if not v_trusted and (
   exists(
     select 1 from public.classes c
     where c.teacher_id=old.id and c.deleted_at is null
   ) or exists(
     select 1 from public.class_subject_teacher a
     where a.teacher_id=old.id and a.deleted_at is null
   )
 ) then
   raise exception 'Reassign current teaching duties before changing this user''s designation'
     using errcode='23514';
 end if;

 -- Archive incompatible profiles and revoke family links. Their school
 -- records remain in history; returning to a designation does not silently
 -- re-enable former guardian access or class enrolment.
 if old.role='student' then
   update public.student_profiles
     set deleted_at=now(),class_id=null
     where user_id=old.id and deleted_at is null;
   update public.student_guardian_link
     set deleted_at=now()
     where student_user_id=old.id and deleted_at is null;
 elsif old.role='parent' then
   update public.parent_profiles set deleted_at=now()
     where user_id=old.id and deleted_at is null;
   update public.student_guardian_link set deleted_at=now()
     where guardian_user_id=old.id and deleted_at is null;
 elsif old.role in ('teacher','head','admin','bursar')
   and new.role not in ('teacher','head','admin','bursar') then
   update public.staff_profiles set deleted_at=now()
     where user_id=old.id and deleted_at is null;
 end if;

 if new.role='student' then
   insert into public.student_profiles(user_id,class_id)
     values(new.id,null)
   on conflict(user_id) do update
     set deleted_at=null,class_id=null;
 elsif new.role='parent' then
   insert into public.parent_profiles(user_id) values(new.id)
   on conflict(user_id) do update set deleted_at=null;
 elsif new.role in ('teacher','head','admin','bursar') then
   insert into public.staff_profiles(user_id) values(new.id)
   on conflict(user_id) do update set deleted_at=null;
 end if;

 insert into public.audit_log(school_id,actor_id,action,entity,entity_id,diff)
 values(new.school_id,v_actor,'user_role_changed','users',new.id,
   jsonb_build_object('previous_role',old.role::text,'new_role',new.role::text));

 return new;
end
$function$;

revoke all on function app_private.control_user_designation()
 from public,anon,authenticated;

drop trigger if exists trg_users_role_provision on public.users;
create trigger trg_users_role_provision
before insert on public.users for each row
execute function app_private.control_user_designation();

drop trigger if exists trg_users_designation_change on public.users;
create trigger trg_users_designation_change
before update of role on public.users for each row
when (old.role is distinct from new.role)
execute function app_private.control_user_designation();
