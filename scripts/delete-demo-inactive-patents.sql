-- Delete demo inactive patents seeded by scripts/seed-demo-inactive-patents.sql.
--
-- Only rows with application_number like 'DEMO-INACTIVE-%' or management_number = 'DEMO-INACTIVE' are removed.
-- Run this whole file in DataGrip or psql.

begin;

create temp table demo_inactive_patent_ids (
    id bigint primary key
) on commit drop;

insert into demo_inactive_patent_ids (id)
select id
from patents
where application_number like 'DEMO-INACTIVE-%'
   or management_number = 'DEMO-INACTIVE';

delete from reviews
where patent_id in (
    select id
    from demo_inactive_patent_ids
);

delete from reports
where patent_id in (
    select id
    from demo_inactive_patent_ids
);

delete from patent_annuities
where patent_id in (
    select id
    from demo_inactive_patent_ids
);

delete from patent_legal_status
where patent_id in (
    select id
    from demo_inactive_patent_ids
);

delete from patents
where id in (
    select id
    from demo_inactive_patent_ids
);

commit;

