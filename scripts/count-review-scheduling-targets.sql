-- Count review scheduling targets by department.
-- Mirrors ReviewTargetSchedulingService:
--   - current review cycle
--   - UNPAID annuities due in the next quarter
--   - approved patents with active assigned departments
--   - excludes reviews already created for the current cycle/patent/department

with current_cycle as (
    select id
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
)
select
    d.id as department_id,
    d.name as department_name,
    count(*) as target_count
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
group by d.id, d.name
order by target_count desc, d.id;

