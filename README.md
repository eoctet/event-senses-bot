# 事件决策引擎

## 项目背景

这是我独立开发的一款企业级产品的第二个版本，正如它的名称一样，作为**事件驱动业务的内核**已在数家企业中落地发芽。

> ℹ️ 注意：该版本简化了部分功能（包括组件、流水线执行拓扑DAG等），~~并且不包含管理端服务（这个不重要）~~。
> 你可以基于该项目实现一些自己的业务场景，你也可以随时提交PR继续丰富这个项目。

#### 设计理念

在海量的实时数据计算中，夹杂了大量的人工干预过程，包括对数据本身的清洗、业务规则判断、调用第三方算法等，重复的开发和半工具式作业使人崩溃，更重要的是业务用户根本不懂这些如何使用。
所以借助傻瓜式的画布拖拽来实现一个自己想要的业务场景很关键，让实时事件决策就像搭积木一样简单，这大大降低了业务用户使用数据进行业务决策的难度，同时让数据开发人员关注数据治理本身而不是ETL。

#### 应用场景

任何需要实时分析、决策的应用场景：

- 💹 **实时交易分析**
  - 对系统发生的每一笔交易进行分析决策，例如：客户营销、交易反欺诈等。
- 📈 **物联网事件监控**
  - 对海量物联网设备事件进行动态处置，例如：灾害预警、设备自检等。

#### 主要功能

- [X] 🚀 事件动作：

| 动作名称 | 功能                  |
|------|---------------------|
| 接口动作 | 支持调用第三方Restful API。 |
| 条件动作 | 支持组合条件、单一条件的逻辑判断。   |
| 邮件动作 | 支持发送邮件。             |
| 模式匹配 | 复杂事件中对不同模式的规则匹配。    |
| 脚本动作 | 支持自定义脚本（Groovy）。    |

- [X] 🚀 自定义流水线
- [X] 🚀 复杂事件（CEP）
- [X] 🚀 单一事件
- [X] 🚀 迟到事件处理

#### 名词解释

| 名词         | 解释                                     |
|------------|----------------------------------------|
| ⚙️ **事件**  | 一个独立的原子业务，包括：事件名称、事件时间、事件源、事件内容4个关键要素。 |
| 📮 **渠道**  | 事件来源的定义，用于对事件进行分类分级，例如：交易渠道、设备网关等。     |
| 🔗 **动作**  | 一个具体的执行动作，例如：发送邮件、调用接口、执行算法等。          |
| 🔄 **流水线** | 一个完整的业务处理场景，由多个动作组合的复杂流程。              |

## 如何使用

这里用一个发送邮件的事件举例。

__1. 环境准备（可选）__

> 为方便快速部署，建议使用Docker完成以下环境准备工作。

- Clickhouse

默认支持Clickhouse Sink作为实时数据仓库，推荐使用21.0及以上版本。

```shell
docker pull clickhouse/clickhouse-server

docker run -p 8123:8123 -p 3500:3500 --name clickhouse-server -e CLICKHOUSE_DB=default -e CLICKHOUSE_USER=root -e CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT=1 -e CLICKHOUSE_PASSWORD=123456 -e TZ=Asia/Shanghai -d clickhouse/clickhouse-server
```

> ℹ️ 初始化数据库
>
> 执行 [clickhouse.init.sql](db_script%2Fclickhouse.init.sql) 初始化CH数据库。

- Redis

用于存储部分实时缓存数据。

```shell
docker pull redis

docker run -p 6379:6379 --name redis-server -d redis
```

- Flink

下载 `Apache Flink 1.14.5` 到本地目录，这里使用简单的本地运行模式。

```shell
https://archive.apache.org/dist/flink/flink-1.14.5
```

__2. 编译打包__

- Maven Build

```shell
git clone https://github.com/eoctet/event-senses-bot.git

cd event-senses-bot

mvn clean package
```

__3. 流水线配置__

配置一个可以发送邮件的流水线：[发送邮件示例](flow-samples%2Femail-sample.yaml)

> 更多流水线配置示例请参考：flow-samples/Xxx.yaml


__4. 服务发布__

发布Flink任务。

```shell
./bin/flink run-application event-senses-bot-22.0816.2.6.jar -yaml /YOUR_PATH/email-sample.yaml
```

> ℹ️ TIPS
> 
> 为了方便快速使用，上述步骤均使用默认的服务参数和本地部署模式。
> 
> 本项目也支持其他的部署方案，如 `Flink On YARN`。


## 开发手册

#### 流水线配置

- 任务参数

事件任务参数定义了该任务的并行度、缓存服务、实时数据库信息。

```yaml
app:
  logger: "DEBUG"                           # 日志模式
  lang: "zh-CN"                             # 事件跟踪日志语言类型 zh-CN/en
  source:
    kafka-bootstrap: "localhost:9093"       # Kafka bootstrap
  job:
    parallelism:
      default: 2                            # Flink job 默认并行度
      max: 100                              # Flink job 最大并行度
    checkpoint:
      interval: 5000                        # Flink job checkpoint 间隔时间（默认5秒）
      storage: "/YOUR_PATH/log/checkpoint"  # Flink job checkpoint 存储路径
    filter:
      enabled: true                         # 是否启用事件重复过滤
      max-cache-expire: 300                 # 最大缓存过期时间（默认300秒）
  sink:
    parallelism: 2                          # SINK并行度
    backup: "/YOUR_PATH/log/backup"         # SINK输出备份目录
    cache:
      max-queue: 5                          # 内存队列缓存刷新数量
      interval-millis: 5000                 # 内存队列缓存刷新间隔时间（默认5秒）
  db:
    redis: # Redis服务配置参数
      cluster-enabled: false
      cluster-nodes:
      host: localhost
      port: 6379
      username:
      password:
      database: 0
      pool:
        max-total: 20
        max-idle: 8
        min-idle: 0
        max-wait-millis: 3000
    clickhouse: # Clickhouse服务配置参数
      jdbc: "jdbc:clickhouse://localhost:8123/event_senses_bot"
      username:
      password:
```

- 事件信息

接入的实时事件信息。

```yaml
event:
  name: "事件的名称"            # 事件名称
  code: "EVENT-5C0595EFEAA9"  # 事件编号（EVENT-XXXXXXXX）
  catalog: "TEST"             # 事件分类
  desc: "这是一个事件示例"       # 事件描述

channel:
  name: "测试事件渠道"
  code: "CHANNEL-88DD8E18EAC7"
  topic: "event_sense_topic"
  format: "JSON"
  groupId: "event_sense_group1"
  encryption:
    type: "TEXT"
    key: ""
  params:
    - { param: "id", filter: true, nullable: true, datatype: "STRING", desc: "UID" }
    - { param: "key1", datatype: "STRING", desc: "demo desc" }
    - { param: "key2", datatype: "STRING", desc: "demo desc" }
```

- 事件动作

一个完整的事件动作包含以下参数组成。

```yaml
action:
  name: "事件动作名称"          # 事件动作名称
  code: "ACTION-C77AC80CDBF3" # 事件动作编号（ACTION-XXXXXXXX）
  type: "API"                 # 事件动作类型
  parent: "0"                 # 事件动作前序节点的编号，0为根节点
  config:
    # 事件动作的配置参数
    # 不同的事件动作配置参数模版是不一样的，具体请参考示例
    # API:
    #   ...
    # 事件动作的输出参数列表
    # { param: "参数名称", datatype: "数据类型", desc: "描述信息" }
    # 例如：
    output:
      - { param: "code", datatype: "LONG", desc: "" }
      - { param: "data", datatype: "STRING", desc: "" }

```

__🎁 流水线示例__

- [完整示例](flow-samples%2Fall-sample.yaml)
- [接口调用示例](flow-samples%2Fapi-sample.yaml)
- [复杂事件示例](flow-samples%2Fcep-sample.yaml)
- [条件判断示例](flow-samples%2Fcondition-sample.yaml)
- [发送邮件示例](flow-samples%2Femail-sample.yaml)
- [自定义脚本示例](flow-samples%2Fscript-sample.yaml)


#### 自定义组件开发

所有的组件都继承了 `AbstractAction` 并且实现 `ActionService` 接口。

> package pro.octet.senses.bot.action.*

除了默认支持的组件以外，你也可以自行开发一些新的组件，它们遵循一致的接口规范，例如：

```java
//继承AbstractAction，实现execute方法
public class DemoAction extends AbstractAction {

    public DemoAction(Action action) {
        super(action);
    }

    @Override
    public ExecuteResult execute() throws ActionExecutionException {
        ExecuteResult executeResult = new ExecuteResult();

        // ... 业务逻辑处理
        // 注意：在准实时的计算中，请不要出现耗时操作、加锁操作，这会严重降低实时性能

        return executeResult;
    }

}
```

> **关于组件执行的异常处理**
>
> 如果是业务逻辑的异常，请自行 `try-catch`，如果是系统级的异常，请抛出到上层由引擎处理。
>
> - 组件类型的定义 `pro.octet.senses.bot.core.enums.ActionType`
> - 组件异常的定义 `pro.octet.senses.bot.exception.ActionExecutionException`

## 一些答疑

```text
Q：是否可以基于该项目实现一些自己的业务场景？
A：完全可以。

Q：是否涉及商业版权问题？
A：涉及，如果你需要进行商业开发，请联系我。

Q：我是否可以提交PR？
A：欢迎一切有想法的创意加入进来！

Q：为什么没有管理端服务？
A：本项目是事件引擎的主要实现，管理端服务并不重要，你可以基于自己的想法去重新设计它。
```

## 问题反馈

- 如果你有任何疑问，欢迎在GitHub Issue中提交。

----

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
