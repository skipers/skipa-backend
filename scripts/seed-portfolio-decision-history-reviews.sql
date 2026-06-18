-- Seed submitted reviews for existing patents so /api/v1/portfolio/decisions
-- can show maintain/abandon trends across the recent 8 quarters.
--
-- This script:
--   - uses existing patents
--   - creates missing review cycles for the recent 8 quarters
--   - assigns one SUBMITTED review per patent
--   - varies maintain/abandon ratios by quarter
--   - keeps abandon below one third in every quarter
--
-- Seed rows are identified by:
--   comment like '[PORTFOLIO_DECISION_HISTORY_SEED]%'
--
-- Run this whole file in DataGrip or psql.

begin;

delete from reviews
where comment like '[PORTFOLIO_DECISION_HISTORY_SEED]%';

with current_quarter as (
    select date_trunc('quarter', current_date)::date as start_date
),
quarter_pool as (
    select
        row_number() over (order by quarter_start)::integer as quarter_index,
        extract(year from quarter_start)::integer as cycle_year,
        extract(quarter from quarter_start)::integer as quarter,
        quarter_start::date as start_date,
        (quarter_start + interval '3 months' - interval '1 day')::date as end_date
    from current_quarter
    cross join generate_series(
        current_quarter.start_date - interval '21 months',
        current_quarter.start_date,
        interval '3 months'
    ) as quarters(quarter_start)
)
insert into review_cycles (
    cycle_year,
    quarter,
    start_date,
    end_date,
    deadline,
    created_at,
    updated_at
)
select
    cycle_year,
    quarter,
    start_date,
    end_date,
    (start_date + interval '2 months' + interval '14 days')::date,
    now(),
    now()
from quarter_pool
on conflict (cycle_year, quarter) do update
set start_date = excluded.start_date,
    end_date = excluded.end_date,
    deadline = excluded.deadline,
    updated_at = now();

with current_quarter as (
    select date_trunc('quarter', current_date)::date as start_date
),
quarter_pool as (
    select
        row_number() over (order by quarter_start)::integer as quarter_index,
        extract(year from quarter_start)::integer as cycle_year,
        extract(quarter from quarter_start)::integer as quarter,
        quarter_start::date as start_date,
        (quarter_start + interval '3 months' - interval '1 day')::date as end_date
    from current_quarter
    cross join generate_series(
        current_quarter.start_date - interval '21 months',
        current_quarter.start_date,
        interval '3 months'
    ) as quarters(quarter_start)
),
fallback_department as (
    select id
    from departments
    order by id
    limit 1
),
patent_pool as (
    select
        p.id as patent_id,
        coalesce(p.current_department_id, fallback_department.id) as department_id,
        row_number() over (order by p.id)::integer as patent_index
    from patents p
    cross join fallback_department
),
assigned_patents as (
    select
        patent_pool.patent_id,
        patent_pool.department_id,
        ((patent_pool.patent_index - 1) % 8 + 1)::integer as quarter_index,
        ((patent_pool.patent_index - 1) / 8 + 1)::integer as slot_in_quarter
    from patent_pool
),
seed_rows as (
    select
        assigned_patents.patent_id,
        assigned_patents.department_id,
        quarter_pool.cycle_year,
        quarter_pool.quarter,
        quarter_pool.start_date,
        quarter_pool.end_date,
        least(
            quarter_pool.start_date + ((assigned_patents.slot_in_quarter - 1) * 3 + (quarter_pool.quarter_index % 5))::integer,
            quarter_pool.end_date - 1
        )::date as submitted_date,
        case
            when quarter_pool.quarter_index = 1 and assigned_patents.slot_in_quarter % 5 = 0 then 'ABANDON'
            when quarter_pool.quarter_index = 2 and assigned_patents.slot_in_quarter % 10 in (0, 3, 7) then 'ABANDON'
            when quarter_pool.quarter_index = 3 and assigned_patents.slot_in_quarter % 8 = 0 then 'ABANDON'
            when quarter_pool.quarter_index = 4 and assigned_patents.slot_in_quarter % 4 = 0 then 'ABANDON'
            when quarter_pool.quarter_index = 5 and assigned_patents.slot_in_quarter % 13 in (0, 4, 8, 12) then 'ABANDON'
            when quarter_pool.quarter_index = 6 and assigned_patents.slot_in_quarter % 11 in (0, 6) then 'ABANDON'
            when quarter_pool.quarter_index = 7 and assigned_patents.slot_in_quarter % 7 in (0, 3) then 'ABANDON'
            when quarter_pool.quarter_index = 8 and assigned_patents.slot_in_quarter % 9 in (0, 4) then 'ABANDON'
            else 'MAINTAIN'
        end as opinion
    from assigned_patents
    join quarter_pool on quarter_pool.quarter_index = assigned_patents.quarter_index
    where assigned_patents.department_id is not null
),
latest_reports as (
    select distinct on (reports.patent_id)
        reports.patent_id,
        reports.id as report_id
    from reports
    where reports.status in ('REPORT_COMPLETED', 'EMBEDDING_COMPLETED')
    order by reports.patent_id, reports.id desc
)
insert into reviews (
    patent_id,
    department_id,
    review_cycle_id,
    report_id,
    patent_annuity_id,
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
    seed_rows.patent_id,
    seed_rows.department_id,
    review_cycles.id,
    latest_reports.report_id,
    null,
    seed_rows.opinion,
    case
        when seed_rows.opinion = 'MAINTAIN'
            then '[PORTFOLIO_DECISION_HISTORY_SEED] 사업 연계성과 활용 가능성이 확인되어 유지 의견으로 제출함.'
        else '[PORTFOLIO_DECISION_HISTORY_SEED] 활용 가능성과 사업 연계성이 낮아 포기 의견으로 제출함.'
    end,
    'SUBMITTED',
    (seed_rows.submitted_date + time '10:00')::timestamptz,
    coalesce(review_cycles.deadline, review_cycles.end_date - 14),
    true,
    now(),
    now()
from seed_rows
join review_cycles
  on review_cycles.cycle_year = seed_rows.cycle_year
 and review_cycles.quarter = seed_rows.quarter
left join latest_reports on latest_reports.patent_id = seed_rows.patent_id
on conflict on constraint uk_reviews_cycle_patent_department do update
set report_id = excluded.report_id,
    patent_annuity_id = excluded.patent_annuity_id,
    opinion = excluded.opinion,
    comment = excluded.comment,
    status = excluded.status,
    submitted_at = excluded.submitted_at,
    due_date = excluded.due_date,
    checked = excluded.checked,
    updated_at = now();

commit;

-- Verification: trend counts by submitted quarter.
select
    extract(year from submitted_at)::integer || 'Q' || extract(quarter from submitted_at)::integer as submitted_quarter,
    count(*) as total_count,
    count(*) filter (where opinion = 'MAINTAIN') as maintain_count,
    count(*) filter (where opinion = 'ABANDON') as abandon_count,
    round(count(*) filter (where opinion = 'ABANDON')::numeric / nullif(count(*), 0), 3) as abandon_ratio
from reviews
where comment like '[PORTFOLIO_DECISION_HISTORY_SEED]%'
group by 1
order by 1;
