-- Delete submitted reviews seeded by scripts/seed-portfolio-decision-history-reviews.sql.
--
-- Only rows identified by this comment prefix are removed:
--   '[PORTFOLIO_DECISION_HISTORY_SEED]%'
--
-- Run this whole file in DataGrip or psql.

begin;

delete from reviews
where comment like '[PORTFOLIO_DECISION_HISTORY_SEED]%';

commit;
