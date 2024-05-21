CREATE TABLE "group" (
    id bigint,
    name varchar,
    description text,
    email varchar,
    partition_id varchar
);

CREATE TABLE embedded_group
(
    parent_id bigint,
    child_id bigint
);

CREATE TABLE member_to_group
(
    group_id bigint,
    member_id bigint,
    role varchar
);

CREATE TABLE member
(
    id bigint,
    email varchar,
    partition_id varchar
)

