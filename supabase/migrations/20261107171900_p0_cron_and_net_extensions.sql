-- P0-T15: foundational extensions prescribed in Part C.
-- No background jobs are scheduled in Phase 0.
create extension if not exists pg_cron with schema pg_catalog;
create extension if not exists pg_net with schema extensions;
