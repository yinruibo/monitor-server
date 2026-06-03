/*
 Navicat Premium Data Transfer

 Source Server         : 10.1.50.124-pg
 Source Server Type    : PostgreSQL
 Source Server Version : 110007
 Source Host           : 10.1.50.124:15434
 Source Catalog        : monitor-server
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 110007
 File Encoding         : 65001

 Date: 22/05/2025 16:56:31
*/


-- ----------------------------
-- Table structure for sys_docker_deploy_ele
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_docker_deploy_ele";
CREATE TABLE "public"."sys_docker_deploy_ele" (
  "ip" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "docker_name" varchar(255) COLLATE "pg_catalog"."default",
  "node_name" varchar(255) COLLATE "pg_catalog"."default",
  "task_name" varchar(255) COLLATE "pg_catalog"."default",
  "type" varchar(50) COLLATE "pg_catalog"."default",
  "is_show" int4,
  "create_time" timestamp(6),
  "update_time" timestamp(6),
  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "sort" int4
)
;
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."ip" IS '服务器IP';
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."docker_name" IS 'docker容器名称';
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."node_name" IS '节点名称';
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."task_name" IS '任务名称（容器显示名称）';
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."type" IS '数据类型 ：docker_cpu：CPU，docker_memory：内存';
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."is_show" IS '是否展示：0：展示，1：不展示';
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_docker_deploy_ele"."sort" IS '排序';
COMMENT ON TABLE "public"."sys_docker_deploy_ele" IS 'Docker监控信息配置表';

-- ----------------------------
-- Table structure for sys_linux_deploy_ele
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_linux_deploy_ele";
CREATE TABLE "public"."sys_linux_deploy_ele" (
  "ip" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "ip_name" varchar(200) COLLATE "pg_catalog"."default",
  "node_name" varchar(200) COLLATE "pg_catalog"."default",
  "type" varchar(255) COLLATE "pg_catalog"."default",
  "is_show" int4,
  "create_time" timestamp(6),
  "update_time" timestamp(6),
  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "sort" int4
)
;
COMMENT ON COLUMN "public"."sys_linux_deploy_ele"."ip" IS '服务器IP';
COMMENT ON COLUMN "public"."sys_linux_deploy_ele"."ip_name" IS '服务器名称';
COMMENT ON COLUMN "public"."sys_linux_deploy_ele"."node_name" IS '节点名称';
COMMENT ON COLUMN "public"."sys_linux_deploy_ele"."type" IS '数据类型种类 ：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录';
COMMENT ON COLUMN "public"."sys_linux_deploy_ele"."is_show" IS '是否展示：0：展示，1：不展示';
COMMENT ON COLUMN "public"."sys_linux_deploy_ele"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_linux_deploy_ele"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."sys_linux_deploy_ele"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_linux_deploy_ele"."sort" IS '排序';
COMMENT ON TABLE "public"."sys_linux_deploy_ele" IS 'linux系统信息配置表';

-- ----------------------------
-- Table structure for sys_warn_deploy
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_warn_deploy";
CREATE TABLE "public"."sys_warn_deploy" (
  "warn_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "ip" varchar(50) COLLATE "pg_catalog"."default",
  "warn_type" varchar(255) COLLATE "pg_catalog"."default",
  "threshold_sign" varchar(50) COLLATE "pg_catalog"."default",
  "min_threshold_num" int4,
  "middle_threshold_num" int4,
  "max_threshold_num" int4,
  "remarks" varchar(255) COLLATE "pg_catalog"."default",
  "email" varchar(50) COLLATE "pg_catalog"."default",
  "phone" varchar(50) COLLATE "pg_catalog"."default",
  "status" int4,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_warn_deploy"."warn_id" IS '主键Id';
COMMENT ON COLUMN "public"."sys_warn_deploy"."warn_type" IS '告警类型:linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录';
COMMENT ON COLUMN "public"."sys_warn_deploy"."threshold_sign" IS '阈值符号: >：大于';
COMMENT ON COLUMN "public"."sys_warn_deploy"."min_threshold_num" IS '最小阈值数值';
COMMENT ON COLUMN "public"."sys_warn_deploy"."middle_threshold_num" IS '中位阈值数值';
COMMENT ON COLUMN "public"."sys_warn_deploy"."max_threshold_num" IS '最大阈值数值';
COMMENT ON COLUMN "public"."sys_warn_deploy"."remarks" IS '告警描述';
COMMENT ON COLUMN "public"."sys_warn_deploy"."email" IS '邮箱-阈值发送邮件';
COMMENT ON COLUMN "public"."sys_warn_deploy"."phone" IS '手机号-阈值发送短信';
COMMENT ON COLUMN "public"."sys_warn_deploy"."status" IS '告警配置状态：0：启用，1：禁用';
COMMENT ON COLUMN "public"."sys_warn_deploy"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_warn_deploy"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."sys_warn_deploy"."ip" IS '内网IP';
COMMENT ON TABLE "public"."sys_warn_deploy" IS '告警管理配置表';

-- ----------------------------
-- Table structure for zr_docker_record_ele
-- ----------------------------
DROP TABLE IF EXISTS "public"."zr_docker_record_ele";
CREATE TABLE "public"."zr_docker_record_ele" (
  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "ip" varchar(255) COLLATE "pg_catalog"."default",
  "docker_name" varchar(255) COLLATE "pg_catalog"."default",
  "data_rate" varchar(20) COLLATE "pg_catalog"."default",
  "type" varchar(50) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "unit" varchar(50) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."zr_docker_record_ele"."id" IS '主键';
COMMENT ON COLUMN "public"."zr_docker_record_ele"."ip" IS '服务器IP';
COMMENT ON COLUMN "public"."zr_docker_record_ele"."docker_name" IS 'docker中容器名称';
COMMENT ON COLUMN "public"."zr_docker_record_ele"."data_rate" IS '利用率';
COMMENT ON COLUMN "public"."zr_docker_record_ele"."type" IS '数据类型 ：docker_cpu：CPU，docker_memory：内存';
COMMENT ON COLUMN "public"."zr_docker_record_ele"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."zr_docker_record_ele"."unit" IS '单位';
COMMENT ON TABLE "public"."zr_docker_record_ele" IS 'linux系统中docker监控信息记录表';

-- ----------------------------
-- Table structure for zr_linux_record_ele
-- ----------------------------
DROP TABLE IF EXISTS "public"."zr_linux_record_ele";
CREATE TABLE "public"."zr_linux_record_ele" (
  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "ip" varchar(50) COLLATE "pg_catalog"."default",
  "data_rate" varchar(50) COLLATE "pg_catalog"."default",
  "type" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "unit" varchar(50) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."zr_linux_record_ele"."id" IS '主键';
COMMENT ON COLUMN "public"."zr_linux_record_ele"."ip" IS '服务器IP';
COMMENT ON COLUMN "public"."zr_linux_record_ele"."data_rate" IS '利用率';
COMMENT ON COLUMN "public"."zr_linux_record_ele"."type" IS '数据类型种类 ：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录';
COMMENT ON COLUMN "public"."zr_linux_record_ele"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."zr_linux_record_ele"."unit" IS '数值单位';
COMMENT ON TABLE "public"."zr_linux_record_ele" IS 'linux系统信息记录表';

-- ----------------------------
-- Table structure for zr_warn_record_ele
-- ----------------------------
DROP TABLE IF EXISTS "public"."zr_warn_record_ele";
CREATE TABLE "public"."zr_warn_record_ele" (
  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "ip" varchar(50) COLLATE "pg_catalog"."default",
  "warn_type" varchar(50) COLLATE "pg_catalog"."default",
  "grade" int4,
  "explain" varchar(255) COLLATE "pg_catalog"."default",
  "continued_time" varchar(255) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6),
  "status" int4,
  "file_path" varchar(250) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."zr_warn_record_ele"."id" IS '主键';
COMMENT ON COLUMN "public"."zr_warn_record_ele"."ip" IS '服务IP';
COMMENT ON COLUMN "public"."zr_warn_record_ele"."warn_type" IS '告警类型:linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录';
COMMENT ON COLUMN "public"."zr_warn_record_ele"."grade" IS '告警级别：0：一般，1：严重，2：非常严重';
COMMENT ON COLUMN "public"."zr_warn_record_ele"."explain" IS '告警说明';
COMMENT ON COLUMN "public"."zr_warn_record_ele"."continued_time" IS '持续时间';
COMMENT ON COLUMN "public"."zr_warn_record_ele"."create_time" IS '首次告警时间';
COMMENT ON COLUMN "public"."zr_warn_record_ele"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."zr_warn_record_ele"."status" IS '告警状态：0：运行中，1：已完成';
COMMENT ON COLUMN "public"."zr_warn_record_ele"."file_path" IS '文件路径';
COMMENT ON TABLE "public"."zr_warn_record_ele" IS '告警监控记录表';

-- ----------------------------
-- Primary Key structure for table sys_docker_deploy_ele
-- ----------------------------
ALTER TABLE "public"."sys_docker_deploy_ele" ADD CONSTRAINT "sys_docker_deploy_ele_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_linux_deploy_ele
-- ----------------------------
ALTER TABLE "public"."sys_linux_deploy_ele" ADD CONSTRAINT "sys_linux_deploy_ele_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_warn_deploy
-- ----------------------------
ALTER TABLE "public"."sys_warn_deploy" ADD CONSTRAINT "sys_warn_deploy_pkey" PRIMARY KEY ("warn_id");

-- ----------------------------
-- Indexes structure for table zr_docker_record_ele
-- ----------------------------
CREATE UNIQUE INDEX "docker_record_index" ON "public"."zr_docker_record_ele" USING btree (
  "ip" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "docker_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "create_time" "pg_catalog"."timestamp_ops" DESC NULLS LAST
);
COMMENT ON INDEX "public"."docker_record_index" IS 'docker容器唯一索引';

-- ----------------------------
-- Primary Key structure for table zr_docker_record_ele
-- ----------------------------
ALTER TABLE "public"."zr_docker_record_ele" ADD CONSTRAINT "zr_docker_record_ele_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table zr_linux_record_ele
-- ----------------------------
CREATE UNIQUE INDEX "linux_record_index" ON "public"."zr_linux_record_ele" USING btree (
  "ip" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" DESC NULLS LAST,
  "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
COMMENT ON INDEX "public"."linux_record_index" IS 'linux日志唯一索引';

-- ----------------------------
-- Primary Key structure for table zr_linux_record_ele
-- ----------------------------
ALTER TABLE "public"."zr_linux_record_ele" ADD CONSTRAINT "zr_linux_record_ele_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table zr_warn_record_ele
-- ----------------------------
CREATE UNIQUE INDEX "warn_record_index" ON "public"."zr_warn_record_ele" USING btree (
  "ip" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "warn_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "grade" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "status" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "create_time" "pg_catalog"."timestamp_ops" DESC NULLS LAST
);
COMMENT ON INDEX "public"."warn_record_index" IS '告警记录表唯一索引';

-- ----------------------------
-- Primary Key structure for table zr_warn_record_ele
-- ----------------------------
ALTER TABLE "public"."zr_warn_record_ele" ADD CONSTRAINT "zr_warn_record_ele_pkey" PRIMARY KEY ("id");
