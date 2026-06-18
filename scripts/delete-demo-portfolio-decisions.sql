-- Delete demo portfolio decision rows seeded by scripts/seed-demo-portfolio-decisions.sql.
--
-- Only rows with application_number like 'DEMO-DECISION-%'
-- or management_number = 'DEMO-PORTFOLIO-DECISION' are removed.
--
-- Run this whole file in DataGrip or psql.

begin;

create temp table demo_portfolio_decision_patent_ids (
    id bigint primary key
) on commit drop;

insert into demo_portfolio_decision_patent_ids (id)
select id
from patents
where application_number like 'DEMO-DECISION-%'
   or management_number = 'DEMO-PORTFOLIO-DECISION';

delete from reviews
where patent_id in (
    select id
    from demo_portfolio_decision_patent_ids
);

delete from reports
where patent_id in (
    select id
    from demo_portfolio_decision_patent_ids
);

delete from patent_annuities
where patent_id in (
    select id
    from demo_portfolio_decision_patent_ids
);

delete from patent_legal_status
where patent_id in (
    select id
    from demo_portfolio_decision_patent_ids
);

delete from patents
where id in (
    select id
    from demo_portfolio_decision_patent_ids
);

commit;

