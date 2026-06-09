alter table patents
    rename column ipc_code to ipc_codes;

alter table patents
    rename column cpc_code to cpc_codes;

alter table patents
    alter column ipc_codes type jsonb
        using case
            when ipc_codes is null or btrim(ipc_codes) = '' then null
            when btrim(ipc_codes) like '[%' then ipc_codes::jsonb
            else jsonb_build_array(ipc_codes)
        end,
    alter column cpc_codes type jsonb
        using case
            when cpc_codes is null or btrim(cpc_codes) = '' then null
            when btrim(cpc_codes) like '[%' then cpc_codes::jsonb
            else jsonb_build_array(cpc_codes)
        end;
