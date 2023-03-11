# RocketMq（分布式消息队列）学习笔记
## RocketMQ概述
### MQ概述
1. MQ：message queue，是提供消息队列服务的中间件
2. **MQ的用途**：
    - 削峰填谷
    - 异步解耦
    - 数据收集
3. 常见MQ产品：ActiveMQ、RabbitMQ、Kafka、RocketMQ
### RocketMQ
1. 来自阿里巴巴
2. 根据Kafka改造而来
3. 承载了万亿级的消息流转
## RocketMQ的安装与启动
### 基本概念
1. 消息（message）：就是数据
2. 主题（topic）：表示一类消息的集合
    - topic ： message = 1 ： n （一个topic可以有许多个message）
    - message ： topic = 1 ： 1 （一个message 只能有一个 topic）
    - producer ： topic = 1 ： n （一个生产者可以发送多条消息）
    - consumer ： topic = 1 ： 1 （一个消费者只能消费一个特定topic的消息）
3. 标签（tag）：为消息设置的标签，用于同一主题下区分不同类型的消息。Topic是消息的一级分类，Tag是消息的二级分类。
4. 队列（Queue）：存储消息的地方。
    - 一个topic里面包含多个queue。也就是说一类主题的n个消息分别存放在不同的queue上
5. 消息标识（MessageId/Key）
    - key： 由用户指定的业务相关的唯一标识
    - msgId：由producer端生成
    - offsetMsgId：由broker端生成
### 系统架构
**（photo）**  
#### producer
1. 消息生产者，负责生产消息
2. 通过MQ的负载均衡模块把消息投递到相应的Broker
#### consumer
1. 消费者，负责消费消息
2. 消费者是通过消费组的形式，消费同一个topic的消息
3. 消费组（多个消费者）的作用：负载均衡和容错
4. 一个topic类型的消息可以被多个消费组消费，但一个消费组只能消费一个topic类型的消息
#### NameServer
1. NameServer是Broker和Topic路由的**注册中心**，支持Broker的动态注册
2. 功能介绍：
    - Broker管理：
        - 接受Broker集群的注册信息并且保存下来作为路由信息的基本数据
        - 提供心跳检测机制，检查Broker是否还存活
    - 路由信息管理：
        - 每个NameServer中都保存着Broker集群的整个路由信息和用于客户端查询的队列（queue）信息。
        - Producer和Conumser通过NameServer可以获取整个Broker集群的路由信息，从而进行消息的投递和消费。
 3. 路由注册
    - Broker启动时主动轮询每一个NameServer，进行注册
    - Broker每30秒向NameServer发送一次心跳包，以证明自己还活着
 4. 路由剔除
    - 如果120秒内没有收到Broker的心跳包，NameServer会把其从Broker列表中剔除
 5. 路由发现
    - 时客户端主动拉取路由信息（pull模式），30s拉一次
    - 客户端会先采取随机策略进行选择，失败后采取轮询策略
#### Broker
 1. 功能介绍：Broker充当着消息中转角色，负责存储消息、转发消息
 2. 集群部署：主从集群
#### 工作流程
 1. 启动NameServer，NameServer启动后开始监听端口，等待Broker、Producer、Consumer连接
 2. 启动Broker时，Broker会与所有的NameServer建立并保持长连接，然后每30秒向NameServer定时发送心跳包
 3. 创建Topic，指定该Topic要存储在哪些Broker上（可选，发送消息时可自动创建）
 4. 启动Producer，获取NameServer的路由信息（queue与broker的映射），最后发送消息。  
 获取的路由信息会被保存在本地，然后每30秒从NameServer更新一次路由信息
 5. 启动consumer，获取NameServer的路由信息（queue与broker的映射），最后消费消息。   
 获取的路由信息会被保存在本地，然后每30秒从NameServer更新一次路由信息  
 另外consumer还会向发送心跳，以确保Broker的存活状态
 #### Topic的创建模式
 1. 手动创建Topic
    - 集群模式：该模式下创建的Topic在该集群中，所有Broker中的Queue数量是相同的。
    - Broker模式：该模式下创建的Topic在该集群中，每个Broker中的Queue数量可以不同。
 2. 自动创建Topic时，默认采用的是Broker模式，会为每个Broker默认创建4个Queue
 #### 读/写队列
 1. 从物理上来讲，读/写队列是同一个队列。所以，不存在读/写队列数据同步问题。
 2. 读/写队列是逻辑上进行区分的概念。一般情况下，读/写队列数量是相同的。 
 3. 读/写队列数量是不同是为了方便队列（queue）的扩容或者缩容
    