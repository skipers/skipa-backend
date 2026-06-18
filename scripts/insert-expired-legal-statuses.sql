-- Insert EXPIRED legal status rows for patents whose expiry_date has already passed.
--
-- Rules:
--   - target patents: expiry_date < current_date
--   - inserted status: EXPIRED
--   - changed_at: patents.expiry_date
--   - avoids duplicate rows for the same patent/status/changed_at
--
-- Run this whole file in DataGrip or psql.

begin;

insert into patent_legal_status (
    patent_id,
    status,
    changed_at,
    created_at,
    updated_at
)
select
    patents.id,
    'EXPIRED',
    patents.expiry_date,
    now(),
    now()
from patents
where patents.expiry_date is not null
  and patents.expiry_date < current_date
  and not exists (
      select 1
      from patent_legal_status
      where patent_legal_status.patent_id = patents.id
        and patent_legal_status.status = 'EXPIRED'
        and patent_legal_status.changed_at = patents.expiry_date
  );

commit;

