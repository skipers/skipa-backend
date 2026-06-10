alter table patents
    add column approval_status varchar(30) not null default 'APPROVED';
