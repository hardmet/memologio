CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
create table post(
    id uuid default uuid_generate_v4() not null primary key,
    url       varchar(512)                    not null,
    published timestamp                       not null,
    likes     integer                         not null
);
