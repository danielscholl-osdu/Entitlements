-- add first group
INSERT INTO "group" VALUES (1, 'first group name', 'description', 'first_group@domen.com', 'partition_1');
INSERT INTO "group" VALUES (2, 'second group name', 'description', 'second_group@domen.com', 'partition_1');
INSERT INTO "group" VALUES (3, 'third group name', 'description', 'third_group@domen.com', 'partition_2');

-- users
INSERT INTO member VALUES (1, 'user1@domen.com', 'partition_1');
INSERT INTO member VALUES (2, 'user2@domen.com', 'partition_1');
INSERT INTO member VALUES (3, 'user3@domen.com', 'partition_1');

-- fill group with users
INSERT INTO member_to_group VALUES (1, 1, 'OWNER');
INSERT INTO member_to_group VALUES (1, 2, 'MEMBER');
INSERT INTO member_to_group VALUES (1, 3, 'MEMBER');

-- add embedded group in first group
INSERT INTO embedded_group VALUES (1, 2);
INSERT INTO embedded_group VALUES (1, 3);
