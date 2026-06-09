alter table patents
    rename column overview to summary;

alter table patents
    drop column core_content;
