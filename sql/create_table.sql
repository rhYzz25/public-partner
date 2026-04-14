# CREATE DATABASE IF NOT EXISTS newMatch;
CREATE DATABASE IF NOT EXISTS newPartner;

use newPartner;

-- 用户表
CREATE TABLE `user` (
                        `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                        `nickname` VARCHAR(50) NULL COMMENT '昵称',
                        `account` VARCHAR(50) NOT NULL UNIQUE COMMENT '账号',
                        `password` VARCHAR(128) NOT NULL COMMENT '密码',
                        `email` VARCHAR(100) COMMENT '邮箱',
                        `introduction` TEXT COMMENT '自我介绍',
                        `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `is_delete` TINYINT(1) DEFAULT 0 COMMENT '0 否 1 是'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# note 你分的清什么是反引号吗
ALTER TABLE `user` ADD COLUMN `role` VARCHAR(256) DEFAULT 'user' COMMENT '角色: user/admin' AFTER gender;
ALTER TABLE `user` ADD COLUMN `avatar` VARCHAR(1024) NULL COMMENT '用户头像' AFTER nickname;

-- 标签表
CREATE TABLE `tag` (
                       `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                       `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名',
                       `kind` VARCHAR(50) COMMENT '分类（如：编程语言、兴趣爱好）',
                       `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户与标签的中间表
CREATE TABLE `user_tag` (
                            `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                            `user_id` BIGINT NOT NULL COMMENT '用户id',
                            `tag_id` BIGINT NOT NULL COMMENT '标签id',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- 唯一索引：防止同一个用户重复贴同一个标签
                            UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`),
    -- 普通索引：加速根据标签搜人的操作
                            INDEX `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create table team
(
    id          bigint auto_increment comment 'id'
        primary key,
    name        varchar(256)                       not null comment '队伍名称',
    description varchar(1024)                      null comment '描述',
    max_num     int      default 1                 not null comment '最大人数',
    expire_time datetime                           null comment '过期时间',
    user_id     bigint                             null comment '用户id（队长 id）',
    status      int      default 0                 not null comment '0 - 公开，1 - 私有，2 - 加密',
    password    varchar(512)                       null comment '密码',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    is_delete   tinyint  default 0                 not null comment '是否删除'
)
    comment '队伍';

-- 多一事那就多一事
alter table team add column `need_approval` tinyint(1) default 0 comment '是否需要审批，0-不需要，1-需要';

create table user_team
(
    id          bigint auto_increment comment 'id'
        primary key,
    user_id     bigint                             null comment '用户id',
    team_id     bigint                             null comment '队伍id',
    join_time   datetime                           null comment '加入时间',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    is_delete   tinyint  default 0                 not null comment '是否删除'
)
    comment '用户队伍关系';

create table if not exists join_req(
                                       `id` int primary key auto_increment comment '主键',
                                       `team_id` int not null comment '队伍id',
                                       `user_id` int not null comment '用户id',
                                       `create_time` datetime default current_timestamp
)engine=innodb default charset = utf8;

alter table join_req add `status` tinyint(1) default 0 comment '其实我是故意忘加的字段';
-- 忘吧,多忘
alter table join_req add column `password` varchar(512) null comment '入队密码，加密队伍需要';

-- 插入默认管理员账号：admin
INSERT INTO `user` (`id`, `nickname`, `avatar`, `account`, `password`, `email`, `gender`, `role`, `is_delete`)
VALUES (1, '管理员', NULL, 'admin123', 'admin123', 'admin@example.com', 0, 'admin', 0);

