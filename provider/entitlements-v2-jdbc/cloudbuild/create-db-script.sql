DROP TABLE IF EXISTS public.member_to_group;
DROP TABLE IF EXISTS public."member";
DROP TABLE IF EXISTS public.embedded_group;
DROP TABLE IF EXISTS public.app_id;
DROP TABLE IF EXISTS public."group";

CREATE TABLE public."group"
(
id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
name character varying COLLATE pg_catalog."default",
description text COLLATE pg_catalog."default",
email character varying COLLATE pg_catalog."default",
partition_id character varying COLLATE pg_catalog."default",
CONSTRAINT group_pkey PRIMARY KEY (id),
CONSTRAINT group_email_key UNIQUE (email),
CONSTRAINT group_name_key UNIQUE (name)
)

TABLESPACE pg_default;

ALTER TABLE public."group"
OWNER to postgres;

CREATE TABLE public.app_id
(
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY,
    group_id bigint,
    app_id character varying,
    PRIMARY KEY (id),
CONSTRAINT app_id_group_fk FOREIGN KEY (group_id)
REFERENCES public."group" (id) MATCH SIMPLE
ON UPDATE NO ACTION
ON DELETE NO ACTION
NOT VALID
);

ALTER TABLE public.app_id
OWNER to postgres;

CREATE TABLE public.member
(
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
email character varying COLLATE pg_catalog."default",
partition_id character varying COLLATE pg_catalog."default",
CONSTRAINT member_pkey PRIMARY KEY (id),
CONSTRAINT member_email_key UNIQUE (email)
)

TABLESPACE pg_default;

ALTER TABLE public.member
OWNER to postgres;

CREATE TABLE public.member_to_group
(
    group_id bigint NOT NULL,
    member_id bigint NOT NULL,
    role character varying COLLATE pg_catalog."default",
    CONSTRAINT member_to_group_pkey PRIMARY KEY (group_id, member_id),
CONSTRAINT group_as_member_holder_fk FOREIGN KEY (group_id)
REFERENCES public."group" (id) MATCH SIMPLE
ON UPDATE NO ACTION
ON DELETE NO ACTION
NOT VALID,
CONSTRAINT user_as_member_fk FOREIGN KEY (member_id)
REFERENCES public.member (id) MATCH SIMPLE
ON UPDATE NO ACTION
ON DELETE NO ACTION
NOT VALID
)

TABLESPACE pg_default;

ALTER TABLE public.member_to_group
OWNER to postgres;

CREATE TABLE public.embedded_group
(
    parent_id bigint NOT NULL,
    child_id bigint NOT NULL,
    CONSTRAINT embedded_group_pk PRIMARY KEY (parent_id, child_id),
CONSTRAINT group_as_child_fk FOREIGN KEY (child_id)
REFERENCES public."group" (id) MATCH SIMPLE
ON UPDATE NO ACTION
ON DELETE NO ACTION
NOT VALID,
CONSTRAINT group_as_parent_fk FOREIGN KEY (parent_id)
REFERENCES public."group" (id) MATCH SIMPLE
ON UPDATE NO ACTION
ON DELETE NO ACTION
NOT VALID
)

TABLESPACE pg_default;

ALTER TABLE public.embedded_group
OWNER to postgres;