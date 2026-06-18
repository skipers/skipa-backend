-- Seed fallback reviews for the current active review cycle.
--
-- Use this only if the scheduler demo does not create enough rows.
-- It uses existing approved patents assigned to active departments, creates lightweight
-- demo reports, then inserts reviews for the current active review cycle.
--
-- Telecom department gets several unsubmitted rows so the business dashboard
-- still has pending/overdue work to show.
--
-- Demo reports are identified by:
--   report_key like 'reports/demo-review-fallback/%'
--
-- Run this whole file in DataGrip or psql.

begin;

with current_cycle as (
    select id, deadline
    from review_cycles
    where current_date between start_date and end_date
    order by start_date desc
    limit 1
),
fallback_report_ids as (
    select reports.id
    from reports
    where reports.report_key like 'reports/demo-review-fallback/%'
)
delete from reviews
where report_id in (
    select id
    from fallback_report_ids
);

delete from reports
where report_key like 'reports/demo-review-fallback/%';

with current_cycle as (
    select id, deadline
    from review_cycles
    where current_date between start_date and end_date
    order by start_date desc
    limit 1
),
ranked_patents as (
    select
        patents.id as patent_id,
        patents.current_department_id as department_id,
        departments.name as department_name,
        row_number() over (
            partition by patents.current_department_id
            order by patents.id
        ) as department_rank
    from patents
    join departments on departments.id = patents.current_department_id
    cross join current_cycle
    where patents.approval_status = 'APPROVED'
      and departments.status = 'ACTIVE'
      and not exists (
          select 1
          from reviews
          where reviews.review_cycle_id = current_cycle.id
            and reviews.patent_id = patents.id
            and reviews.department_id = patents.current_department_id
      )
),
selected_patents as (
    select
        patent_id,
        department_id,
        department_name,
        department_rank,
        row_number() over (order by department_name, department_rank, patent_id) as fallback_no
    from ranked_patents
    where (
            department_name = '통신 사업부'
            and department_rank <= 8
        )
       or (
            department_name <> '통신 사업부'
            and department_rank <= 3
        )
),
created_reports as (
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
        selected_patents.patent_id,
        'reports/demo-review-fallback/patent-' || selected_patents.patent_id || '.html',
        case
            when selected_patents.department_name = '통신 사업부'
                 and selected_patents.department_rank <= 2 then 'GENERATING'
            when selected_patents.fallback_no % 7 = 0 then 'GENERATING'
            when selected_patents.fallback_no % 5 = 0 then 'EMBEDDING_COMPLETED'
            else 'REPORT_COMPLETED'
        end,
        case
            when selected_patents.department_name = '통신 사업부'
                 and selected_patents.department_rank <= 2 then null
            when selected_patents.fallback_no % 7 = 0 then null
            else (70 + (selected_patents.fallback_no % 25))::numeric(5, 2)
        end,
        case
            when selected_patents.department_name = '통신 사업부'
                 and selected_patents.department_rank <= 2 then null
            when selected_patents.fallback_no % 7 = 0 then null
            when 70 + (selected_patents.fallback_no % 25) >= 90 then 'S'
            when 70 + (selected_patents.fallback_no % 25) >= 80 then 'A'
            else 'B'
        end,
        case
            when selected_patents.department_name = '통신 사업부'
                 and selected_patents.department_rank <= 2 then null
            when selected_patents.fallback_no % 7 = 0 then null
            else now()
        end,
        now(),
        now()
    from selected_patents
    returning id, patent_id
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
    selected_patents.patent_id,
    selected_patents.department_id,
    current_cycle.id,
    created_reports.id,
    case
        when selected_patents.department_name = '통신 사업부'
             and selected_patents.department_rank <= 6 then null
        when selected_patents.fallback_no % 4 = 0 then 'ABANDON'
        else 'MAINTAIN'
    end,
    case
        when selected_patents.department_name = '통신 사업부'
             and selected_patents.department_rank <= 6 then null
        when selected_patents.fallback_no % 4 = 0 then '활용 가능성이 낮아 포기 의견으로 제출합니다.'
        else '사업 연계성이 높아 유지 의견으로 제출합니다.'
    end,
    case
        when selected_patents.department_name = '통신 사업부'
             and selected_patents.department_rank in (1, 2) then 'OVERDUE'
        when selected_patents.department_name = '통신 사업부'
             and selected_patents.department_rank <= 6 then 'PENDING'
        when selected_patents.fallback_no % 6 = 0 then 'PENDING'
        else 'SUBMITTED'
    end,
    case
        when selected_patents.department_name = '통신 사업부'
             and selected_patents.department_rank <= 6 then null
        when selected_patents.fallback_no % 6 = 0 then null
        else now() - (selected_patents.fallback_no % 12) * interval '1 day'
    end,
    case
        when selected_patents.department_name = '통신 사업부'
             and selected_patents.department_rank in (1, 2) then (current_date - interval '2 days')::date
        else coalesce(current_cycle.deadline, current_date + interval '14 days')::date
    end,
    case
        when selected_patents.department_name = '통신 사업부'
             and selected_patents.department_rank <= 6 then false
        when selected_patents.fallback_no % 3 = 0 then false
        else true
    end,
    now(),
    now()
from selected_patents
join created_reports on created_reports.patent_id = selected_patents.patent_id
cross join current_cycle
on conflict on constraint uk_reviews_cycle_patent_department do update
set report_id = excluded.report_id,
    opinion = excluded.opinion,
    comment = excluded.comment,
    status = excluded.status,
    submitted_at = excluded.submitted_at,
    due_date = excluded.due_date,
    checked = excluded.checked,
    updated_at = now();

commit;
