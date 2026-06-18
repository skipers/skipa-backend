-- Seed demo patents whose latest legal status is inactive.
-- This is separate from real KIPRIS history and is intended only for dashboard/demo scenarios.
--
-- Demo rows are identified by:
--   application_number like 'DEMO-INACTIVE-%'
--   management_number = 'DEMO-INACTIVE'
--
-- Run this whole file in DataGrip or psql.

begin;

with department_pool as (
    select
        id,
        row_number() over (order by id) as department_index,
        count(*) over () as department_count
    from departments
    where status = 'ACTIVE'
),
seed_patents(
    demo_no,
    title,
    application_number,
    registration_number,
    application_date,
    registration_date,
    expiry_date,
    tech_field,
    business_field,
    filing_country,
    summary
) as (
    values
        (1, '데모 소멸 특허 01', 'DEMO-INACTIVE-001', 'DREG-000001', date '2016-03-10', date '2018-06-15', date '2024-03-31', 'AI', '데모 포트폴리오', 'KR', '시연용 소멸 특허입니다.'),
        (2, '데모 포기 특허 02', 'DEMO-INACTIVE-002', 'DREG-000002', date '2016-05-22', date '2019-01-18', date '2036-05-22', '반도체', '데모 포트폴리오', 'KR', '시연용 포기 특허입니다.'),
        (3, '데모 무효 특허 03', 'DEMO-INACTIVE-003', 'DREG-000003', date '2017-02-14', date '2019-09-03', date '2037-02-14', '블록체인', '데모 포트폴리오', 'US', '시연용 무효 특허입니다.'),
        (4, '데모 취하 특허 04', 'DEMO-INACTIVE-004', null, date '2017-08-01', null, null, '클라우드', '데모 포트폴리오', 'KR', '시연용 취하 특허입니다.'),
        (5, '데모 거절 특허 05', 'DEMO-INACTIVE-005', null, date '2018-01-19', null, null, '데이터', '데모 포트폴리오', 'JP', '시연용 거절 특허입니다.'),
        (6, '데모 소멸 특허 06', 'DEMO-INACTIVE-006', 'DREG-000006', date '2018-04-12', date '2020-07-20', date '2025-01-31', '통신', '데모 포트폴리오', 'KR', '시연용 소멸 특허입니다.'),
        (7, '데모 포기 특허 07', 'DEMO-INACTIVE-007', 'DREG-000007', date '2018-10-05', date '2021-02-10', date '2038-10-05', '제조', '데모 포트폴리오', 'CN', '시연용 포기 특허입니다.'),
        (8, '데모 무효 특허 08', 'DEMO-INACTIVE-008', 'DREG-000008', date '2019-03-27', date '2021-06-09', date '2039-03-27', '금융전략', '데모 포트폴리오', 'KR', '시연용 무효 특허입니다.'),
        (9, '데모 취하 특허 09', 'DEMO-INACTIVE-009', null, date '2019-07-16', null, null, 'ESG', '데모 포트폴리오', 'KR', '시연용 취하 특허입니다.'),
        (10, '데모 거절 특허 10', 'DEMO-INACTIVE-010', null, date '2019-11-04', null, null, '솔루션', '데모 포트폴리오', 'EP', '시연용 거절 특허입니다.'),
        (11, '데모 소멸 특허 11', 'DEMO-INACTIVE-011', 'DREG-000011', date '2020-02-08', date '2022-05-13', date '2025-06-30', 'AI', '데모 포트폴리오', 'KR', '시연용 소멸 특허입니다.'),
        (12, '데모 포기 특허 12', 'DEMO-INACTIVE-012', 'DREG-000012', date '2020-06-21', date '2022-09-28', date '2040-06-21', '반도체', '데모 포트폴리오', 'US', '시연용 포기 특허입니다.'),
        (13, '데모 무효 특허 13', 'DEMO-INACTIVE-013', 'DREG-000013', date '2020-12-15', date '2023-03-24', date '2040-12-15', '블록체인', '데모 포트폴리오', 'KR', '시연용 무효 특허입니다.'),
        (14, '데모 취하 특허 14', 'DEMO-INACTIVE-014', null, date '2021-04-02', null, null, '클라우드', '데모 포트폴리오', 'JP', '시연용 취하 특허입니다.'),
        (15, '데모 거절 특허 15', 'DEMO-INACTIVE-015', null, date '2021-09-09', null, null, '데이터', '데모 포트폴리오', 'KR', '시연용 거절 특허입니다.'),
        (16, '데모 소멸 특허 16', 'DEMO-INACTIVE-016', 'DREG-000016', date '2021-12-03', date '2023-08-17', date '2026-02-28', '통신', '데모 포트폴리오', 'KR', '시연용 소멸 특허입니다.'),
        (17, '데모 포기 특허 17', 'DEMO-INACTIVE-017', 'DREG-000017', date '2022-03-18', date '2024-01-26', date '2042-03-18', '제조', '데모 포트폴리오', 'CN', '시연용 포기 특허입니다.'),
        (18, '데모 무효 특허 18', 'DEMO-INACTIVE-018', 'DREG-000018', date '2022-08-25', date '2024-05-02', date '2042-08-25', '금융전략', '데모 포트폴리오', 'KR', '시연용 무효 특허입니다.'),
        (19, '데모 취하 특허 19', 'DEMO-INACTIVE-019', null, date '2022-11-11', null, null, 'ESG', '데모 포트폴리오', 'KR', '시연용 취하 특허입니다.'),
        (20, '데모 거절 특허 20', 'DEMO-INACTIVE-020', null, date '2023-02-23', null, null, '솔루션', '데모 포트폴리오', 'US', '시연용 거절 특허입니다.'),
        (21, '데모 소멸 특허 21', 'DEMO-INACTIVE-021', 'DREG-000021', date '2023-06-06', date '2024-11-19', date '2026-05-31', 'AI', '데모 포트폴리오', 'KR', '시연용 소멸 특허입니다.'),
        (22, '데모 포기 특허 22', 'DEMO-INACTIVE-022', 'DREG-000022', date '2023-09-14', date '2025-01-09', date '2043-09-14', '반도체', '데모 포트폴리오', 'KR', '시연용 포기 특허입니다.'),
        (23, '데모 취하 특허 23', 'DEMO-INACTIVE-023', null, date '2024-01-30', null, null, '클라우드', '데모 포트폴리오', 'KR', '시연용 취하 특허입니다.'),
        (24, '데모 거절 특허 24', 'DEMO-INACTIVE-024', null, date '2024-04-18', null, null, '데이터', '데모 포트폴리오', 'KR', '시연용 거절 특허입니다.')
),
upserted_patents as (
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
        seed_patents.title,
        seed_patents.application_number,
        seed_patents.registration_number,
        seed_patents.application_date,
        seed_patents.registration_date,
        seed_patents.expiry_date,
        jsonb_build_array('DEMO') as ipc_codes,
        jsonb_build_array('DEMO') as cpc_codes,
        'SK Demo' as applicant,
        '데모 발명자' as inventor,
        0 as citation_count,
        'patents/demo-inactive/' || seed_patents.demo_no || '/original.pdf' as original_pdf_key,
        'patents/demo-inactive/' || seed_patents.demo_no || '/parsed.json' as parsed_json_key,
        'DEMO-INACTIVE' as management_number,
        seed_patents.business_field,
        seed_patents.tech_field,
        jsonb_build_array('시연용') as related_products,
        seed_patents.filing_country,
        false as is_joint_application,
        departments.name as initial_department,
        departments.id as current_department_id,
        jsonb_build_array('시연', '비활성', seed_patents.tech_field) as keywords,
        seed_patents.summary,
        'APPROVED' as approval_status,
        now(),
        now()
    from seed_patents
    join department_pool
      on department_pool.department_index = ((seed_patents.demo_no - 1) % department_pool.department_count) + 1
    join departments
      on departments.id = department_pool.id
    on conflict (application_number) do update
    set title = excluded.title,
        registration_number = excluded.registration_number,
        application_date = excluded.application_date,
        registration_date = excluded.registration_date,
        expiry_date = excluded.expiry_date,
        ipc_codes = excluded.ipc_codes,
        cpc_codes = excluded.cpc_codes,
        applicant = excluded.applicant,
        inventor = excluded.inventor,
        citation_count = excluded.citation_count,
        original_pdf_key = excluded.original_pdf_key,
        parsed_json_key = excluded.parsed_json_key,
        management_number = excluded.management_number,
        business_field = excluded.business_field,
        tech_field = excluded.tech_field,
        related_products = excluded.related_products,
        filing_country = excluded.filing_country,
        is_joint_application = excluded.is_joint_application,
        initial_department = excluded.initial_department,
        current_department_id = excluded.current_department_id,
        keywords = excluded.keywords,
        summary = excluded.summary,
        approval_status = excluded.approval_status,
        updated_at = now()
    returning id, application_number
),
demo_patents as (
    select patents.id, seed_patents.demo_no
    from patents
    join seed_patents
      on seed_patents.application_number = patents.application_number
)
delete from patent_legal_status
where patent_id in (
    select id
    from demo_patents
);

with seed_statuses(demo_no, status_order, status, changed_at) as (
    values
        (1, 1, 'APPLIED', date '2016-03-10'), (1, 2, 'PUBLISHED', date '2017-09-10'), (1, 3, 'REGISTERED', date '2018-06-15'), (1, 4, 'EXPIRED', date '2024-07-31'),
        (2, 1, 'APPLIED', date '2016-05-22'), (2, 2, 'PUBLISHED', date '2017-11-22'), (2, 3, 'REGISTERED', date '2019-01-18'), (2, 4, 'ABANDONED', date '2024-08-31'),
        (3, 1, 'APPLIED', date '2017-02-14'), (3, 2, 'PUBLISHED', date '2018-08-14'), (3, 3, 'REGISTERED', date '2019-09-03'), (3, 4, 'INVALIDATED', date '2024-09-30'),
        (4, 1, 'APPLIED', date '2017-08-01'), (4, 2, 'PUBLISHED', date '2019-02-01'), (4, 3, 'WITHDRAWN', date '2024-10-31'),
        (5, 1, 'APPLIED', date '2018-01-19'), (5, 2, 'PUBLISHED', date '2019-07-19'), (5, 3, 'REJECTED', date '2024-11-30'),
        (6, 1, 'APPLIED', date '2018-04-12'), (6, 2, 'PUBLISHED', date '2019-10-12'), (6, 3, 'REGISTERED', date '2020-07-20'), (6, 4, 'EXPIRED', date '2024-12-31'),
        (7, 1, 'APPLIED', date '2018-10-05'), (7, 2, 'PUBLISHED', date '2020-04-05'), (7, 3, 'REGISTERED', date '2021-02-10'), (7, 4, 'ABANDONED', date '2025-01-31'),
        (8, 1, 'APPLIED', date '2019-03-27'), (8, 2, 'PUBLISHED', date '2020-09-27'), (8, 3, 'REGISTERED', date '2021-06-09'), (8, 4, 'INVALIDATED', date '2025-02-28'),
        (9, 1, 'APPLIED', date '2019-07-16'), (9, 2, 'PUBLISHED', date '2021-01-16'), (9, 3, 'WITHDRAWN', date '2025-03-31'),
        (10, 1, 'APPLIED', date '2019-11-04'), (10, 2, 'PUBLISHED', date '2021-05-04'), (10, 3, 'REJECTED', date '2025-04-30'),
        (11, 1, 'APPLIED', date '2020-02-08'), (11, 2, 'PUBLISHED', date '2021-08-08'), (11, 3, 'REGISTERED', date '2022-05-13'), (11, 4, 'EXPIRED', date '2025-05-31'),
        (12, 1, 'APPLIED', date '2020-06-21'), (12, 2, 'PUBLISHED', date '2021-12-21'), (12, 3, 'REGISTERED', date '2022-09-28'), (12, 4, 'ABANDONED', date '2025-06-30'),
        (13, 1, 'APPLIED', date '2020-12-15'), (13, 2, 'PUBLISHED', date '2022-06-15'), (13, 3, 'REGISTERED', date '2023-03-24'), (13, 4, 'INVALIDATED', date '2025-07-31'),
        (14, 1, 'APPLIED', date '2021-04-02'), (14, 2, 'PUBLISHED', date '2022-10-02'), (14, 3, 'WITHDRAWN', date '2025-08-31'),
        (15, 1, 'APPLIED', date '2021-09-09'), (15, 2, 'PUBLISHED', date '2023-03-09'), (15, 3, 'REJECTED', date '2025-09-30'),
        (16, 1, 'APPLIED', date '2021-12-03'), (16, 2, 'PUBLISHED', date '2023-06-03'), (16, 3, 'REGISTERED', date '2023-08-17'), (16, 4, 'EXPIRED', date '2025-10-31'),
        (17, 1, 'APPLIED', date '2022-03-18'), (17, 2, 'PUBLISHED', date '2023-09-18'), (17, 3, 'REGISTERED', date '2024-01-26'), (17, 4, 'ABANDONED', date '2025-11-30'),
        (18, 1, 'APPLIED', date '2022-08-25'), (18, 2, 'PUBLISHED', date '2024-02-25'), (18, 3, 'REGISTERED', date '2024-05-02'), (18, 4, 'INVALIDATED', date '2025-12-31'),
        (19, 1, 'APPLIED', date '2022-11-11'), (19, 2, 'PUBLISHED', date '2024-05-11'), (19, 3, 'WITHDRAWN', date '2026-01-31'),
        (20, 1, 'APPLIED', date '2023-02-23'), (20, 2, 'PUBLISHED', date '2024-08-23'), (20, 3, 'REJECTED', date '2026-02-28'),
        (21, 1, 'APPLIED', date '2023-06-06'), (21, 2, 'PUBLISHED', date '2024-12-06'), (21, 3, 'REGISTERED', date '2024-11-19'), (21, 4, 'EXPIRED', date '2026-03-31'),
        (22, 1, 'APPLIED', date '2023-09-14'), (22, 2, 'PUBLISHED', date '2025-03-14'), (22, 3, 'REGISTERED', date '2025-01-09'), (22, 4, 'ABANDONED', date '2026-04-30'),
        (23, 1, 'APPLIED', date '2024-01-30'), (23, 2, 'PUBLISHED', date '2025-07-30'), (23, 3, 'WITHDRAWN', date '2026-05-31'),
        (24, 1, 'APPLIED', date '2024-04-18'), (24, 2, 'PUBLISHED', date '2025-10-18'), (24, 3, 'REJECTED', date '2026-06-30')
),
demo_patents as (
    select patents.id, seed_patents.demo_no
    from patents
    join (
        values
            (1, 'DEMO-INACTIVE-001'), (2, 'DEMO-INACTIVE-002'), (3, 'DEMO-INACTIVE-003'), (4, 'DEMO-INACTIVE-004'),
            (5, 'DEMO-INACTIVE-005'), (6, 'DEMO-INACTIVE-006'), (7, 'DEMO-INACTIVE-007'), (8, 'DEMO-INACTIVE-008'),
            (9, 'DEMO-INACTIVE-009'), (10, 'DEMO-INACTIVE-010'), (11, 'DEMO-INACTIVE-011'), (12, 'DEMO-INACTIVE-012'),
            (13, 'DEMO-INACTIVE-013'), (14, 'DEMO-INACTIVE-014'), (15, 'DEMO-INACTIVE-015'), (16, 'DEMO-INACTIVE-016'),
            (17, 'DEMO-INACTIVE-017'), (18, 'DEMO-INACTIVE-018'), (19, 'DEMO-INACTIVE-019'), (20, 'DEMO-INACTIVE-020'),
            (21, 'DEMO-INACTIVE-021'), (22, 'DEMO-INACTIVE-022'), (23, 'DEMO-INACTIVE-023'), (24, 'DEMO-INACTIVE-024')
    ) as seed_patents(demo_no, application_number)
      on seed_patents.application_number = patents.application_number
)
insert into patent_legal_status (patent_id, status, changed_at, created_at, updated_at)
select
    demo_patents.id,
    seed_statuses.status,
    seed_statuses.changed_at,
    now(),
    now()
from seed_statuses
join demo_patents
  on demo_patents.demo_no = seed_statuses.demo_no
order by demo_patents.id, seed_statuses.status_order;

commit;

