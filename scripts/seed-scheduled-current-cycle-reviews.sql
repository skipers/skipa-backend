-- Seed current-cycle reviews as "not requested yet" (SCHEDULED) from real target data.
--
-- Mirrors ReviewTargetSchedulingService:
--   - current active review cycle
--   - UNPAID annuities due in the next quarter
--   - approved patents with active assigned departments
--   - skips reviews already created for the same cycle/patent/department
--
-- Created reports are marked GENERATING, matching the scheduler's initial state.
-- Demo reports are identified by:
--   report_key like 'reports/scheduled-review-seed/%'
--
-- Run this whole file in DataGrip or psql.

begin;

with seed_report_ids as (
    select id
    from reports
    where report_key like 'reports/scheduled-review-seed/%'
)
delete from reviews
where report_id in (
    select id
    from seed_report_ids
);

delete from reports
where report_key like 'reports/scheduled-review-seed/%';

with current_cycle as (
    select id, deadline
    from review_cycles
    where current_date between start_date and end_date
    order by start_date desc
    limit 1
),
next_quarter as (
    select
        case
            when extract(quarter from current_date)::int = 4
                then make_date(extract(year from current_date)::int + 1, 1, 1)
            else make_date(
                    extract(year from current_date)::int,
                    extract(quarter from current_date)::int * 3 + 1,
                    1
            )
        end as start_date
),
next_quarter_range as (
    select
        start_date,
        (start_date + interval '3 months' - interval '1 day')::date as end_date
    from next_quarter
),
target_rows as (
    select
        p.id as patent_id,
        p.current_department_id as department_id,
        pa.id as patent_annuity_id,
        row_number() over (order by d.name, p.id, pa.due_date, pa.id) as seed_no
    from patent_annuities pa
    join patents p on p.id = pa.patent_id
    join departments d on d.id = p.current_department_id
    cross join current_cycle rc
    cross join next_quarter_range nq
    where pa.status = 'UNPAID'
      and pa.due_date between nq.start_date and nq.end_date
      and p.approval_status = 'APPROVED'
      and d.status = 'ACTIVE'
      and not exists (
          select 1
          from reviews r
          where r.review_cycle_id = rc.id
            and r.patent_id = p.id
            and r.department_id = d.id
      )
),
created_reports as (
    insert into reports (
        patent_id,
        report_key,
        status,
        evaluated_at,
        created_at,
        updated_at
    )
    select
        target_rows.patent_id,
        'reports/scheduled-review-seed/patent-' || target_rows.patent_id
            || '-annuity-' || target_rows.patent_annuity_id || '.html',
        'GENERATING',
        null,
        now(),
        now()
    from target_rows
    returning id, patent_id, report_key
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
    target_rows.patent_id,
    target_rows.department_id,
    current_cycle.id,
    created_reports.id,
    target_rows.patent_annuity_id,
    null,
    null,
    'SCHEDULED',
    null,
    coalesce(current_cycle.deadline, current_date + interval '14 days')::date,
    false,
    now(),
    now()
from target_rows
join created_reports
  on created_reports.patent_id = target_rows.patent_id
 and created_reports.report_key = 'reports/scheduled-review-seed/patent-' || target_rows.patent_id
     || '-annuity-' || target_rows.patent_annuity_id || '.html'
cross join current_cycle
on conflict on constraint uk_reviews_cycle_patent_department do nothing;

commit;
