export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.18"
  }
  public: {
    Tables: {
      academic_years: {
        Row: {
          created_at: string
          deleted_at: string | null
          end_date: string | null
          id: string
          is_current: boolean
          name: string
          school_id: string
          start_date: string | null
          sync_version: number
          updated_at: string
        }
        Insert: {
          created_at?: string
          deleted_at?: string | null
          end_date?: string | null
          id?: string
          is_current?: boolean
          name: string
          school_id: string
          start_date?: string | null
          sync_version?: number
          updated_at?: string
        }
        Update: {
          created_at?: string
          deleted_at?: string | null
          end_date?: string | null
          id?: string
          is_current?: boolean
          name?: string
          school_id?: string
          start_date?: string | null
          sync_version?: number
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "academic_years_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
        ]
      }
      audit_log: {
        Row: {
          action: string
          actor_id: string | null
          created_at: string
          deleted_at: string | null
          diff: Json | null
          entity: string | null
          entity_id: string | null
          id: string
          ip: string | null
          school_id: string
          sync_version: number
          updated_at: string
          user_agent: string | null
        }
        Insert: {
          action: string
          actor_id?: string | null
          created_at?: string
          deleted_at?: string | null
          diff?: Json | null
          entity?: string | null
          entity_id?: string | null
          id?: string
          ip?: string | null
          school_id: string
          sync_version?: number
          updated_at?: string
          user_agent?: string | null
        }
        Update: {
          action?: string
          actor_id?: string | null
          created_at?: string
          deleted_at?: string | null
          diff?: Json | null
          entity?: string | null
          entity_id?: string | null
          id?: string
          ip?: string | null
          school_id?: string
          sync_version?: number
          updated_at?: string
          user_agent?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "audit_log_actor_id_fkey"
            columns: ["actor_id"]
            isOneToOne: false
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "audit_log_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
        ]
      }
      calendar_events: {
        Row: {
          all_day: boolean
          approval_status: Database["public"]["Enums"]["approval_status"]
          approved_at: string | null
          approved_by: string | null
          attachments: Json
          audience_class_ids: string[] | null
          audience_roles: string[] | null
          category: Database["public"]["Enums"]["calendar_category"]
          colour: string | null
          created_at: string
          created_by: string
          deleted_at: string | null
          description: string | null
          end_at: string
          id: string
          is_published: boolean
          location: string | null
          push_sent: boolean
          recurrence_parent_id: string | null
          recurrence_rule: string | null
          rejection_reason: string | null
          school_id: string
          start_at: string
          sync_version: number
          term_id: string | null
          title: string
          title_i18n: Json | null
          updated_at: string
          visibility: Database["public"]["Enums"]["visibility_scope"]
        }
        Insert: {
          all_day?: boolean
          approval_status?: Database["public"]["Enums"]["approval_status"]
          approved_at?: string | null
          approved_by?: string | null
          attachments?: Json
          audience_class_ids?: string[] | null
          audience_roles?: string[] | null
          category?: Database["public"]["Enums"]["calendar_category"]
          colour?: string | null
          created_at?: string
          created_by: string
          deleted_at?: string | null
          description?: string | null
          end_at: string
          id?: string
          is_published?: boolean
          location?: string | null
          push_sent?: boolean
          recurrence_parent_id?: string | null
          recurrence_rule?: string | null
          rejection_reason?: string | null
          school_id: string
          start_at: string
          sync_version?: number
          term_id?: string | null
          title: string
          title_i18n?: Json | null
          updated_at?: string
          visibility?: Database["public"]["Enums"]["visibility_scope"]
        }
        Update: {
          all_day?: boolean
          approval_status?: Database["public"]["Enums"]["approval_status"]
          approved_at?: string | null
          approved_by?: string | null
          attachments?: Json
          audience_class_ids?: string[] | null
          audience_roles?: string[] | null
          category?: Database["public"]["Enums"]["calendar_category"]
          colour?: string | null
          created_at?: string
          created_by?: string
          deleted_at?: string | null
          description?: string | null
          end_at?: string
          id?: string
          is_published?: boolean
          location?: string | null
          push_sent?: boolean
          recurrence_parent_id?: string | null
          recurrence_rule?: string | null
          rejection_reason?: string | null
          school_id?: string
          start_at?: string
          sync_version?: number
          term_id?: string | null
          title?: string
          title_i18n?: Json | null
          updated_at?: string
          visibility?: Database["public"]["Enums"]["visibility_scope"]
        }
        Relationships: [
          {
            foreignKeyName: "calendar_events_approved_by_fkey"
            columns: ["approved_by"]
            isOneToOne: false
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "calendar_events_created_by_fkey"
            columns: ["created_by"]
            isOneToOne: false
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "calendar_events_recurrence_parent_id_fkey"
            columns: ["recurrence_parent_id"]
            isOneToOne: false
            referencedRelation: "calendar_events"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "calendar_events_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "calendar_events_term_id_fkey"
            columns: ["term_id"]
            isOneToOne: false
            referencedRelation: "terms"
            referencedColumns: ["id"]
          },
        ]
      }
      class_subject_teacher: {
        Row: {
          class_id: string
          created_at: string
          deleted_at: string | null
          id: string
          subject_id: string
          sync_version: number
          teacher_id: string
          term_id: string
          updated_at: string
        }
        Insert: {
          class_id: string
          created_at?: string
          deleted_at?: string | null
          id?: string
          subject_id: string
          sync_version?: number
          teacher_id: string
          term_id: string
          updated_at?: string
        }
        Update: {
          class_id?: string
          created_at?: string
          deleted_at?: string | null
          id?: string
          subject_id?: string
          sync_version?: number
          teacher_id?: string
          term_id?: string
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "class_subject_teacher_class_id_fkey"
            columns: ["class_id"]
            isOneToOne: false
            referencedRelation: "classes"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "class_subject_teacher_subject_id_fkey"
            columns: ["subject_id"]
            isOneToOne: false
            referencedRelation: "subjects"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "class_subject_teacher_teacher_id_fkey"
            columns: ["teacher_id"]
            isOneToOne: false
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "class_subject_teacher_term_id_fkey"
            columns: ["term_id"]
            isOneToOne: false
            referencedRelation: "terms"
            referencedColumns: ["id"]
          },
        ]
      }
      classes: {
        Row: {
          academic_year_id: string | null
          created_at: string
          deleted_at: string | null
          grade_level: string | null
          id: string
          name: string
          school_id: string
          stream: string | null
          sync_version: number
          teacher_id: string | null
          updated_at: string
        }
        Insert: {
          academic_year_id?: string | null
          created_at?: string
          deleted_at?: string | null
          grade_level?: string | null
          id?: string
          name: string
          school_id: string
          stream?: string | null
          sync_version?: number
          teacher_id?: string | null
          updated_at?: string
        }
        Update: {
          academic_year_id?: string | null
          created_at?: string
          deleted_at?: string | null
          grade_level?: string | null
          id?: string
          name?: string
          school_id?: string
          stream?: string | null
          sync_version?: number
          teacher_id?: string | null
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "classes_academic_year_id_fkey"
            columns: ["academic_year_id"]
            isOneToOne: false
            referencedRelation: "academic_years"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "classes_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "classes_teacher_id_fkey"
            columns: ["teacher_id"]
            isOneToOne: false
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
        ]
      }
      device_registry: {
        Row: {
          app_version: string | null
          created_at: string
          deleted_at: string | null
          device_id: string
          fcm_token: string | null
          id: string
          last_seen: string
          platform: string | null
          sync_version: number
          updated_at: string
          user_id: string
        }
        Insert: {
          app_version?: string | null
          created_at?: string
          deleted_at?: string | null
          device_id: string
          fcm_token?: string | null
          id?: string
          last_seen?: string
          platform?: string | null
          sync_version?: number
          updated_at?: string
          user_id: string
        }
        Update: {
          app_version?: string | null
          created_at?: string
          deleted_at?: string | null
          device_id?: string
          fcm_token?: string | null
          id?: string
          last_seen?: string
          platform?: string | null
          sync_version?: number
          updated_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "device_registry_user_id_fkey"
            columns: ["user_id"]
            isOneToOne: false
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
        ]
      }
      feature_flags: {
        Row: {
          created_at: string
          deleted_at: string | null
          id: string
          key: string
          school_id: string
          sync_version: number
          updated_at: string
          value: Json
        }
        Insert: {
          created_at?: string
          deleted_at?: string | null
          id?: string
          key: string
          school_id: string
          sync_version?: number
          updated_at?: string
          value?: Json
        }
        Update: {
          created_at?: string
          deleted_at?: string | null
          id?: string
          key?: string
          school_id?: string
          sync_version?: number
          updated_at?: string
          value?: Json
        }
        Relationships: [
          {
            foreignKeyName: "feature_flags_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
        ]
      }
      parent_profiles: {
        Row: {
          created_at: string
          deleted_at: string | null
          id: string
          national_id: string | null
          occupation: string | null
          preferred_channel: string
          relationship: string | null
          sync_version: number
          updated_at: string
          user_id: string
        }
        Insert: {
          created_at?: string
          deleted_at?: string | null
          id?: string
          national_id?: string | null
          occupation?: string | null
          preferred_channel?: string
          relationship?: string | null
          sync_version?: number
          updated_at?: string
          user_id: string
        }
        Update: {
          created_at?: string
          deleted_at?: string | null
          id?: string
          national_id?: string | null
          occupation?: string | null
          preferred_channel?: string
          relationship?: string | null
          sync_version?: number
          updated_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "parent_profiles_user_id_fkey"
            columns: ["user_id"]
            isOneToOne: true
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
        ]
      }
      schools: {
        Row: {
          contact_email: string | null
          contact_phone: string | null
          created_at: string
          currency: string
          deleted_at: string | null
          district: string | null
          emis_number: string | null
          id: string
          is_demo: boolean
          locale_default: string
          logo_url: string | null
          name: string
          province: string | null
          sync_version: number
          term_system: number
          theme: Json
          timezone: string
          type: string | null
          updated_at: string
        }
        Insert: {
          contact_email?: string | null
          contact_phone?: string | null
          created_at?: string
          currency?: string
          deleted_at?: string | null
          district?: string | null
          emis_number?: string | null
          id?: string
          is_demo?: boolean
          locale_default?: string
          logo_url?: string | null
          name: string
          province?: string | null
          sync_version?: number
          term_system?: number
          theme?: Json
          timezone?: string
          type?: string | null
          updated_at?: string
        }
        Update: {
          contact_email?: string | null
          contact_phone?: string | null
          created_at?: string
          currency?: string
          deleted_at?: string | null
          district?: string | null
          emis_number?: string | null
          id?: string
          is_demo?: boolean
          locale_default?: string
          logo_url?: string | null
          name?: string
          province?: string | null
          sync_version?: number
          term_system?: number
          theme?: Json
          timezone?: string
          type?: string | null
          updated_at?: string
        }
        Relationships: []
      }
      staff_profiles: {
        Row: {
          classes: string[] | null
          created_at: string
          deleted_at: string | null
          employee_no: string | null
          hire_date: string | null
          id: string
          qualifications: string | null
          subjects: string[] | null
          sync_version: number
          updated_at: string
          user_id: string
        }
        Insert: {
          classes?: string[] | null
          created_at?: string
          deleted_at?: string | null
          employee_no?: string | null
          hire_date?: string | null
          id?: string
          qualifications?: string | null
          subjects?: string[] | null
          sync_version?: number
          updated_at?: string
          user_id: string
        }
        Update: {
          classes?: string[] | null
          created_at?: string
          deleted_at?: string | null
          employee_no?: string | null
          hire_date?: string | null
          id?: string
          qualifications?: string | null
          subjects?: string[] | null
          sync_version?: number
          updated_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "staff_profiles_user_id_fkey"
            columns: ["user_id"]
            isOneToOne: true
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
        ]
      }
      student_guardian_link: {
        Row: {
          can_pay_fees: boolean
          created_at: string
          deleted_at: string | null
          guardian_user_id: string
          id: string
          is_primary: boolean
          student_user_id: string
          sync_version: number
          updated_at: string
        }
        Insert: {
          can_pay_fees?: boolean
          created_at?: string
          deleted_at?: string | null
          guardian_user_id: string
          id?: string
          is_primary?: boolean
          student_user_id: string
          sync_version?: number
          updated_at?: string
        }
        Update: {
          can_pay_fees?: boolean
          created_at?: string
          deleted_at?: string | null
          guardian_user_id?: string
          id?: string
          is_primary?: boolean
          student_user_id?: string
          sync_version?: number
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "student_guardian_link_guardian_user_id_fkey"
            columns: ["guardian_user_id"]
            isOneToOne: false
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "student_guardian_link_student_user_id_fkey"
            columns: ["student_user_id"]
            isOneToOne: false
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
        ]
      }
      student_profiles: {
        Row: {
          admission_no: string | null
          class_id: string | null
          created_at: string
          deleted_at: string | null
          dob: string | null
          enrolled_at: string | null
          house: string | null
          id: string
          stream: string | null
          sync_version: number
          updated_at: string
          user_id: string
        }
        Insert: {
          admission_no?: string | null
          class_id?: string | null
          created_at?: string
          deleted_at?: string | null
          dob?: string | null
          enrolled_at?: string | null
          house?: string | null
          id?: string
          stream?: string | null
          sync_version?: number
          updated_at?: string
          user_id: string
        }
        Update: {
          admission_no?: string | null
          class_id?: string | null
          created_at?: string
          deleted_at?: string | null
          dob?: string | null
          enrolled_at?: string | null
          house?: string | null
          id?: string
          stream?: string | null
          sync_version?: number
          updated_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "student_profiles_user_id_fkey"
            columns: ["user_id"]
            isOneToOne: true
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
        ]
      }
      subjects: {
        Row: {
          code: string | null
          created_at: string
          curriculum: Database["public"]["Enums"]["curriculum"]
          deleted_at: string | null
          grade_levels: string[] | null
          id: string
          name: string
          school_id: string
          sync_version: number
          updated_at: string
        }
        Insert: {
          code?: string | null
          created_at?: string
          curriculum?: Database["public"]["Enums"]["curriculum"]
          deleted_at?: string | null
          grade_levels?: string[] | null
          id?: string
          name: string
          school_id: string
          sync_version?: number
          updated_at?: string
        }
        Update: {
          code?: string | null
          created_at?: string
          curriculum?: Database["public"]["Enums"]["curriculum"]
          deleted_at?: string | null
          grade_levels?: string[] | null
          id?: string
          name?: string
          school_id?: string
          sync_version?: number
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "subjects_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
        ]
      }
      sync_change_log: {
        Row: {
          entity: string
          entity_id: string
          recorded_at: string
          row_data: Json
          school_id: string
          seq: number
        }
        Insert: {
          entity: string
          entity_id: string
          recorded_at?: string
          row_data: Json
          school_id: string
          seq?: never
        }
        Update: {
          entity?: string
          entity_id?: string
          recorded_at?: string
          row_data?: Json
          school_id?: string
          seq?: never
        }
        Relationships: [
          {
            foreignKeyName: "sync_change_log_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
        ]
      }
      sync_op_log: {
        Row: {
          client_ts: string | null
          conflict_detail: Json | null
          created_at: string
          deleted_at: string | null
          device_id: string
          entity: string
          entity_id: string
          id: string
          op_id: string
          op_type: string
          result: string
          school_id: string
          server_row: Json | null
          server_ts: string
          sync_version: number
          updated_at: string
          user_id: string
        }
        Insert: {
          client_ts?: string | null
          conflict_detail?: Json | null
          created_at?: string
          deleted_at?: string | null
          device_id: string
          entity: string
          entity_id: string
          id?: string
          op_id: string
          op_type: string
          result: string
          school_id: string
          server_row?: Json | null
          server_ts?: string
          sync_version?: number
          updated_at?: string
          user_id: string
        }
        Update: {
          client_ts?: string | null
          conflict_detail?: Json | null
          created_at?: string
          deleted_at?: string | null
          device_id?: string
          entity?: string
          entity_id?: string
          id?: string
          op_id?: string
          op_type?: string
          result?: string
          school_id?: string
          server_row?: Json | null
          server_ts?: string
          sync_version?: number
          updated_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "sync_op_log_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "sync_op_log_user_id_fkey"
            columns: ["user_id"]
            isOneToOne: false
            referencedRelation: "users"
            referencedColumns: ["id"]
          },
        ]
      }
      terms: {
        Row: {
          academic_year_id: string
          created_at: string
          deleted_at: string | null
          end_date: string | null
          id: string
          name: string
          school_id: string
          start_date: string | null
          sync_version: number
          term_number: number
          updated_at: string
        }
        Insert: {
          academic_year_id: string
          created_at?: string
          deleted_at?: string | null
          end_date?: string | null
          id?: string
          name: string
          school_id: string
          start_date?: string | null
          sync_version?: number
          term_number: number
          updated_at?: string
        }
        Update: {
          academic_year_id?: string
          created_at?: string
          deleted_at?: string | null
          end_date?: string | null
          id?: string
          name?: string
          school_id?: string
          start_date?: string | null
          sync_version?: number
          term_number?: number
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "terms_academic_year_id_fkey"
            columns: ["academic_year_id"]
            isOneToOne: false
            referencedRelation: "academic_years"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "terms_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
        ]
      }
      users: {
        Row: {
          avatar_url: string | null
          created_at: string
          deleted_at: string | null
          email: string | null
          full_name: string
          id: string
          is_active: boolean
          last_login_at: string | null
          phone: string
          preferred_lang: string
          role: Database["public"]["Enums"]["user_role"]
          school_id: string
          sync_version: number
          updated_at: string
        }
        Insert: {
          avatar_url?: string | null
          created_at?: string
          deleted_at?: string | null
          email?: string | null
          full_name: string
          id: string
          is_active?: boolean
          last_login_at?: string | null
          phone: string
          preferred_lang?: string
          role: Database["public"]["Enums"]["user_role"]
          school_id: string
          sync_version?: number
          updated_at?: string
        }
        Update: {
          avatar_url?: string | null
          created_at?: string
          deleted_at?: string | null
          email?: string | null
          full_name?: string
          id?: string
          is_active?: boolean
          last_login_at?: string | null
          phone?: string
          preferred_lang?: string
          role?: Database["public"]["Enums"]["user_role"]
          school_id?: string
          sync_version?: number
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "users_school_id_fkey"
            columns: ["school_id"]
            isOneToOne: false
            referencedRelation: "schools"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      sync_calendar_operation: {
        Args: {
          p_base_version: number
          p_client_ts: string
          p_device_id: string
          p_entity_id: string
          p_op_id: string
          p_op_type: string
          p_payload: Json
        }
        Returns: Json
      }
      sync_current_cursor: { Args: never; Returns: string }
      sync_pull_calendar: {
        Args: { p_cursor?: number; p_limit?: number }
        Returns: Json
      }
    }
    Enums: {
      approval_status: "draft" | "pending" | "approved" | "rejected"
      calendar_category:
        | "academic"
        | "exam"
        | "sports"
        | "cultural"
        | "admin"
        | "holiday"
        | "meeting"
        | "trip"
        | "other"
      curriculum: "zimsec" | "cambridge"
      user_role: "admin" | "head" | "teacher" | "student" | "parent" | "bursar"
      visibility_scope:
        | "all"
        | "staff"
        | "students"
        | "parents"
        | "admin"
        | "custom"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends (DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never) = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends (PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never) = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {
      approval_status: ["draft", "pending", "approved", "rejected"],
      calendar_category: [
        "academic",
        "exam",
        "sports",
        "cultural",
        "admin",
        "holiday",
        "meeting",
        "trip",
        "other",
      ],
      curriculum: ["zimsec", "cambridge"],
      user_role: ["admin", "head", "teacher", "student", "parent", "bursar"],
      visibility_scope: [
        "all",
        "staff",
        "students",
        "parents",
        "admin",
        "custom",
      ],
    },
  },
} as const
