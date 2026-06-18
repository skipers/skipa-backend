-- Seed demo patents and submitted reviews for /api/v1/portfolio/decisions.
--
-- This script creates 80 demo patents:
--   - recent 8 quarters from 2024Q3 through 2026Q2
--   - 10 submitted reviews per quarter
--   - departments are assigned round-robin from active departments
--   - review opinions are mixed differently by quarter
--
-- Demo rows are identified by:
--   application_number like 'DEMO-DECISION-%'
--   management_number = 'DEMO-PORTFOLIO-DECISION'
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

with seed_cycles(cycle_year, quarter, start_date, end_date) as (
    values
        (2024, 3, date '2024-07-01', date '2024-09-30'),
        (2024, 4, date '2024-10-01', date '2024-12-31'),
        (2025, 1, date '2025-01-01', date '2025-03-31'),
        (2025, 2, date '2025-04-01', date '2025-06-30'),
        (2025, 3, date '2025-07-01', date '2025-09-30'),
        (2025, 4, date '2025-10-01', date '2025-12-31'),
        (2026, 1, date '2026-01-01', date '2026-03-31'),
        (2026, 2, date '2026-04-01', date '2026-06-30')
)
insert into review_cycles (cycle_year, quarter, start_date, end_date, deadline, created_at, updated_at)
select
    cycle_year,
    quarter,
    start_date,
    end_date,
    (start_date + interval '2 months' + interval '14 days')::date,
    now(),
    now()
from seed_cycles
on conflict (cycle_year, quarter) do update
set start_date = excluded.start_date,
    end_date = excluded.end_date,
    deadline = excluded.deadline,
    updated_at = now();

with department_pool as (
    select
        id,
        name,
        row_number() over (order by id) as department_index,
        count(*) over () as department_count
    from departments
    where status = 'ACTIVE'
),
quarter_pool(quarter_index, cycle_year, quarter, start_date, end_date) as (
    values
        (1, 2024, 3, date '2024-07-01', date '2024-09-30'),
        (2, 2024, 4, date '2024-10-01', date '2024-12-31'),
        (3, 2025, 1, date '2025-01-01', date '2025-03-31'),
        (4, 2025, 2, date '2025-04-01', date '2025-06-30'),
        (5, 2025, 3, date '2025-07-01', date '2025-09-30'),
        (6, 2025, 4, date '2025-10-01', date '2025-12-31'),
        (7, 2026, 1, date '2026-01-01', date '2026-03-31'),
        (8, 2026, 2, date '2026-04-01', date '2026-06-30')
),
quarter_opinion_thresholds(quarter_index, maintain_count) as (
    values
        (1, 7),
        (2, 5),
        (3, 8),
        (4, 4),
        (5, 6),
        (6, 3),
        (7, 7),
        (8, 5)
),
seed_rows as (
    select
        (quarter_pool.quarter_index - 1) * 10 + review_slot as demo_no,
        quarter_pool.cycle_year,
        quarter_pool.quarter,
        quarter_pool.start_date,
        quarter_pool.end_date,
        (quarter_pool.start_date + ((review_slot - 1) * 8 + (quarter_pool.quarter_index % 3))::integer)::date as submitted_date,
        case
            when review_slot <= quarter_opinion_thresholds.maintain_count then 'MAINTAIN'
            else 'ABANDON'
        end as opinion,
        case ((review_slot + quarter_pool.quarter_index) % 5)
            when 0 then 'EXPIRED'
            when 1 then 'ABANDONED'
            when 2 then 'WITHDRAWN'
            when 3 then 'INVALIDATED'
            else 'REJECTED'
        end as inactive_status,
        case ((review_slot + quarter_pool.quarter_index) % 10)
            when 0 then 'AI'
            when 1 then 'ESG'
            when 2 then '금융전략'
            when 3 then '데이터'
            when 4 then '반도체'
            when 5 then '블록체인'
            when 6 then '솔루션'
            when 7 then '제조'
            when 8 then '클라우드'
            else '통신'
        end as tech_field
    from quarter_pool
    join quarter_opinion_thresholds
      on quarter_opinion_thresholds.quarter_index = quarter_pool.quarter_index
    cross join generate_series(1, 10) as slots(review_slot)
)
insert into patents (
    title,
    application_number,
    registration_number,
    application_date,
    registration_date,
    expiry_date,
    ipc_codes,
    cpc_codes,
    applicant,
    inventor,
    citation_count,
    original_pdf_key,
    parsed_json_key,
    management_number,
    business_field,
    tech_field,
    related_products,
    filing_country,
    is_joint_application,
    initial_department,
    current_department_id,
    keywords,
    summary,
    approval_status,
    created_at,
    updated_at
)
select
    '포트폴리오 의사결정 데모 특허 ' || lpad(seed_rows.demo_no::text, 3, '0'),
    'DEMO-DECISION-' || lpad(seed_rows.demo_no::text, 3, '0'),
    case
        when seed_rows.opinion = 'MAINTAIN' or seed_rows.inactive_status in ('EXPIRED', 'ABANDONED', 'INVALIDATED')
            then 'DDR-' || lpad(seed_rows.demo_no::text, 6, '0')
        else null
    end,
    (seed_rows.start_date - interval '6 years' + (seed_rows.demo_no % 90) * interval '1 day')::date,
    case
        when seed_rows.opinion = 'MAINTAIN' or seed_rows.inactive_status in ('EXPIRED', 'ABANDONED', 'INVALIDATED')
            then (seed_rows.start_date - interval '4 years' + (seed_rows.demo_no % 60) * interval '1 day')::date
        else null
    end,
    case
        when seed_rows.opinion = 'MAINTAIN'
            then (seed_rows.start_date + interval '14 years')::date
        else seed_rows.submitted_date
    end,
    jsonb_build_array('DEMO'),
    jsonb_build_array('DEMO'),
    'SK Demo',
    '김데모, 이시연',
    (seed_rows.demo_no % 17),
    'patents/demo-decisions/' || seed_rows.demo_no || '/original.pdf',
    'patents/demo-decisions/' || seed_rows.demo_no || '/parsed.json',
    'DEMO-PORTFOLIO-DECISION',
    department_pool.name,
    seed_rows.tech_field,
    jsonb_build_array('시연용', '포트폴리오'),
    case (seed_rows.demo_no % 5)
        when 0 then 'KR'
        when 1 then 'US'
        when 2 then 'JP'
        when 3 then 'CN'
        else 'EP'
    end,
    false,
    department_pool.name,
    department_pool.id,
    jsonb_build_array('시연', '의사결정', seed_rows.tech_field),
    seed_rows.cycle_year || '년 ' || seed_rows.quarter || '분기 포트폴리오 의사결정 시연용 특허입니다.',
    'APPROVED',
    now(),
    now()
from seed_rows
join department_pool
  on department_pool.department_index = ((seed_rows.demo_no - 1) % department_pool.department_count) + 1;

with demo_patents as (
    select
        id as patent_id,
        substring(application_number from 'DEMO-DECISION-([0-9]+)')::integer as demo_no
    from patents
    where application_number like 'DEMO-DECISION-%'
      and management_number = 'DEMO-PORTFOLIO-DECISION'
)
insert into reports (
    patent_id,
    report_key,
    status,
    total_score,
    value_grade,
    evaluated_at,
    created_at,
    updated_at
)
select
    patent_id,
    'reports/demo-decisions/patent-' || demo_no || '.html',
    'REPORT_COMPLETED',
    (60 + (demo_no % 40))::numeric(5, 2),
    case
        when 60 + (demo_no % 40) >= 90 then 'S'
        when 60 + (demo_no % 40) >= 80 then 'A'
        when 60 + (demo_no % 40) >= 70 then 'B'
        else 'C'
    end,
    now(),
    now(),
    now()
from demo_patents;

with quarter_pool(quarter_index, cycle_year, quarter, start_date, end_date) as (
    values
        (1, 2024, 3, date '2024-07-01', date '2024-09-30'),
        (2, 2024, 4, date '2024-10-01', date '2024-12-31'),
        (3, 2025, 1, date '2025-01-01', date '2025-03-31'),
        (4, 2025, 2, date '2025-04-01', date '2025-06-30'),
        (5, 2025, 3, date '2025-07-01', date '2025-09-30'),
        (6, 2025, 4, date '2025-10-01', date '2025-12-31'),
        (7, 2026, 1, date '2026-01-01', date '2026-03-31'),
        (8, 2026, 2, date '2026-04-01', date '2026-06-30')
),
quarter_opinion_thresholds(quarter_index, maintain_count) as (
    values
        (1, 7),
        (2, 5),
        (3, 8),
        (4, 4),
        (5, 6),
        (6, 3),
        (7, 7),
        (8, 5)
),
seed_rows as (
    select
        (quarter_pool.quarter_index - 1) * 10 + review_slot as demo_no,
        quarter_pool.cycle_year,
        quarter_pool.quarter,
        quarter_pool.start_date,
        (quarter_pool.start_date + ((review_slot - 1) * 8 + (quarter_pool.quarter_index % 3))::integer)::date as submitted_date,
        case
            when review_slot <= quarter_opinion_thresholds.maintain_count then 'MAINTAIN'
            else 'ABANDON'
        end as opinion
    from quarter_pool
    join quarter_opinion_thresholds
      on quarter_opinion_thresholds.quarter_index = quarter_pool.quarter_index
    cross join generate_series(1, 10) as slots(review_slot)
),
demo_patents as (
    select
        patents.id as patent_id,
        patents.current_department_id as department_id,
        substring(patents.application_number from 'DEMO-DECISION-([0-9]+)')::integer as demo_no
    from patents
    where patents.application_number like 'DEMO-DECISION-%'
      and patents.management_number = 'DEMO-PORTFOLIO-DECISION'
),
demo_reports as (
    select distinct on (reports.patent_id)
        reports.patent_id,
        reports.id as report_id
    from reports
    join demo_patents on demo_patents.patent_id = reports.patent_id
    order by reports.patent_id, reports.id desc
)
insert into reviews (
    patent_id,
    department_id,
    review_cycle_id,
    report_id,
    opinion,
    comment,
    status,
    submitted_at,
    due_date,
    checked,
    created_at,
    updated_at
)
select
    demo_patents.patent_id,
    demo_patents.department_id,
    review_cycles.id,
    demo_reports.report_id,
    seed_rows.opinion,
    case
        when seed_rows.opinion = 'MAINTAIN' then '핵심 사업과 연계성이 높아 유지 의견으로 제출합니다.'
        else '활용 가능성이 낮아 포기 의견으로 제출합니다.'
    end,
    'SUBMITTED',
    (seed_rows.submitted_date + time '10:00')::timestamptz,
    review_cycles.deadline,
    true,
    now(),
    now()
from seed_rows
join demo_patents on demo_patents.demo_no = seed_rows.demo_no
join demo_reports on demo_reports.patent_id = demo_patents.patent_id
join review_cycles
  on review_cycles.cycle_year = seed_rows.cycle_year
 and review_cycles.quarter = seed_rows.quarter
on conflict on constraint uk_reviews_cycle_patent_department do update
set report_id = excluded.report_id,
    opinion = excluded.opinion,
    comment = excluded.comment,
    status = excluded.status,
    submitted_at = excluded.submitted_at,
    due_date = excluded.due_date,
    checked = excluded.checked,
    updated_at = now();

with quarter_pool(quarter_index, cycle_year, quarter, start_date, end_date) as (
    values
        (1, 2024, 3, date '2024-07-01', date '2024-09-30'),
        (2, 2024, 4, date '2024-10-01', date '2024-12-31'),
        (3, 2025, 1, date '2025-01-01', date '2025-03-31'),
        (4, 2025, 2, date '2025-04-01', date '2025-06-30'),
        (5, 2025, 3, date '2025-07-01', date '2025-09-30'),
        (6, 2025, 4, date '2025-10-01', date '2025-12-31'),
        (7, 2026, 1, date '2026-01-01', date '2026-03-31'),
        (8, 2026, 2, date '2026-04-01', date '2026-06-30')
),
quarter_opinion_thresholds(quarter_index, maintain_count) as (
    values
        (1, 7),
        (2, 5),
        (3, 8),
        (4, 4),
        (5, 6),
        (6, 3),
        (7, 7),
        (8, 5)
),
seed_rows as (
    select
        (quarter_pool.quarter_index - 1) * 10 + review_slot as demo_no,
        quarter_pool.start_date,
        (quarter_pool.start_date + ((review_slot - 1) * 8 + (quarter_pool.quarter_index % 3))::integer)::date as submitted_date,
        case
            when review_slot <= quarter_opinion_thresholds.maintain_count then 'MAINTAIN'
            else 'ABANDON'
        end as opinion,
        case ((review_slot + quarter_pool.quarter_index) % 5)
            when 0 then 'EXPIRED'
            when 1 then 'ABANDONED'
            when 2 then 'WITHDRAWN'
            when 3 then 'INVALIDATED'
            else 'REJECTED'
        end as inactive_status
    from quarter_pool
    join quarter_opinion_thresholds
      on quarter_opinion_thresholds.quarter_index = quarter_pool.quarter_index
    cross join generate_series(1, 10) as slots(review_slot)
),
demo_patents as (
    select
        patents.id as patent_id,
        substring(patents.application_number from 'DEMO-DECISION-([0-9]+)')::integer as demo_no
    from patents
    where patents.application_number like 'DEMO-DECISION-%'
      and patents.management_number = 'DEMO-PORTFOLIO-DECISION'
),
status_rows as (
    select
        demo_patents.patent_id,
        'APPLIED' as status,
        (seed_rows.start_date - interval '6 years')::date as changed_at
    from demo_patents
    join seed_rows on seed_rows.demo_no = demo_patents.demo_no
    union all
    select
        demo_patents.patent_id,
        'REGISTERED' as status,
        (seed_rows.start_date - interval '4 years')::date as changed_at
    from demo_patents
    join seed_rows on seed_rows.demo_no = demo_patents.demo_no
    where seed_rows.opinion = 'MAINTAIN'
       or seed_rows.inactive_status in ('EXPIRED', 'ABANDONED', 'INVALIDATED')
    union all
    select
        demo_patents.patent_id,
        seed_rows.inactive_status as status,
        seed_rows.submitted_date as changed_at
    from demo_patents
    join seed_rows on seed_rows.demo_no = demo_patents.demo_no
    where seed_rows.opinion = 'ABANDON'
)
insert into patent_legal_status (
    patent_id,
    status,
    changed_at,
    created_at,
    updated_at
)
select
    patent_id,
    status,
    changed_at,
    now(),
    now()
from status_rows
order by patent_id, changed_at;

commit;
