# Monitor Server

> 一套轻量级的服务器硬件与 Docker 容器监控告警系统，支持 Linux、Windows 和 Docker 三大监控场景。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-1.8-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.2-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-11-blue.svg)](https://www.postgresql.org/)

---

## 📋 目录

- [功能特性](#-功能特性)
- [监控指标](#-监控指标)
- [系统架构](#-系统架构)
- [技术栈](#-技术栈)
- [环境要求](#-环境要求)
- [快速开始](#-快速开始)
- [配置说明](#-配置说明)
- [API 接口文档](#-api-接口文档)
- [数据库表说明](#-数据库表说明)
- [项目结构](#-项目结构)
- [部署运行](#-部署运行)
- [常见问题](#-常见问题)
- [License](#-license)

---

## 🚀 功能特性

- **三大监控场景** — 同时支持 Linux 服务器、Windows 主机和 Docker 容器的硬件指标采集
- **定时采集** — 每 5 分钟自动采集一次监控数据，支持异步线程池并发处理
- **三级告警** — 支持一般（≥50%）、严重（≥70%）、非常严重（≥80%）三个等级的阈值告警
- **告警生命周期** — 告警触发自动记录，指标恢复自动关闭，等级变化自动切换
- **Docker 流式采集** — 采用 Docker Client 单例 + 流式 Stats 订阅，高效获取容器实时指标
- **历史数据查询** — 支持按时间范围查询历史监控数据
- **告警记录导出** — 支持 Excel 格式导出告警记录
- **数据自动清理** — 每日凌晨 2:00 自动清理 15 天前的历史数据
- **Swagger API 文档** — 内置接口文档，启动后即可查看
- **Nacos 配置中心** — 支持通过 Nacos 动态刷新配置

---

## 📊 监控指标

### Linux / Windows 服务器

| 指标 | 说明 | Type 编码 |
|------|------|-----------|
| CPU 利用率 | 处理器使用百分比 | `linux_cpu` / `win_cpu` |
| 内存利用率 | 内存使用百分比 | `linux_memory` / `win_memory` |
| 磁盘利用率 | 磁盘使用百分比 | `linux_disk` / `win_disk` |
| 磁盘 IO 读取 | 磁盘读取速率 (MB/s) | `linux_IO_read` / `win_IO_read` |
| 磁盘 IO 写入 | 磁盘写入速率 (MB/s) | `linux_IO_write` / `win_IO_write` |
| 网卡上传 | 网络上传速率 (KB/s) | `linux_network_up` / `win_network_up` |
| 网卡下载 | 网络下载速率 (KB/s) | `linux_network_down` / `win_network_down` |

### Docker 容器

| 指标 | 说明 | Type 编码 |
|------|------|-----------|
| CPU 利用率 | 容器 CPU 使用百分比 | `docker_cpu` |
| 内存利用率 | 容器内存使用百分比 | `docker_memory` |
| 磁盘 IO 读取 | 容器磁盘读取速率 | `docker_IO_read` |
| 磁盘 IO 写入 | 容器磁盘写入速率 | `docker_IO_write` |
| 网卡上传 | 容器网络上传速率 | `docker_network_up` |
| 网卡下载 | 容器网络下载速率 | `docker_network_down` |

---

## 🏗 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Monitor Server                           │
│                                                                 │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐                    │
│  │  Linux   │   │ Windows  │   │  Docker  │    定时采集层        │
│  │ CornJob  │   │ CornJob  │   │ CornJob  │   (每5分钟触发)     │
│  └────┬─────┘   └────┬─────┘   └────┬─────┘                    │
│       │              │              │                           │
│       ▼              ▼              ▼                           │
│  ┌──────────────────────────────────────┐                      │
│  │     ZrWarnRecordEleService           │    业务逻辑层         │
│  │  (指标采集 + 阈值判断 + 告警记录)      │                      │
│  └──────────────────┬───────────────────┘                      │
│                     │                                          │
│       ┌─────────────┼─────────────┐                            │
│       ▼             ▼             ▼                             │
│  ┌─────────┐  ┌──────────┐  ┌─────────┐                       │
│  │ Linux   │  │  Docker  │  │  Warn   │    数据持久层           │
│  │ Record  │  │  Record  │  │ Record  │    (PostgreSQL)        │
│  │ Table   │  │  Table   │  │ Table   │                        │
│  └─────────┘  └──────────┘  └─────────┘                       │
│                                                                 │
│  ┌──────────────────────────────────────┐                      │
│  │          Controller Layer            │    REST API 层        │
│  │  (Linux / Docker / Warn / Config)    │                      │
│  └──────────────────────────────────────┘                      │
└─────────────────────────────────────────────────────────────────┘
```

**Docker 监控数据流（优化后）：**

```
Docker Daemon ──(streaming stats)──▶ DockerClient (单例)
                                         │
                                         ▼
                                   ConcurrentHashMap
                                   (DockerMetricsSnapshot)
                                         │
                              ┌──────────┼──────────┐
                              ▼          ▼          ▼
                          告警判断    实时查询    历史记录
```

---

## 🛠 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 2.3.2.RELEASE | 应用框架 |
| **Spring Cloud** | 2.2.3.RELEASE | 微服务支持（Nacos 配置中心） |
| **MyBatis-Plus** | 3.3.2 | ORM 框架，支持分页、批量操作 |
| **PostgreSQL** | 11 | 关系型数据库 |
| **Alibaba Druid** | 1.2.4 | 数据库连接池，内置监控页面 |
| **docker-java** | 3.5.3 | Docker API 客户端 |
| **OSHI** | 6.4.4 | 跨平台硬件信息采集（Windows/Linux） |
| **JNA** | 5.14.0 | Java 原生访问，OSHI 依赖 |
| **Hutool** | 5.5.7 | Java 工具库 |
| **Swagger 3** | 3.0.0 | API 接口文档 |
| **Apache POI** | 3.17 | Excel 导出 |
| **Lombok** | — | 代码简化 |
| **MapStruct** | 1.3.0 | 对象映射 |

---

## 📦 环境要求

- **JDK** 1.8+
- **Maven** 3.6+
- **PostgreSQL** 11+
- **Docker**（如需 Docker 容器监控）
- **Nacos**（如需配置中心功能，可选）

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/yinruibo/monitor-server.git
cd monitor-server
```

### 2. 初始化数据库

```bash
# 创建数据库
psql -U postgres -c "CREATE DATABASE \"monitor-server\";"

# 导入表结构
psql -U postgres -d monitor-server -f src/main/resources/sql/monitor.sql
```

### 3. 修改配置

编辑 `src/main/resources/bootstrap.yml`，配置数据库连接和监控参数：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/monitor-server
    username: your_username
    password: your_password

datacenter:
  monitor:
    network:
      ip: 192.168.1.100          # 本机内网 IP
    disk:
      path: /,/data              # 需要监控的磁盘路径（逗号分隔）
    cpumory:
      path: /proc                # proc 文件系统路径
    network:
      cards: /sys/class/net      # 网卡设备路径
```

### 4. 编译打包

```bash
mvn clean package -DskipTests
```

### 5. 启动运行

```bash
java -jar target/monitor-server-0.0.1-SNAPSHOT.jar
```

启动成功后访问：
- **接口文档**：`http://localhost:10020/doc.html`
- **Druid 监控**：`http://localhost:10020/druid`

---

## ⚙️ 配置说明

### 应用配置 (`bootstrap.yml`)

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `server.port` | 服务端口 | `10020` |
| `datacenter.monitor.network.ip` | 本机内网 IP（支持读取 ifcfg 文件） | `192.168.1.100` |
| `datacenter.monitor.disk.path` | 监控磁盘路径（逗号分隔） | `/,/data,/home` |
| `datacenter.monitor.cpumory.path` | proc 文件系统路径 | `/proc` |
| `datacenter.monitor.network.cards` | 网卡设备路径 | `/sys/class/net` |
| `datacenter.monitor.warnRecord.path` | 告警记录导出路径 | `/tmp/warn` |

### 监控开关

通过数据库表控制监控的启用/禁用：

| 表名 | 字段 | 说明 |
|------|------|------|
| `sys_linux_deploy_ele` | `isShow` | `0`=展示并监控，`1`=隐藏并停止监控 |
| `sys_docker_deploy_ele` | `isShow` | `0`=展示并监控，`1`=隐藏并停止监控 |
| `sys_warn_deploy` | `status` | 告警配置页面是否可查询展示 |

### 定时任务

| 类名 | 周期 | 功能 |
|------|------|------|
| `LinuxCornJob` | 每 5 分钟 | Linux 服务器指标采集 |
| `DockerCornJob` | 每 5 分钟 | Docker 容器指标采集 + 订阅刷新 |
| `Win10CronJob` | 每 5 分钟 | Windows 主机指标采集 |
| `CleanSchJob` | 每日 02:00 | 清理 15 天前的历史数据 |

> 💡 如不使用某类监控，注释掉对应的 `CornJob` 类即可。

---

## 📡 API 接口文档

### Linux 监控接口 (`/linux`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/queryNodeList` | 查询节点列表 |
| GET | `/queryServersByNode?nodeName=` | 查询单节点下的服务器列表 |
| GET | `/queryServerList` | 查询所有服务器列表 |
| GET | `/queryNodeMonitor?nodeName=&ip=` | 查询单服务器最新监控数据 |
| POST | `/queryServerRecord` | 查询服务器历史监控记录 |

### Docker 监控接口 (`/docker`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/queryNodeList` | 查询节点列表 |
| GET | `/queryServersByNode?nodeName=` | 查询单节点下的服务器列表 |
| POST | `/queryServerList` | 查询所有服务器列表 |
| GET | `/queryDockerNameList?nodeName=&ip=` | 查询指定服务器的容器列表 |
| GET | `/queryNodeDocker?nodeName=&ip=` | 查询单服务器所有容器最新监控 |
| POST | `/queryDockerRecord` | 查询容器历史监控记录 |

### 告警接口 (`/warn`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/queryWarnRecordList` | 分页查询告警记录 |
| POST | `/queryWarnSignList` | 告警分布饼图数据 |
| POST | `/queryFaultList` | 故障趋势数据 |
| GET | `/queryExport` | 告警记录 Excel 导出 |
| GET | `/queryWarnNumber?ip=` | 查询当日告警数量 |

### 告警配置接口 (`/sys`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/queryWarnDeployList` | 查询告警配置列表 |
| POST | `/saveWarnDeploy` | 新增告警配置 |
| POST | `/updateWarnDeploy` | 修改告警配置 |
| POST | `/deleteWarnDeploy` | 删除告警配置 |

### 历史记录请求示例

**查询 Linux 历史记录：**
```json
{
    "ip": "192.168.1.124",
    "startTime": "2025-10-15 01:00:00",
    "endTime": "2025-10-15 12:00:00",
    "type": "linux_cpu"
}
```

**查询 Docker 历史记录：**
```json
{
    "ip": "192.168.1.124",
    "dockerName": "postgresql",
    "startTime": "2025-10-15 01:00:00",
    "endTime": "2025-10-15 12:00:00",
    "type": "docker_cpu"
}
```

---

## 🗄 数据库表说明

| 表名 | 说明 |
|------|------|
| `sys_linux_deploy_ele` | Linux 监控配置表（IP、节点、类型、显示开关） |
| `sys_docker_deploy_ele` | Docker 监控配置表（IP、容器名、节点、类型） |
| `sys_warn_deploy` | 告警阈值配置表（IP、类型、三级阈值、邮箱、手机号） |
| `zr_linux_record_ele` | Linux 监控数据记录表 |
| `zr_docker_record_ele` | Docker 监控数据记录表 |
| `zr_warn_record_ele` | 告警事件记录表（IP、类型、等级、状态、持续时间） |

> 所有表均配置了复合唯一索引用于去重，历史数据保留 **15 天**。

---

## 📁 项目结构

```
monitor-server/
├── pom.xml                                    # Maven 配置
├── readme.md                                  # 项目说明文档
├── AGENT.txt                                  # Docker 监控优化说明
├── windows监控.bat                            # Windows 启动脚本
└── src/
    ├── main/
    │   ├── java/cn/hongt/monitor/server/
    │   │   ├── MonitorServerApplication.java  # 启动入口
    │   │   ├── CorsFilter.java                # 跨域过滤器
    │   │   ├── controller/                    # REST 接口层
    │   │   │   ├── LinuxMonitorController     # Linux 监控查询
    │   │   │   ├── DockerMonitorController    # Docker 监控查询
    │   │   │   ├── LinuxDeployController      # Linux 配置管理
    │   │   │   ├── DockerDeployController     # Docker 配置管理
    │   │   │   ├── SysWarnDeployController    # 告警配置管理
    │   │   │   ├── ZrWarnRecordEleController  # 告警记录查询
    │   │   │   ├── CleanController            # 数据清理
    │   │   │   └── UpdateMapDataController    # 缓存刷新
    │   │   ├── service/                       # 业务逻辑层
    │   │   │   ├── DockerClientFactory        # Docker 客户端工厂（接口）
    │   │   │   ├── DockerMetricCollectorService # Docker 指标采集（接口）
    │   │   │   ├── DockerMetricsSnapshot      # Docker 指标快照
    │   │   │   └── impl/                      # 实现类
    │   │   ├── entity/                        # 数据库实体类
    │   │   ├── dto/                           # 数据传输对象
    │   │   │   ├── input/                     # 请求 DTO
    │   │   │   └── output/                    # 响应 DTO
    │   │   ├── mapper/                        # MyBatis Mapper 接口
    │   │   ├── enums/                         # 枚举类（告警类型编码）
    │   │   ├── schedule/                      # 定时任务
    │   │   │   ├── LinuxCornJob               # Linux 采集任务
    │   │   │   ├── DockerCornJob              # Docker 采集任务
    │   │   │   ├── Win10CronJob               # Windows 采集任务
    │   │   │   └── CleanSchJob                # 数据清理任务
    │   │   ├── config/                        # 配置类
    │   │   └── common/                        # 公共模块
    │   │       ├── consts/                    # 常量
    │   │       ├── utils/                     # 工具类
    │   │       ├── exception/                 # 异常处理
    │   │       └── page/                      # 分页基类
    │   └── resources/
    │       ├── bootstrap.yml                  # 应用配置
    │       ├── sql/monitor.sql                # 数据库初始化脚本
    │       ├── lib/                           # 系统依赖 JAR
    │       └── log/logback.xml                # 日志配置
    └── test/                                  # 单元测试
```

---

## 🖥 部署运行

### 方式一：直接运行

```bash
java -Dfile.encoding=utf-8 -jar monitor-server-0.0.1-SNAPSHOT.jar
```

### 方式二：Windows 批处理启动

双击 `windows监控.bat` 即可启动（需将 JAR 放在 `D:\monitor` 目录下）。

### 方式三：Docker 部署

```bash
# 构建镜像
docker build -t monitor-server .

# 运行容器
docker run -d \
  --name monitor-server \
  -p 10020:10020 \
  -v /proc:/host/proc:ro \
  -v /sys:/host/sys:ro \
  -v /var/run/docker.sock:/var/run/docker.sock \
  monitor-server
```

> ⚠️ Docker 部署时需要挂载宿主机的 `/proc`、`/sys` 和 `docker.sock` 以采集硬件和容器指标。

---

## ❓ 常见问题

### Q: 如何只启用 Linux 监控，不需要 Docker 监控？

注释掉 `DockerCornJob` 类上的 `@Component` 注解即可。

### Q: 如何修改监控采集频率？

修改 `CornJob` 类中 `@Scheduled` 注解的 cron 表达式，默认为 `0 0/5 * * * ?`（每 5 分钟）。

### Q: Docker 监控连接失败？

确保 Docker Daemon 已启动，且应用有权限访问 Docker Socket（默认 `/var/run/docker.sock`）。

### Q: 如何调整告警阈值？

通过 `/sys` 接口或直接修改 `sys_warn_deploy` 表中的阈值字段。

### Q: 历史数据保留多久？

默认保留 **15 天**，由 `CleanSchJob` 每日凌晨 2:00 自动清理。如需调整，修改清理任务中的天数参数。

---

## 📄 License

本项目采用 [MIT License](LICENSE) 开源协议。

---

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) — 应用框架
- [MyBatis-Plus](https://baomidou.com/) — ORM 框架
- [docker-java](https://github.com/docker-java/docker-java) — Docker Java 客户端
- [OSHI](https://github.com/oshi/oshi) — 跨平台硬件信息库
- [Hutool](https://hutool.cn/) — Java 工具库
