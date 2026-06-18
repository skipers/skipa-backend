-- Delete every row from reviews so the scheduler can create review targets again.
--
-- This intentionally keeps patents, annuities, legal statuses, reports, and departments intact.
-- DataGrip may warn on unconditional deletes, so the WHERE clause is explicit.
--
-- Run this whole file in DataGrip or psql.

begin;

delete from reviews
where id >= 1;

select setval(pg_get_serial_sequence('reviews', 'id'), 1, false);

commit;

