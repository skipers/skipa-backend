-- One-off demo seed for deployed PostgreSQL databases.
-- Default password for inserted accounts is: 1234
-- This script is intentionally not a Flyway migration. Run it manually with psql.

begin;

insert into departments (name, status, created_at, updated_at)
values
    ('반도체', 'ACTIVE', now(), now()),
    ('통신', 'ACTIVE', now(), now()),
    ('제조', 'ACTIVE', now(), now())
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
        ('biz01', '사업부 담당자 1', 'biz01@sk.com', 'BUSINESS', '반도체'),
        ('biz02', '사업부 담당자 2', 'biz02@sk.com', 'BUSINESS', '반도체'),
        ('biz03', '사업부 담당자 3', 'biz03@sk.com', 'BUSINESS', '통신'),
        ('biz04', '사업부 담당자 4', 'biz04@sk.com', 'BUSINESS', '제조'),
        ('biz05', '사업부 담당자 5', 'biz05@sk.com', 'BUSINESS', '제조')
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
    from generate_series(2024, 2027) as years(year_value)
    cross join generate_series(1, 4) as quarters(quarter_value)
)
insert into review_cycles (cycle_year, quarter, start_date, end_date, deadline, created_at, updated_at)
select
    cycle_year,
    quarter,
    start_date,
    end_date,
    case
        when cycle_year = 2027 and quarter = 4 then null
        else (start_date + interval '2 months' + interval '14 days')::date
    end,
    now(),
    now()
from seed_cycles
on conflict (cycle_year, quarter) do update
set start_date = excluded.start_date,
    end_date = excluded.end_date,
    deadline = excluded.deadline,
    updated_at = now();

create temp table if not exists skipa_seed_patents (
    idx integer primary key,
    patent_id bigint not null,
    department_id bigint not null
) on commit drop;

truncate table skipa_seed_patents;

with source as (
    select
        idx,
        case (idx - 1) % 3
            when 0 then (select id from departments where name = '반도체')
            when 1 then (select id from departments where name = '통신')
            else (select id from departments where name = '제조')
        end as department_id,
        case (idx - 1) % 3
            when 0 then 'AI 반도체 전력 최적화 샘플 특허 ' || lpad(idx::text, 3, '0')
            when 1 then '5G/6G 네트워크 제어 샘플 특허 ' || lpad(idx::text, 3, '0')
            else '스마트팩토리 예지보전 샘플 특허 ' || lpad(idx::text, 3, '0')
        end as title,
        case (idx - 1) % 3
            when 0 then 'AI 반도체'
            when 1 then '무선 네트워크'
            else '제조 DX'
        end as business_field,
        case (idx - 1) % 3
            when 0 then case when idx % 2 = 0 then '패키징 방열' else '저전력 추론 가속' end
            when 1 then case when idx % 2 = 0 then '빔포밍' else '네트워크 슬라이싱' end
            else case when idx % 2 = 0 then '예지보전' else '공정 최적화' end
        end as tech_field,
        case (idx - 1) % 3
            when 0 then '반도체'
            when 1 then '통신'
            else '제조'
        end as department_name
    from generate_series(1, 50) as series(idx)
),
inserted as (
    insert into patents (
        title,
        application_number,
        registration_number,
        publication_number,
        application_date,
        registration_date,
        publication_date,
        expiry_date,
        ipc_codes,
        cpc_codes,
        applicant,
        inventor,
        citation_count,
        examination_claim_count,
        management_number,
        business_field,
        tech_field,
        related_products,
        filing_country,
        is_joint_application,
        joint_applicant,
        initial_department,
        current_department_id,
        keywords,
        summary,
        approval_status,
        created_at,
        updated_at
    )
    select
        source.title,
        '10-2026-' || lpad((100000 + source.idx)::text, 6, '0'),
        case when source.idx % 2 = 0 then '10-26' || lpad(source.idx::text, 6, '0') else null end,
        '10-2026-' || lpad((700000 + source.idx)::text, 6, '0'),
        make_date(2022 + source.idx % 4, source.idx % 12 + 1, source.idx % 24 + 1),
        case when source.idx % 2 = 0 then make_date(2025, source.idx % 12 + 1, source.idx % 24 + 1) else null end,
        make_date(2024, source.idx % 12 + 1, source.idx % 24 + 1),
        case (source.idx - 1) % 6
            when 0 then ('2026-06-11'::date + interval '1 month' + (source.idx % 20) * interval '1 day')::date
            when 1 then ('2026-06-11'::date + interval '4 months' + (source.idx % 20) * interval '1 day')::date
            when 2 then ('2026-06-11'::date + interval '9 months' + (source.idx % 20) * interval '1 day')::date
            when 3 then ('2026-06-11'::date + interval '24 months' + (source.idx % 20) * interval '1 day')::date
            when 4 then ('2026-06-11'::date + interval '48 months' + (source.idx % 20) * interval '1 day')::date
            else ('2026-06-11'::date + interval '72 months' + (source.idx % 20) * interval '1 day')::date
        end,
        jsonb_build_array(case when source.idx % 2 = 0 then 'G06N 3/08' else 'H04B 7/06' end, 'G05B 23/02'),
        jsonb_build_array(case when source.idx % 2 = 0 then 'G06N3/084' else 'H04B7/0617' end, 'G05B23/0243'),
        case source.idx % 3 when 0 then 'SK텔레콤' when 1 then 'SK하이닉스' else 'SK온' end,
        format('샘플 발명자 %s; 공동 발명자 %s', lpad(source.idx::text, 2, '0'), lpad((source.idx + 50)::text, 2, '0')),
        source.idx * 3 % 41,
        5 + source.idx % 12,
        'SKP-DEMO-' || lpad(source.idx::text, 3, '0'),
        source.business_field,
        source.tech_field,
        jsonb_build_array(source.business_field || ' 제품군', 'Demo Product ' || lpad(source.idx::text, 3, '0')),
        case when source.idx % 5 = 0 then 'US' else 'KR' end,
        source.idx % 7 = 0,
        case when source.idx % 7 = 0 then 'SK스퀘어' else null end,
        source.department_name,
        source.department_id,
        jsonb_build_array(
            case source.department_name when '반도체' then 'AI' when '통신' then '5G' else '스마트팩토리' end,
            source.department_name,
            source.tech_field
        ),
        source.department_name || '의 화면 검증을 위해 생성된 샘플 특허입니다. 검토, 보고서, 권리 상태, 연차료 데이터를 함께 가집니다.',
        'APPROVED',
        now(),
        now()
    from source
    on conflict (application_number) do update
    set title = excluded.title,
        current_department_id = excluded.current_department_id,
        approval_status = 'APPROVED',
        updated_at = now()
    returning id, application_number, current_department_id
)
insert into skipa_seed_patents (idx, patent_id, department_id)
select
    cast(right(inserted.application_number, 6) as integer) - 100000,
    inserted.id,
    inserted.current_department_id
from inserted;

insert into reports (patent_id, report_key, total_score, value_grade, status, evaluated_at, created_at, updated_at)
select
    seed.patent_id,
    'reports/sample/demo-' || lpad(seed.idx::text, 3, '0') || '.html',
    case when seed.idx % 10 = 0 or seed.idx % 15 = 0 then null else (60 + seed.idx % 35)::numeric(5, 2) end,
    case
        when seed.idx % 10 = 0 or seed.idx % 15 = 0 then null
        when seed.idx % 5 = 0 then 'S'
        when seed.idx % 3 = 0 then 'A'
        when seed.idx % 3 = 1 then 'B'
        else 'C'
    end,
    case
        when seed.idx % 15 = 0 then 'FAILED'
        when seed.idx % 10 = 0 then 'GENERATING'
        else 'EMBEDDING_COMPLETED'
    end,
    case when seed.idx % 10 = 0 or seed.idx % 15 = 0 then null else ('2026-04-01'::date + seed.idx)::timestamptz end,
    now(),
    now()
from skipa_seed_patents seed
where not exists (
    select 1
    from reports
    where reports.report_key = 'reports/sample/demo-' || lpad(seed.idx::text, 3, '0') || '.html'
);

insert into patent_legal_status (patent_id, status, changed_at, created_at, updated_at)
select seed.patent_id, status_value, changed_at, now(), now()
from skipa_seed_patents seed
cross join lateral (
    values
        ('APPLIED', '2023-01-01'::date + seed.idx * 9),
        (case when seed.idx % 2 = 0 then 'REGISTERED' else 'PUBLISHED' end, '2025-01-01'::date + seed.idx * 5),
        (
            case seed.idx % 8
                when 1 then 'ABANDONED'
                when 2 then 'EXPIRED'
                when 3 then 'WITHDRAWN'
                when 4 then 'EXPIRED'
                else null
            end,
            make_date(2022 + (seed.idx - 1) % 4, seed.idx % 12 + 1, seed.idx % 24 + 1)
        )
) as legal(status_value, changed_at)
where legal.status_value is not null
  and not exists (
    select 1
    from patent_legal_status existing
    where existing.patent_id = seed.patent_id
      and existing.status = legal.status_value
      and existing.changed_at = legal.changed_at
);

insert into patent_annuities (patent_id, start_year, end_year, due_date, paid_date, status, amount, created_at, updated_at)
select
    seed.patent_id,
    1,
    case when seed.idx % 3 = 0 then 3 else null end,
    '2026-05-15'::date + seed.idx % 75,
    case when seed.idx % 3 = 0 then '2026-05-01'::date + seed.idx % 20 else null end,
    case when seed.idx % 3 = 0 then 'PAID' else 'UNPAID' end,
    160000 + seed.idx * 12000,
    now(),
    now()
from skipa_seed_patents seed
where not exists (
    select 1
    from patent_annuities existing
    where existing.patent_id = seed.patent_id
      and existing.start_year = 1
);

with current_cycle as (
    select id from review_cycles where cycle_year = 2026 and quarter = 2
),
current_reviews as (
    select
        seed.idx,
        seed.patent_id,
        seed.department_id,
        current_cycle.id as review_cycle_id,
        current_cycle.deadline as deadline,
        case
            when seed.idx % 5 = 0 then 'SCHEDULED'
            when seed.idx % 4 = 0 then 'OVERDUE'
            when seed.idx % 6 between 1 and 3 then 'SUBMITTED'
            else 'PENDING'
        end as review_status
    from skipa_seed_patents seed
    cross join current_cycle
    where seed.idx % 7 <> 0
)
insert into reviews (
    patent_id,
    department_id,
    review_cycle_id,
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
    current_reviews.patent_id,
    current_reviews.department_id,
    current_reviews.review_cycle_id,
    case
        when current_reviews.review_status = 'SUBMITTED' and current_reviews.idx % 2 = 0 then 'MAINTAIN'
        when current_reviews.review_status = 'SUBMITTED' then 'ABANDON'
        else null
    end,
    case
        when current_reviews.review_status = 'SUBMITTED' and current_reviews.idx % 2 = 0 then '사업 연계성이 높아 유지가 필요합니다.'
        when current_reviews.review_status = 'SUBMITTED' then '대체 기술과 중복되어 포기 검토가 가능합니다.'
        else null
    end,
    current_reviews.review_status,
    case
        when current_reviews.review_status = 'SUBMITTED' then ('2026-06-01'::date + current_reviews.idx % 10)::timestamptz
        else null
    end,
    current_reviews.deadline,
    current_reviews.review_status = 'SUBMITTED' and current_reviews.idx % 2 = 0,
    now(),
    now()
from current_reviews
on conflict (review_cycle_id, patent_id, department_id) do update
set opinion = excluded.opinion,
    comment = excluded.comment,
    status = excluded.status,
    submitted_at = excluded.submitted_at,
    due_date = excluded.due_date,
    checked = excluded.checked,
    updated_at = now();

with past_cycles as (
    select
        id,
        cycle_year,
        quarter,
        end_date,
        deadline,
        row_number() over (order by cycle_year, quarter) as cycle_index
    from review_cycles
    where end_date < '2026-06-11'
      and cycle_year between 2024 and 2026
),
past_reviews as (
    select
        seed.idx,
        seed.patent_id,
        seed.department_id,
        past_cycles.id as review_cycle_id,
        past_cycles.cycle_year,
        past_cycles.quarter,
        past_cycles.end_date,
        past_cycles.deadline,
        past_cycles.cycle_index,
        offsets.offset_value,
        ((past_cycles.cycle_index - 1) * 3 + offsets.offset_value + 1) as history_index
    from past_cycles
    cross join generate_series(0, 2) as offsets(offset_value)
    join skipa_seed_patents seed
      on seed.idx = (((past_cycles.cycle_index - 1) * 3 + offsets.offset_value) % 50) + 1
)
insert into reviews (
    patent_id,
    department_id,
    review_cycle_id,
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
    past_reviews.patent_id,
    past_reviews.department_id,
    past_reviews.review_cycle_id,
    case when past_reviews.history_index % 2 = 0 then 'MAINTAIN' else 'ABANDON' end,
    past_reviews.cycle_year || '년 ' || past_reviews.quarter || '분기 이력 확인용 제출 의견입니다.',
    'SUBMITTED',
    (past_reviews.end_date - (15 - past_reviews.history_index % 10)::integer)::timestamptz,
    past_reviews.deadline,
    true,
    now(),
    now()
from past_reviews
on conflict (review_cycle_id, patent_id, department_id) do update
set opinion = excluded.opinion,
    comment = excluded.comment,
    status = excluded.status,
    submitted_at = excluded.submitted_at,
    due_date = excluded.due_date,
    checked = excluded.checked,
    updated_at = now();

commit;
