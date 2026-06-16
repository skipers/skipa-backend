-- Base data seed for deployed PostgreSQL databases.
-- Step 1 inserts the foundational tables: departments, users, and review cycles.
-- Default password for inserted accounts is: 1234
-- This script is intentionally not a Flyway migration. Run it manually with psql.

begin;

insert into departments (name, status, created_at, updated_at)
values
    ('AI 사업부', 'ACTIVE', now(), now()),
    ('ESG 사업부', 'ACTIVE', now(), now()),
    ('금융전략 사업부', 'ACTIVE', now(), now()),
    ('데이터 사업부', 'ACTIVE', now(), now()),
    ('반도체 사업부', 'ACTIVE', now(), now()),
    ('블록체인 사업부', 'ACTIVE', now(), now()),
    ('솔루션 사업부', 'ACTIVE', now(), now()),
    ('제조 사업부', 'ACTIVE', now(), now()),
    ('클라우드 사업부', 'ACTIVE', now(), now()),
    ('통신 사업부', 'ACTIVE', now(), now())
on conflict (name) do update
set status = 'ACTIVE',
    updated_at = now();

with seed_users(login_id, name, email, role, department_name) as (
    values
        ('admin', '관리자', 'admin@sk.com', 'ADMIN', null),
        ('legal01', '법무 담당자 1', 'legal01@sk.com', 'LEGAL', null),
        ('legal02', '법무 담당자 2', 'legal02@sk.com', 'LEGAL', null),
        ('legal03', '법무 담당자 3', 'legal03@sk.com', 'LEGAL', null),
        ('legal04', '법무 담당자 4', 'legal04@sk.com', 'LEGAL', null),
        ('legal05', '법무 담당자 5', 'legal05@sk.com', 'LEGAL', null),
        ('legal06', '법무 담당자 6', 'legal06@sk.com', 'LEGAL', null),
        ('legal07', '법무 담당자 7', 'legal07@sk.com', 'LEGAL', null),
        ('legal08', '법무 담당자 8', 'legal08@sk.com', 'LEGAL', null),
        ('legal09', '법무 담당자 9', 'legal09@sk.com', 'LEGAL', null),
        ('legal10', '법무 담당자 10', 'legal10@sk.com', 'LEGAL', null),
        ('biz01', '사업부 담당자 1', 'biz01@sk.com', 'BUSINESS', 'AI 사업부'),
        ('biz02', '사업부 담당자 2', 'biz02@sk.com', 'BUSINESS', 'ESG 사업부'),
        ('biz03', '사업부 담당자 3', 'biz03@sk.com', 'BUSINESS', '금융전략 사업부'),
        ('biz04', '사업부 담당자 4', 'biz04@sk.com', 'BUSINESS', '데이터 사업부'),
        ('biz05', '사업부 담당자 5', 'biz05@sk.com', 'BUSINESS', '반도체 사업부'),
        ('biz06', '사업부 담당자 6', 'biz06@sk.com', 'BUSINESS', '블록체인 사업부'),
        ('biz07', '사업부 담당자 7', 'biz07@sk.com', 'BUSINESS', '솔루션 사업부'),
        ('biz08', '사업부 담당자 8', 'biz08@sk.com', 'BUSINESS', '제조 사업부'),
        ('biz09', '사업부 담당자 9', 'biz09@sk.com', 'BUSINESS', '클라우드 사업부'),
        ('biz10', '사업부 담당자 10', 'biz10@sk.com', 'BUSINESS', '통신 사업부')
)
insert into users (login_id, name, email, password, role, department_id, status, created_at, updated_at)
select
    seed_users.login_id,
    seed_users.name,
    seed_users.email,
    '$2a$10$hFlVtKBIyXMaJv1Y.nyQm.qAdPcaxj5XupeE4b9EGIDJHcOB4rPD2',
    seed_users.role,
    departments.id,
    'ACTIVE',
    now(),
    now()
from seed_users
left join departments on departments.name = seed_users.department_name
on conflict (login_id) do update
set name = excluded.name,
    email = excluded.email,
    role = excluded.role,
    department_id = excluded.department_id,
    status = 'ACTIVE',
    updated_at = now();

with seed_cycles as (
    select
        year_value as cycle_year,
        quarter_value as quarter,
        make_date(year_value, (quarter_value - 1) * 3 + 1, 1) as start_date,
        (make_date(year_value, (quarter_value - 1) * 3 + 1, 1) + interval '3 months' - interval '1 day')::date as end_date
    from generate_series(2019, 2032) as years(year_value)
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
from seed_cycles
on conflict (cycle_year, quarter) do update
set start_date = excluded.start_date,
    end_date = excluded.end_date,
    deadline = excluded.deadline,
    updated_at = now();

commit;
