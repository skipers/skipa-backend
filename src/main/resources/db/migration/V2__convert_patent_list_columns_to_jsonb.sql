alter table patents
    alter column related_products type jsonb
        using case
            when related_products is null or btrim(related_products) = '' then null
            when btrim(related_products) like '[%' then related_products::jsonb
            else jsonb_build_array(related_products)
        end,
    alter column keywords type jsonb
        using case
            when keywords is null or btrim(keywords) = '' then null
            when btrim(keywords) like '[%' then keywords::jsonb
            else jsonb_build_array(keywords)
        end;
