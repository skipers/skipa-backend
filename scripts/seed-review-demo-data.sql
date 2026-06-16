-- Review demo data seed for deployed PostgreSQL databases.
-- Step 3 must be run after:
--   1. scripts/seed-base-data.sql
--   2. external patent/minio loading script
--
-- Assumptions:
--   - patents table contains demo patents with ids 1 through 185.
--   - departments and review_cycles were created by scripts/seed-base-data.sql.
--   - The current demo cycle is selected by current_date between start_date and end_date.
--
-- This script fills patent_annuities, patent_legal_status, reports, and reviews.
-- It intentionally creates a mixed current-cycle dashboard state:
--   - requested but pending
--   - requested and overdue
--   - submitted/completed reviews
--   - scheduled/not requested reviews
--   - generating, failed, and not-started reports

begin;

do $$
declare
    patent_count integer;
    current_cycle_count integer;
begin
    select count(*) into patent_count
    from patents
    where id between 1 and 185;

    if patent_count < 185 then
        raise exception 'Expected patents with ids 1..185, but found % rows.', patent_count;
    end if;

    select count(*) into current_cycle_count
    from review_cycles
    where current_date between start_date and end_date;

    if current_cycle_count = 0 then
        raise exception 'No review cycle contains current_date %. Run seed-base-data.sql first.', current_date;
    end if;
end $$;

insert into patent_annuities (patent_id, start_year, end_year, due_date, paid_date, status, amount, created_at, updated_at)
select
    patents.id,
    1,
    case
        when patents.id % 5 in (0, 1) then 3
        when patents.id % 11 = 0 then 1
        else null
    end,
    (current_date + ((patents.id % 180) - 60) * interval '1 day')::date,
    case
        when patents.id % 5 in (0, 1) then (current_date - (patents.id % 30) * interval '1 day')::date
        else null
    end,
    case
        when patents.id % 11 = 0 then 'ABANDONED'
        when patents.id % 5 in (0, 1) then 'PAID'
        else 'UNPAID'
    end,
    120000 + patents.id * 7000,
    now(),
    now()
from patents
where patents.id between 1 and 185
  and not exists (
    select 1
    from patent_annuities existing
    where existing.patent_id = patents.id
      and existing.start_year = 1
  );

insert into patent_legal_status (patent_id, status, changed_at, created_at, updated_at)
select patent_statuses.patent_id, patent_statuses.status, patent_statuses.changed_at, now(), now()
from (
    select
        patents.id as patent_id,
        legal_status.status,
        legal_status.changed_at
    from patents
    cross join lateral (
        values
            ('APPLIED', (current_date - interval '5 years' + (patents.id % 365) * interval '1 day')::date),
            (
                case
                    when patents.id % 4 = 0 then 'REGISTERED'
                    else 'PUBLISHED'
                end,
                (current_date - interval '2 years' + (patents.id % 240) * interval '1 day')::date
            ),
            (
                case
                    when patents.id % 23 = 0 then 'EXPIRED'
                    when patents.id % 29 = 0 then 'ABANDONED'
                    when patents.id % 31 = 0 then 'WITHDRAWN'
                    when patents.id % 37 = 0 then 'REJECTED'
                    else null
                end,
                (current_date - (patents.id % 120) * interval '1 day')::date
            )
    ) as legal_status(status, changed_at)
    where patents.id between 1 and 185
      and legal_status.status is not null
) patent_statuses
where not exists (
    select 1
    from patent_legal_status existing
    where existing.patent_id = patent_statuses.patent_id
      and existing.status = patent_statuses.status
      and existing.changed_at = patent_statuses.changed_at
);

with desired_reports as (
    select
        patents.id as patent_id,
        'reports/seed/review-demo/patent-' || lpad(patents.id::text, 3, '0') || '.html' as report_key,
        case
            when patents.id between 131 and 145 then 'GENERATING'
            when patents.id between 146 and 155 then 'FAILED'
            when patents.id between 71 and 85 then 'REPORT_COMPLETED'
            else 'EMBEDDING_COMPLETED'
        end as status,
        case
            when patents.id between 131 and 155 then null
            else (55 + patents.id % 42)::numeric(5, 2)
        end as total_score,
        case
            when patents.id between 131 and 155 then null
            when patents.id % 10 in (0, 1) then 'S'
            when patents.id % 10 in (2, 3, 4) then 'A'
            when patents.id % 10 in (5, 6, 7) then 'B'
            else 'C'
        end as value_grade,
        case
            when patents.id between 131 and 155 then null
            else (current_date - (patents.id % 25) * interval '1 day')::timestamptz
        end as evaluated_at
    from patents
    where patents.id between 1 and 155
),
inserted_reports as (
    insert into reports (patent_id, report_key, total_score, value_grade, status, evaluated_at, created_at, updated_at)
    select
        desired_reports.patent_id,
        desired_reports.report_key,
        desired_reports.total_score,
        desired_reports.value_grade,
        desired_reports.status,
        desired_reports.evaluated_at,
        now(),
        now()
    from desired_reports
    where not exists (
        select 1
        from reports existing
        where existing.report_key = desired_reports.report_key
    )
    returning report_key
)
update reports
set patent_id = desired_reports.patent_id,
    total_score = desired_reports.total_score,
    value_grade = desired_reports.value_grade,
    status = desired_reports.status,
    evaluated_at = desired_reports.evaluated_at,
    updated_at = now()
from desired_reports
where reports.report_key = desired_reports.report_key;

with current_cycle as (
    select id, coalesce(deadline, current_date + interval '14 days')::date as deadline
    from review_cycles
    where current_date between start_date and end_date
    order by start_date desc
    limit 1
),
department_pool as (
    select
        departments.id,
        row_number() over (order by departments.id) as department_index,
        count(*) over () as department_count
    from departments
),
desired_reviews as (
    select
        patents.id as patent_id,
        coalesce(patents.current_department_id, department_pool.id) as department_id,
        current_cycle.id as review_cycle_id,
        reports.id as report_id,
        patent_annuities.id as patent_annuity_id,
        case
            when patents.id between 1 and 30 then 'SCHEDULED'
            when patents.id between 31 and 70 then 'PENDING'
            when patents.id between 71 and 85 then 'OVERDUE'
            when patents.id between 86 and 130 then 'SUBMITTED'
            else 'SCHEDULED'
        end as status,
        case
            when patents.id between 86 and 130 and patents.id % 3 = 0 then 'ABANDON'
            when patents.id between 86 and 130 then 'MAINTAIN'
            else null
        end as opinion,
        case
            when patents.id between 86 and 130 and patents.id % 3 = 0 then '사업 연계성이 낮아 포기 검토가 가능합니다.'
            when patents.id between 86 and 130 then '핵심 제품과 연계되어 유지가 필요합니다.'
            else null
        end as comment,
        case
            when patents.id between 86 and 130 then (current_date - (patents.id % 12) * interval '1 day')::timestamptz
            else null
        end as submitted_at,
        case
            when patents.id between 71 and 85 then (current_date - (patents.id % 7 + 1) * interval '1 day')::date
            else current_cycle.deadline
        end as due_date,
        patents.id between 86 and 130 and patents.id % 4 = 0 as checked
    from patents
    cross join current_cycle
    left join department_pool
      on department_pool.department_index = ((patents.id - 1) % department_pool.department_count) + 1
    left join reports
      on reports.report_key = 'reports/seed/review-demo/patent-' || lpad(patents.id::text, 3, '0') || '.html'
    left join patent_annuities
      on patent_annuities.patent_id = patents.id
     and patent_annuities.start_year = 1
    where patents.id between 1 and 185
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
    desired_reviews.patent_id,
    desired_reviews.department_id,
    desired_reviews.review_cycle_id,
    desired_reviews.report_id,
    desired_reviews.patent_annuity_id,
    desired_reviews.opinion,
    desired_reviews.comment,
    desired_reviews.status,
    desired_reviews.submitted_at,
    desired_reviews.due_date,
    desired_reviews.checked,
    now(),
    now()
from desired_reviews
on conflict (review_cycle_id, patent_id, department_id) do update
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
