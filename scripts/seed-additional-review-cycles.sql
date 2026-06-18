-- Adds review cycles outside the range created by scripts/seed-base-data.sql if needed.
-- seed-base-data.sql currently creates 2019 through 2032; this fills 2006 through 2018 and 2033 through 2040.

begin;

with additional_cycles as (
    select
        year_value as cycle_year,
        quarter_value as quarter,
        make_date(year_value, (quarter_value - 1) * 3 + 1, 1) as start_date,
        (make_date(year_value, (quarter_value - 1) * 3 + 1, 1) + interval '3 months' - interval '1 day')::date as end_date
    from (
        select year_value
        from generate_series(2006, 2018) as years(year_value)
        union all
        select year_value
        from generate_series(2033, 2040) as years(year_value)
    ) years
    cross join generate_series(1, 4) as quarters(quarter_value)
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
from additional_cycles
on conflict (cycle_year, quarter) do update
set start_date = excluded.start_date,
    end_date = excluded.end_date,
    deadline = excluded.deadline,
    updated_at = now();

commit;
