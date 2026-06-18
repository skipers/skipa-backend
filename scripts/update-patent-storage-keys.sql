-- Update storage object keys for patent rows 1 through 185.
-- Based on ai-server-integration.md final object key convention:
--   patents/{patentId}/original.pdf
--   patents/{patentId}/parsed.json
--
-- Run this whole file in DataGrip or psql.

begin;

update patents
set original_pdf_key = 'patents/' || id || '/original.pdf',
    parsed_json_key = 'patents/' || id || '/parsed.json'
where id between 1 and 185;

commit;
