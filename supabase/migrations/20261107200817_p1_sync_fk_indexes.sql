-- Phase 1 post-deploy performance review: cover all reported foreign keys.
-- Purely additive; safe to reapply and does not change records or access rules.
create index if not exists idx_audit_actor_id
  on public.audit_log(actor_id);
create index if not exists idx_calendar_approved_by
  on public.calendar_events(approved_by);
create index if not exists idx_calendar_recurrence_parent
  on public.calendar_events(recurrence_parent_id);
create index if not exists idx_calendar_term_id
  on public.calendar_events(term_id);
