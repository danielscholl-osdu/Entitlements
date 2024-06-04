
--   /- g3
-- g1 - g2 - g4
-- |         |
-- u1(O)     u1(M)

-- add first group
INSERT INTO "group" VALUES (1, 'first group name', 'description', 'group1@dp.group.com', 'dp');
INSERT INTO "group" VALUES (2, 'second group name', 'description', 'group2@dp.group.com', 'dp');
INSERT INTO "group" VALUES (3, 'third group name', 'description', 'group3@dp.group.com', 'dp');
INSERT INTO "group" VALUES (4, 'forth group name', 'description', 'group4@dp.group.com', 'dp');

-- users
INSERT INTO member VALUES (1, 'user1@xxx.com', 'dp');

-- fill group with users
INSERT INTO member_to_group VALUES (1, 1, 'OWNER');
INSERT INTO member_to_group VALUES (4, 1, 'MEMBER');

-- add embedded group in first group
INSERT INTO embedded_group VALUES (1, 2);
INSERT INTO embedded_group VALUES (1, 3);
INSERT INTO embedded_group VALUES (2, 4);
