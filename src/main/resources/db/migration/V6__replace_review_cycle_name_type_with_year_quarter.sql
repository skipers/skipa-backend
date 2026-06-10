alter table review_cycles add column cycle_year integer;
alter table review_cycles add column quarter integer;

update review_cycles
set cycle_year = cast(extract(year from start_date) as integer),
    quarter = cast(extract(quarter from start_date) as integer);

alter table review_cycles alter column cycle_year set not null;
alter table review_cycles alter column quarter set not null;

alter table review_cycles drop constraint uk_review_cycles_name;
alter table review_cycles add constraint uk_review_cycles_year_quarter unique (cycle_year, quarter);

alter table review_cycles drop column name;
alter table review_cycles drop column type;
