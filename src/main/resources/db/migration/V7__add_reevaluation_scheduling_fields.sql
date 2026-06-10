alter table patent_annuities rename column annuity_year to start_year;
alter table patent_annuities add column end_year integer;
update patent_annuities set end_year = start_year where status = 'PAID';

alter table reviews add column patent_annuity_id bigint;
alter table reviews add constraint fk_reviews_patent_annuity foreign key (patent_annuity_id) references patent_annuities (id);
create index idx_reviews_patent_annuity_id on reviews (patent_annuity_id);
