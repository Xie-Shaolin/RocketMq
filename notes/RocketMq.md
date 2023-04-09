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
![045](img/045.png )
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
### 单机安装与启动
1. 官网：https://rocketmq.apache.org/
2. 选择Binary 下载
 ![047](img/047.png )
3. 在linux可视化的模式下，可以直接把文件拖入到指定目录
4. 解压zip文件：unzip [参数] [压缩文件]
5. 修改初始内存： vim命令修改bin/runserver.sh文件
 ![048](img/048.png )
6. 使用vim命令修改bin/runbroker.sh文件
 ![049](img/049.png )
7. 启动NameServer
```shell
### 启动namesrv
$ nohup sh bin/mqnamesrv &
### 验证namesrv是否启动成功
$ tail -f ~/logs/rocketmqlogs/namesrv.log
The Name Server boot success...
```
8. 启动broker
```shell
### 先启动broker  声明了地址为9876
$ nohup sh bin/mqbroker -n localhost:9876  &
### 验证broker是否启动成功, 比如, broker的ip是192.168.1.2 然后名字是broker-a
$ tail -f ~/logs/rocketmqlogs/broker.log
The broker[broker-a,192.169.1.2:10911] boot success…
2023-03-24 00:25:56 INFO main - The broker[localhost.localdomain, 192.168.124.160:10911] boot success. serializeType=JSON and name server is localhost:9876
```
9. 发送/接收消息测试
```shell
# 工具测试发生消息
$ export NAMESRV_ADDR=localhost:9876
$ sh bin/tools.sh org.apache.rocketmq.example.quickstart.Producer
 SendResult [sendStatus=SEND_OK, msgId= ...
 
 
 # 工具测试接收消息
 $ sh bin/tools.sh org.apache.rocketmq.example.quickstart.Consumer
 ConsumeMessageThread_%d Receive New Messages: [MessageExt...
```
10. 关闭Server: 无论是关闭name server还是broker，都是使用bin/mqshutdown命令
```shell
sh bin/mqshutdown broker
sh bin/mqshutdown namesrv
```
### 控制台的安装与启动
1. [参考文章](https://blog.csdn.net/m_lonel/article/details/126591465)
2. 新版本地址：https://github.com/apache/rocketmq-dashboard
3. 修改配置文件：rocketmq-dashboard-rocketmq-dashboard-1.0.0\src\main\resources\application.properties
   ![050](img/050.png )
4. 使用maven打包：**mvn clean package -Dmaven.test.skip=true**。要记得在rocketmq-dashboard-rocketmq-dashboard-1.0.0文件夹外。在Git Bash里面执行
   ![051](img/051.png )
5. 去target文件夹里面启动jar包：**java -jar rocketmq-dashboard-1.0.0.jar**
### 集群搭建理论
#### 数据的负载与刷盘
![046](img/046.png )
- 复制策略：Master与Slave间的数据同步方式
  - 同步复制
  - 异步复制
- 刷盘策略：内存持久化至磁盘
  - 同步刷盘
  - 异步刷盘
#### Broker集群模式
- 单Master：不是集群
- 多Master+RAID10：同一Topic的各个Queue会平均分布在各个master节点上
  - 优点：不会丢失消息：异步刷盘丢失少量消息（RAID10微秒级），同步刷盘一条不丢
  - 缺点：宕机期间无法消费
- 多Master+多Slave模式（异步复制）：没有RAID磁盘阵列
  - 优点：当master宕机后slave能够自动切换为master
  - 缺点：master同步数据到slave，①效率比RAID慢（毫秒级），②丢失的数据要比RAID多
- 多Master+多Slave模式（同步复制）：没有RAID磁盘阵列
  - 优点：不会丢失消息
  - 缺点：效率低
- 最佳实践：Master+RAID10+一个Slave
### 磁盘阵列RAID
#### 关键技术
- 镜像技术：备份，防止数据丢失
- 数据条带技术：多进程IO，提高IO效率
- 数据校验技术：写入数据的时候进行校验计算。当其中一部分数据出错时，就可以对剩余数据和校验数据进行反校验计算重建丢失的数据。节省了磁盘，但是降低了效率
#### 常见RAID等级详解
- JBOD：只是简单提供一种扩展存储空间的机制，JBOD可用存储容量等于所有成员磁盘的存储空间之和
- RAID0：利用条带技术，提高了IO效率，但是没有镜像，不提供数据安全
- RAID1：只提供了镜像，没有条带技术，效率低
- RAID10：先条带，再镜像，容错率比RAID01高
- RAID01：先镜像，再条带，容错率比RAID10低
### 搭建集群
课程搭建了一个双主双从异步复制的Broker集群。但是为了方便，使用了两台主机来完成集群的搭建。

例如 mqA 是Master1 + Slave2； 而mqB 是Master2 + Slave1

#### 修改配置
broker-a.properties/broker-b.properties
``` properties
# 指定整个broker集群的名称，或者说是RocketMQ集群的名称
brokerClusterName=DefaultCluster
# 指定master-slave集群的名称。一个RocketMQ集群可以包含多个master-slave集群
brokerName=broker-a
# master的brokerId为0
brokerId=0
# 指定删除消息存储过期文件的时间为凌晨4点
deleteWhen=04
# 指定未发生更新的消息存储文件的保留时长为48小时，48小时后过期，将会被删除
fileReservedTime=48
# 指定当前broker为异步复制master
brokerRole=ASYNC_MASTER
# 指定刷盘策略为异步刷盘
flushDiskType=ASYNC_FLUSH
# 指定Name Server的地址
namesrvAddr=192.168.59.164:9876;192.168.59.165:9876
```
broker-a-s.properties/broker-b-s.properties
```properties
brokerClusterName=DefaultCluster
# 指定这是另外一个master-slave集群
brokerName=broker-b
# slave的brokerId为非0
brokerId=1
deleteWhen=04
fileReservedTime=48
# 指定当前broker为slave
brokerRole=SLAVE
flushDiskType=ASYNC_FLUSH
namesrvAddr=192.168.59.164:9876;192.168.59.165:9876
# 指定Broker对外提供服务的端口，即Broker与producer与consumer通信的端口。默认10911。由于当前主机同时充当着master1与slave2，而前面的master1使用的是默认端口。这里需要将这两个端口加以区分，以区分出master1与slave2
listenPort=11911
# 指定消息存储相关的路径。默认路径为~/store目录。由于当前主机同时充当着master1与slave2，master1使用的是默认路径，这里就需要再指定一个不同路径
storePathRootDir=~/store-s
storePathCommitLog=~/store-s/commitlog
storePathConsumeQueue=~/store-s/consumequeue
storePathIndex=~/store-s/index
storeCheckpoint=~/store-s/checkpoint
abortFile=~/store-s/abort
```
其他配置
```properties
#指定整个broker集群的名称，或者说是RocketMQ集群的名称
brokerClusterName=rocket-MS
#指定master-slave集群的名称。一个RocketMQ集群可以包含多个master-slave集群
brokerName=broker-a
#0 表示 Master，>0 表示 Slave
brokerId=0
#nameServer地址，分号分割
namesrvAddr=nameserver1:9876;nameserver2:9876
#默认为新建Topic所创建的队列数
defaultTopicQueueNums=4
#是否允许 Broker 自动创建Topic，建议生产环境中关闭
autoCreateTopicEnable=true
#是否允许 Broker 自动创建订阅组，建议生产环境中关闭
autoCreateSubscriptionGroup=true
#Broker对外提供服务的端口，即Broker与producer与consumer通信的端口
listenPort=10911
#HA高可用监听端口，即Master与Slave间通信的端口，默认值为listenPort+1
haListenPort=10912
#指定删除消息存储过期文件的时间为凌晨4点
deleteWhen=04
#指定未发生更新的消息存储文件的保留时长为48小时，48小时后过期，将会被删除
fileReservedTime=48
#指定commitLog目录中每个文件的大小，默认1G
mapedFileSizeCommitLog=1073741824
#指定ConsumeQueue的每个Topic的每个Queue文件中可以存放的消息数量，默认30w条
mapedFileSizeConsumeQueue=300000
#在清除过期文件时，如果该文件被其他线程所占用（引用数大于0，比如读取消息），此时会阻止此次删除任务，同时在第一次试图删除该文件时记录当前时间戳。
#该属性则表示从第一次拒绝删除后开始计时，该文件最多可以保留的时长。在此时间内若引用数仍不为0，则删除仍会被拒绝。不过时间到后，文件将被强制删除
destroyMapedFileIntervalForcibly=120000
#指定commitlog、consumequeue所在磁盘分区的最大使用率，超过该值，则需立即清除过期文件
diskMaxUsedSpaceRatio=88
#指定store目录的路径，默认在当前用户主目录中
storePathRootDir=/usr/local/rocketmq-all-4.5.0/store
#commitLog目录路径
storePathCommitLog=/usr/local/rocketmq-all-4.5.0/store/commitlog
#consumeueue目录路径
storePathConsumeQueue=/usr/local/rocketmq-all-4.5.0/store/consumequeue
#index目录路径
storePathIndex=/usr/local/rocketmq-all-4.5.0/store/index
#checkpoint文件路径
storeCheckpoint=/usr/local/rocketmq-all-4.5.0/store/checkpoint
#abort文件路径
abortFile=/usr/local/rocketmq-all-4.5.0/store/abort
#指定消息的最大大小
maxMessageSize=65536
#Broker的角色
# - ASYNC_MASTER 异步复制Master
# - SYNC_MASTER 同步双写Master
# - SLAVE
brokerRole=SYNC_MASTER
#刷盘策略
# - ASYNC_FLUSH 异步刷盘
# - SYNC_FLUSH 同步刷盘
flushDiskType=SYNC_FLUSH
#发消息线程池数量
sendMessageThreadPoolNums=128
#拉消息线程池数量
pullMessageThreadPoolNums=128
#强制指定本机IP，需要根据每台机器进行修改。官方介绍可为空，系统默认自动识别，但多网卡
时IP地址可能读取错误
brokerIP1=192.168.3.105
```
#### 启动服务器
##### 启动NameServer集群: 两个都相同
```
nohup sh bin/mqnamesrv &
tail -f ~/logs/rocketmqlogs/namesrv.log
```
##### 启动两个Master
```
nohup sh bin/mqbroker -c conf/2m-2s-async/broker-a.properties &
tail -f ~/logs/rocketmqlogs/broker.log

nohup sh bin/mqbroker -c conf/2m-2s-async/broker-b.properties &
tail -f ~/logs/rocketmqlogs/broker.log
```
##### 启动两个Slave
```
nohup sh bin/mqbroker -c conf/2m-2s-async/broker-b-s.properties &
tail -f ~/logs/rocketmqlogs/broker.log

nohup sh bin/mqbroker -c conf/2m-2s-async/broker-a-s.properties &
tail -f ~/logs/rocketmqlogs/broker.log
```
### mqadmin命令: web控制台都可以实现

## RocketMQ工作原理
### 消息的生产
#### 消息的生产过程：Producer可以将消息写入到某Broker中的某Queue中
1. Producer会先向NameServer发出获取消息Topic的路由信息的请求
2. NameServer返回该Topic的**路由表**及**Broker列表** （找到消息发送的地方）
   - 路由表：一个map，key是Topic，value是QueueData。QueueData包含BrokerName列表以及brokerName对应的queue
   - Broker列表：一个map，key为brokerName，value为BrokerData。BrokerData包含对应brokerName的master和slave的地址
3. Producer根据代码中指定的Queue选择策略，从Queue列表中选出一个队列
4. Producer向选择出的Queue所在的Broker发出RPC请求，将消息发送到选择出的Queue
#### Queue选择算法（消息投递算法）（无序消息）
- 轮询算法：生产者的消息堆积
- 最小投递延迟算法：消费者的消息堆积
### 消息的存储
#### store目录
RocketMQ中的消息存储在本地文件系统中，这些相关文件默认在当前用户主目录下的store目录中。
#### commitlog目录
commitlog目录中存放着很多的mappedFile文件，当前Broker中的所有消息都是落盘到这些mappedFile文件中的

一个Broker中仅包含一个commitlog目录，所有的mappedFile文件都是存放在该目录中的。<br>
即这些消息在Broker中存放时并没有被按照Topic进行分类存放。

mappedFile文件内容由一个个的消息单元构成。<br>
每个消息单元中包含消息总长度MsgLen、消息的物理位置physicalOffset、**消息体内容Body**、消息体长度BodyLength、
**消息主题Topic**、Topic长度TopicLength、**消息生产者BornHost**、消息发送时间戳BornTimestamp、
**消息所在的队列QueueId**、**消息在Queue中存储的偏移量QueueOffset**等近20余项消息相关属性。
#### consumequeue
目录为：/store/consumequeue/topicName/queueId/consumequeue文件

consumequeue文件是commitlog的索引文件，可以根据consumequeue定位到具体的消息。

每个consumequeue文件可以包含30w个**索引条目**，每个索引条目包含了三个消息重要属性：
消息在mappedFile文件中的偏移量CommitLog Offset、消息长度、**消息Tag的hashcode值**。
一个consumequeue文件中所有消息的Topic一定是相同的。**但每条消息的Tag可能是不同的**。

#### 对文件的读写
1.消息写入
- Broker根据queueId，获取到该消息对应索引条目要在consumequeue目录中的写入偏移量，即QueueOffset（之前写到哪了）
- 将queueId、queueOffset等数据，与消息一起封装为消息单元
- 将消息单元写入到commitlog。同时，形成消息索引条目。将消息索引条目分发到相应的consumequeue
2.消息拉取
- Consumer获取到其要消费消息所在Queue的消费偏移量offset ，计算出其要消费消息的消息offset（之前消费到哪了）
- Consumer向Broker发送拉取请求，其中会包含其要拉取消息的Queue、消息offset及消息Tag。
- Broker计算在该consumequeue中的queueOffset。
- 从该queueOffset处开始向后查找第一个指定Tag的索引条目。
- 解析该索引条目的前8个字节，即可定位到该消息在commitlog中的commitlog offset
- 从对应commitlog offset中读取消息单元，并发送给Consumer
### indexFile（略）
除了通过通常的指定Topic进行消息消费外，RocketMQ还提供了根据key进行消息查询的功能。该查询是通过store目录中的index子目录中的indexFile进行索引实现的快速查询
### 消息的消费
#### 获取消费类型 
- 拉取式消费(pull)：<br>
  Consumer主动从Broker中拉取消息，主动权由Consumer控制。一旦获取了批量消息，就会启动消费过程。不过，该方式的实时性较弱，即Broker中有了新的消息时消费者并不能及时发现并消费。
- 推送式消费(push):<br>
  该模式下Broker收到数据后会主动推送给Consumer。该获取方式一般实时性较高。该获取方式是典型的发布-订阅模式，即Consumer向其关联的Queue注册了监听器，一旦发现有新的
  消息到来就会触发回调的执行，回调方法是Consumer去Queue中拉取消息。而这些都是基于Consumer与Broker间的**长连接**的。长连接的维护是需要消耗系统资源的。
#### 消费模式
- 广播消费: 
  - 每条消息都会被发送到Consumer Group中的**每个**Consumer。
  - 消费进度保存在consumer端
- 集群消费: 
  - 每条消息只会被发送到Consumer Group中的**某个**Consumer。
  - 消费进度保存在broker中。
#### Rebalance机制
- Rebalance机制讨论的前提是：集群消费。
- Rebalance危害
  - 消费暂停
  - 消费重复
  - 消费突刺
- Rebalance过程： 
  - 消费者的Queue数量或消费者组中消费者的数量发生变化，
  - Broker中维护着的Map集合就会发生变化
  - 立即向Consumer Group中的每个实例发出Rebalance通知
  - Consumer实例在接收到通知后会采用Queue分配算法自己获取到相应的Queue
- Queue分配算法
  - 平均分配策略
  - 环形平均策略
  - 一致性hash策略：减少Rebalance的影响
  - 同机房策略
#### 至少一次原则
RocketMQ有一个原则：每条消息必须要被成功消费一次。

成功消费：Consumer在消费完消息后会向其消费进度记录器提交其消费消息的offset，offset被成功记录到记录器中，那么这条消费就被成功消费了。<br>

消费进度记录器:
- 对于广播消费模式来说，Consumer本身就是消费进度记录器
- 对于集群消费模式来说，Broker是消费进度记录器。
### 订阅关系的一致性
订阅关系的一致性指的是，同一个消费者组（Group ID相同）下所有Consumer实例所订阅的**Topic**与**Tag**及对**消息的处理逻辑**必须完全一致。
否则，消息消费的逻辑就会混乱，甚至导致消息丢失。
### offset管理
1. 这里的offset指的是Consumer的消费进度offset。
2. 两种模式
   - offset本地管理模式: 广播消费
   - offset远程管理模式: 集群消费
3. offset用途: 从哪里开始消费
4. 重试队列 ： 存储发生异常的offset
5. offset的同步提交与异步提交
   - 在集群消费下
   - 同步提交： 
     - 要等待broker的响应
     - 从ACK中获取nextBeginOffset（下次消费的起始offset）
     - 严重影响了消费者的吞吐量
   - 异步提交：
     - 无需等待Broker的成功响应
     - Consumer会从Broker中直接获取nextBeginOffset
     - 增加了消费者的吞吐量
### 消费幂等
1. 什么是消费幂等
   - 幂等：若某操作执行多次与执行一次对系统产生的影响是相同的，则称该操作是幂等的。
   - 当出现消费者对某条消息重复消费的情况时，重复消费的结果与消费一次的结果是相同的，并且多次消费并未对业务系统产生任何负面影响，那么这个消费过程就是消费幂等的。
2. 消息重复的场景分析
   - 发送时消息重复
   - 消费时消息重复
   - Rebalance时消息重复
3. 通用解决方案
   - 首先通过缓存去重。在缓存中如果已经存在了**某幂等令牌（如流水号）**，则说明本次操作是重复性操作；若缓存没有命中，则进入下一步。
   - 在唯一性处理之前，先在数据库中查询幂等令牌作为索引的数据是否存在。若存在，则说明本次操作为重复性操作；若不存在，则进入下一步。
   - 在同一事务中完成三项操作：唯一性处理后，将幂等令牌写入到缓存，并将幂等令牌作为唯一索引的数据写入到DB中。
### 消息堆积与消费延迟
1. 概念：
   消息处理流程中，如果Consumer的消费速度跟不上Producer的发送速度，MQ中未处理的消息会越来越多（进的多出的少），这部分消息就被称为堆积消息。
   消息出现堆积进而会造成消息的消费延迟。
2. 产生原因分析
    - 消费耗时
    - 消费并发度
3. 消费耗时
    - CPU内部计算型代码
      - 复杂的递归和循环
      - 其他可以忽略不记
    - 外部I/O操作型代码
      - 读写外部数据库
      - 读写外部缓存系统
      - 下游系统调用
        - 服务异常
        - 达到了DBMS容量限制
4. 消费并发度
    - 一般情况：消费并发度=单节点线程数 * 节点数量
      - 单节点线程数：即单个Consumer所包含的线程数量
      - 节点数量：即Consumer Group所包含的Consumer数量
    - 顺序消费
      - 全局顺序消息： 消费并发度=1
      - 分区顺序消息： 消费并发度= Topic的分区（queue）数量
5. 单机线程数计算
   - 最优线程数计算模型为：C *（T1 + T2）/ T1 = C + C * T2/T1
     - C：CPU内核数
     - T1：CPU内部逻辑计算耗时
     - T2：外部IO操作耗时
   - 实际工作中：在最优模型附近经行压测，测出最有的线程数
6. 节点数 = 流量峰值 / 单个节点消息吞吐量
### 消息的清理
1. 消息被消费过后不会被马上清理掉
2. 消息的清理不是以消息为单位进行清理的，而是以commitlog文件为单位进行清理的
3. ommitlog文件存在一个过期时间，默认为72小时，即三天
4. 过期文件会被自动清理
## RocketMQ应用
### 普通消息
#### 消息发送分类
- 同步发送消息
- 异步发送消息
- 单向发送消息
#### 代码
##### 引入依赖，构建项目
```xml
  <!-- 版本号要和使用的rocketmq一致   -->
  <dependencies>
    <dependency>
      <groupId>org.apache.rocketmq</groupId>
      <artifactId>rocketmq-client</artifactId>
      <version>4.8.0</version>
    </dependency>
  </dependencies>
```
**使用maven构建项目**
![005](img/005.jpg )
##### 定义同步消息发送生产者
```java
public class SyncProducer {
    public static void main(String[] args) throws Exception {
        // 创建一个producer，参数为Producer Group名称
        DefaultMQProducer producer = new DefaultMQProducer("pg");
        // 指定nameServer地址
        producer.setNamesrvAddr("192.168.124.160:9876");
        // 设置当发送失败时重试发送的次数，默认为2次
        producer.setRetryTimesWhenSendFailed(3);
        // 设置发送超时时限为5s，默认3s
        producer.setSendMsgTimeout(5000);
        // 开启生产者
        producer.start();
        // 生产并发送100条消息
        for (int i = 0; i < 100; i++) {
            byte[] body = ("Hi," + i).getBytes();
            Message msg = new Message("someTopic", "someTag", body);
            // 为消息指定key
            msg.setKeys("key-" + i);
            // 发送消息
            SendResult sendResult = producer.send(msg);
            System.out.println(sendResult);
        }
        // 关闭producer
        producer.shutdown();
    }
}
```
![001](img/001.png )

```java
// SendResult 是producer的返回结果
public class SendResult {
    private SendStatus sendStatus;
    private String msgId;
    private MessageQueue messageQueue;
    private long queueOffset;
    private String transactionId;
    private String offsetMsgId;
    private String regionId;
    private boolean traceOn = true;
    /*.....省略......*/
}
```
```java
// 消息发送的状态
public enum SendStatus {
    // 发送成功    
    SEND_OK,
    // 刷盘超时。当Broker设置的刷盘策略为同步刷盘时才可能出现这种异常状态。异步刷盘不会出现
    FLUSH_DISK_TIMEOUT,
    // Slave同步超时。当Broker集群设置的Master-Slave的复制方式为同步复制时才可能出现这种异常状态。异步复制不会出现
    FLUSH_SLAVE_TIMEOUT,
    // 没有可用的Slave。当Broker集群设置为Master-Slave的复制方式为同步复制时才可能出现这种异常状态。异步复制不会出现
    SLAVE_NOT_AVAILABLE, 
}
```

##### 定义异步消息发送生产者
```java
public class AsyncProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("pg");
        producer.setNamesrvAddr("192.168.124.160:9876");
        // 指定异步发送失败后不进行重试发送
        producer.setRetryTimesWhenSendAsyncFailed(0);
        // 指定新创建的Topic的Queue数量为2，默认为4
        producer.setDefaultTopicQueueNums(2);
        producer.start();
        for (int i = 0; i < 100; i++) {
            byte[] body = ("Hi," + i).getBytes();
            try {
                Message msg = new Message("myTopicA", "myTag", body);
                // 异步发送。指定回调
                producer.send(msg, new SendCallback() {
                    // 当producer接收到MQ发送来的ACK后就会触发该回调方法的执行
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        System.out.println(sendResult);
                    }
                    @Override
                    public void onException(Throwable e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } // end-for
        // sleep一会儿
        // 由于采用的是异步发送，所以若这里不sleep，
        // 则消息还未发送就会将producer给关闭，报错
        TimeUnit.SECONDS.sleep(3);
        producer.shutdown();
    }
}
```
![002](img/002.jpg )

##### 定义单向消息发送生产者
```java
public class OnewayProducer {
    public static void main(String[] args) throws Exception{
        DefaultMQProducer producer = new DefaultMQProducer("pg");
        producer.setNamesrvAddr("192.168.124.160:9876");
        producer.start();
        for (int i = 0; i < 10; i++) {
            byte[] body = ("Hi," + i).getBytes();
            Message msg = new Message("single", "someTag", body);
            // 单向发送
            producer.sendOneway(msg);
        }
        producer.shutdown();
        System.out.println("producer shutdown");
    }
}
```
![003](img/003.jpg )
##### 定义消息消费者
```java
public class SomeConsumer {
    public static void main(String[] args) throws MQClientException {
        // 定义一个pull消费者
        // DefaultLitePullConsumer consumer = new DefaultLitePullConsumer("cg");
        // 定义一个push消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("cg");
        // 指定nameServer
        consumer.setNamesrvAddr("192.168.124.160:9876");
        // 指定从第一条消息开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        // 指定消费topic与tag
        consumer.subscribe("someTopic", "*");
        // 指定采用“广播模式”进行消费，默认为“集群模式”
        // consumer.setMessageModel(MessageModel.BROADCASTING);
        // 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            // 一旦broker中有了其订阅的消息就会触发该方法的执行，
            // 其返回值为当前consumer消费的状态
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                // 逐条消费消息
                for (MessageExt msg : msgs) {
                    System.out.println(msg);
                }
                // 返回消费状态：消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 开启消费者消费
        consumer.start();
        System.out.println("Consumer Started");
    }
}
```
![004](img/004.jpg )
### 顺序消费
#### 有序性分类
- 全局有序：一个Topic里面只有一个queue
- 分区有序：一个Topic里面只有多个queue,但单个queue里面是有序的
#### 代码
##### 生产者生产消息
```java
public class OrderedProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("pg");
        producer.setNamesrvAddr("192.168.124.160:9876");
        // 指定新创建的Topic的Queue数量为1时，为全局有序。默认为4
        // producer.setDefaultTopicQueueNums(1);
        producer.start();
        for (int orderId = 0; orderId < 100; orderId++) {

            for (int j = 0; j<4; j++){
                String status="";
                if(j==0){
                    status="未支付";
                }else if(j==1){
                    status="已支付";
                }else if(j==2){
                    status="发货中";
                }else {
                    status="已收货";
                }
                byte[] body = ("orderId="+orderId+"status="+status ).getBytes();
                Message msg = new Message("TopicA", "TagA", body);
                SendResult sendResult = producer.send(msg, new
                        MessageQueueSelector() {
                            @Override
                            public MessageQueue select(List<MessageQueue> mqs,
                                                       Message msg, Object arg) {
                                Integer id = (Integer) arg;
                                int index = id % mqs.size();
                                return mqs.get(index);
                            }
                        }, orderId);
                System.out.println("["+orderId+"]"+sendResult);
            }

        }
        producer.shutdown();
    }
}
```
![006](img/006.png )
##### 消费者消费消息
```java
public class OrderConsumer {
    public static void main(String[] args) throws MQClientException {
        // 定义一个pull消费者
        // DefaultLitePullConsumer consumer = new DefaultLitePullConsumer("cg");
        // 定义一个push消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("cg");
        // 指定nameServer
        consumer.setNamesrvAddr("192.168.124.160:9876");
        // 指定从第一条消息开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        // 指定消费topic与tag
        consumer.subscribe("TopicA", "*");
        // 指定采用“广播模式”进行消费，默认为“集群模式”
        // consumer.setMessageModel(MessageModel.BROADCASTING);
        // 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            // 一旦broker中有了其订阅的消息就会触发该方法的执行，
            // 其返回值为当前consumer消费的状态
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                // 逐条消费消息
                for (MessageExt msg : msgs) {
                    int queueId = msg.getQueueId();
                    String keys = msg.getKeys();
                    String msgId = msg.getMsgId();
                    byte[] body = msg.getBody();
                    String bodyStr = new String(body);
                    System.out.println("msgId:"+msgId+"  keys:"+keys+"    queueId:"+queueId+" bodyStr:"+bodyStr);
                    //System.out.println(msg);
                }
                // 返回消费状态：消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 开启消费者消费
        consumer.start();
        System.out.println("Consumer Started");
    }
}
```
![007](img/007.jpg )
### 延时消息
#### 什么时延时消息
当消息写入到Broker后，在指定的时长后才可被消费处理的消息，称为延时消息。
#### 延时等级
- 延时消息的延迟时长不支持随意时长的延迟
- 延时等级定义在RocketMQ服务端的MessageStoreConfig 类中<br>
  ![008](img/008.jpg )
- 等级从1开始，也就是指定3等级，意味延迟10秒
- 自定义延迟等级可以在RocketMQ安装目录下的conf目录配置<br>
  ![009](img/009.jpg )
    ```
    messageDelayLevel = 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h 1d
    ```
#### 延时消息实现原理
![010](img/010.jpg )
1. 修改消息
   - 修改消息的Topic为SCHEDULE_TOPIC_XXXX
   - 在 SCHEDULE_TOPIC_XXXX 创建queueId目录和consumequeue文件。queueId目录与延迟等级想对应。（为 queueId = delayLevel -1）
   - 修改消息索引单元内容：Message Tag HashCode 改为 投递时间
     - 投递时间 = 消息存储时间 + 延时等级时间
   - 将消息索引写入到SCHEDULE_TOPIC_XXXX主题下，相应延迟等级（queueId）下的consumequeue中
     - 在各个延时等级Queue中，消息是根据投递时间排序。 但相同的queue，延时等级时间相同，所以最终跟根据消息存储时间排序
2. 投递延时消息
   - Broker内部有⼀个延迟消息服务类ScheuleMessageService，其会消费SCHEDULE_TOPIC_XXXX中的消息
3. 将消息重新写入commitlog
   - 延迟消息服务类ScheuleMessageService将延迟消息再次发送给了commitlog，并再次形成新的消息索引条目，分发到相应Queue。
#### 代码
##### 生产者
```java
public class DelayProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("pg");
        producer.setNamesrvAddr("192.168.124.160:9876");
        producer.start();
        for (int i = 0; i < 1; i++) {
            byte[] body = ("Hi," + i).getBytes();
            Message msg = new Message("TopicB", "delayTag", body);
            // 指定消息延迟等级为3级，即延迟10s
            msg.setDelayTimeLevel(3);
            SendResult sendResult = producer.send(msg);
            // 输出消息被发送的时间
            System.out.print(new SimpleDateFormat("mm:ss").format(new Date()));
            System.out.println(" ," + sendResult);
        }
        producer.shutdown();
    }
}
```
##### 消费者
```java
public class DelayConsumer {
    public static void main(String[] args) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("cg");
        consumer.setNamesrvAddr("192.168.124.160:9876");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET
        );
        consumer.subscribe("TopicB", "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    // 输出消息被消费的时间
                    System.out.print(new SimpleDateFormat("mm:ss").format(new Date()));
                    System.out.println(" ," + msg);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.out.println("Consumer Started");
    }
}
```
#### 效果
- 在store目录下生成了SCHEDULE_TOPIC_XXXX<br>
  ![011](img/011.png) 
- 37:40生产消息<br>
  ![012](img/012.jpg) 
- 37:50消费消息<br>
  ![013](img/013.png) 
### 事务消息
#### 问题引入
这里的一个需求场景是：工行用户A向建行用户B转账1万元。

![014](img/014.png) 

如果第3步从用户A里面扣款失败了，但是消息已经投递到了Broker里面，很可能就已经被建行系统消费了。
#### 解决思路
让1、2和3步具有原子性，要么全部成功，要么全部失败。<br>
即消息发送成功后，必须要保证扣款成功。如果扣款失败，则回滚发送成功的消息。<br>
而该思路即使用事务消息，这里要使用分布式事务解决方案。<br>
![015](img/015.png) 
在第6步和第7步中，预扣款执行结果（**本地事务的执行状态**）存在三种可能性：
```java
// 描述本地事务执行状态
public enum LocalTransactionState {
    COMMIT_MESSAGE, // 本地事务执行成功
    ROLLBACK_MESSAGE, // 本地事务执行失败
    UNKNOW, // 不确定，表示需要进行回查以确定本地事务的执行结果
}
```
在第8步中：TM会根据上报结果向TC发出不同的确认指令
> - 若预扣款成功（本地事务状态为COMMIT_MESSAGE），则TM向TC发送Global Commit指令
> - 若预扣款失败（本地事务状态为ROLLBACK_MESSAGE），则TM向TC发送Global Rollback指令
> - 若现未知状态（本地事务状态为UNKNOW），则会触发工行系统的本地事务状态回查操作。
>   - 回查操作会将回查结果，即COMMIT_MESSAGE或ROLLBACK_MESSAGE Report给TC。
>   - TC将结果上报给TM，TM会再向TC发送最终确认指令Global Commit或Global Rollback

在第9步中： TC在接收到指令后会向Broker与工行系统发出确认指令
> - TC接收的若是Global Commit指令，则向Broker与工行系统发送Branch Commit指令。此时Broker中的消息M才可被建行系统看到；此时的工行用户A中的扣款操作才真正被确认
> - TC接收到的若是Global Rollback指令，则向Broker与工行系统发送Branch Rollback指令。此时Broker中的消息M将被撤销；工行用户A中的扣款操作将被回滚

#### 一些分布式事务的概念
- **分布式事务**：一次操作由若干个分支操作组成，但这些分支操作分布在不同的应用中。分布式事务需要保证这些操作要么都成功，要么都失败
- **事务消息**：RocketMQ提供的类似X/Open XA的分布式事务功能
- **半事务消息**：消息已经投递到Broker，但是Broker未收到最终确认指令（分支操作都成功了），消费者不能看到也不能消费
- **本地事务状态**：Producer 回调操作执行的结果为本地事务状态
- **消息回查**：即重新查询本地事务的执行状态。本例就是重新到DB中查看预扣款操作是否执行成功。<br>
  ![016](img/016.png)
  - 引发消息回查的原因最常见的有两个：
    - 回调操作返回UNKNWON
    - TC没有接收到TM的最终全局事务确认指令
  - RocketMQ中的消息回查设置: 它们都在broker加载的配置文件中设置
    - transactionTimeout=20，指定TM在20秒内应将最终确认状态发送给TC，否则引发消息回查。默认为60秒
    - transactionCheckMax=5，指定最多回查5次，超过后将丢弃消息并记录错误日志。默认15次。
    - transactionCheckInterval=10，指定设置的多次消息回查的时间间隔为10秒。默认为60秒。
#### XA模式三剑客
- XA（Unix Transaction）是一种分布式事务解决方案，一种分布式事务处理模式。XA模式中有三个重要组件：TC、TM、RM
- TC：Transaction Coordinator，事务协调者。维护全局和分支事务的状态，驱动全局事务提交或回滚
  - RocketMQ中Broker中的一个线程充当着TC
- Transaction Manager，事务管理器。定义全局事务的范围：开始全局事务、提交或回滚全局事务。它实际是全局事务的发起者
  - RocketMQ中事务消息的Producer中的一个线程充当着TM
- RM：Resource Manager，资源管理器。管理分支事务处理的资源，与TC交谈以注册分支事务和报告分支事务的状态，并驱动分支事务提交或回滚
  - RocketMQ中事务消息的Producer及Broker均是RM。
#### 注意
- 事务消息不支持延时消息
- 对于事务消息要做好幂等性检查，因为事务消息可能不止一次被消费（因为存在回滚后再提交的情况）
####  代码与效果
##### 定义事物消息生产者
```java
public class TransactionProducer {
    public static void main(String[] args) throws Exception {
        TransactionMQProducer producer = new TransactionMQProducer("tpg");
        producer.setNamesrvAddr("192.168.124.160:9876");
        /**
         * 定义一个线程池
         * @param corePoolSize 线程池中核心线程数量
         * @param maximumPoolSize 线程池中最多线程数
         * @param keepAliveTime 这是一个时间。当线程池中线程数量大于核心线程数量是，
         * 多余空闲线程的存活时长
         * @param unit 时间单位
         * @param workQueue 临时存放任务的队列，其参数就是队列的长度
         * @param threadFactory 线程工厂
         */
        ExecutorService executorService = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(2000), new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("client-transaction-msg-check-thread");
                        return thread;
                    }
                });
        // 为生产者指定一个线程池
        producer.setExecutorService(executorService);
        // 为生产者添加事务监听器
        producer.setTransactionListener(new ICBCTransactionListener());
        producer.start();
        String[] tags = {"TAGA","TAGB","TAGC"};
        for (int i = 0; i < 3; i++) {
            byte[] body = ("A要转账给B：" + (i*10000)).getBytes();
            Message msg = new Message("TopicTran", tags[i], body);
            // 发送事务消息
            // 第二个参数用于指定在执行本地事务时要使用的业务参数
            System.out.println("[TransactionMQProducer]: 开始发送消息------");
            SendResult sendResult =  producer.sendMessageInTransaction(msg,null);
            System.out.println("[TransactionMQProducer]发送结果为：" + sendResult.getSendStatus());
        }
    }
}
```
##### 定义工行事务监听器
```java
public class ICBCTransactionListener implements TransactionListener {
    // 回调操作方法
    // 消息预提交成功就会触发该方法的执行，用于完成本地事务
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        // 在这一步进行扣款
        System.out.println("[ICBCTransactionListener]:预提交消息成功：" + msg);
        System.out.println("[ICBCTransactionListener]:我要开始扣款啦....." );
        // 假设接收到TAGA的消息就表示扣款操作成功，TAGB的消息表示扣款失败，TAGC表示扣款结果不清楚，需要执行消息回查
        try {
            if (StringUtils.equals("TAGA", msg.getTags())) {
                System.out.println("[ICBCTransactionListener]:操作数据库扣款成功，扣款信息："+new String(msg.getBody()));
                return LocalTransactionState.COMMIT_MESSAGE;
            } else if (StringUtils.equals("TAGB", msg.getTags())) {
                System.out.println("[ICBCTransactionListener]:数据库扣款失败。。。。。。");
                int a = 1 / 0;// 模拟异常，扣款失败
                return LocalTransactionState.COMMIT_MESSAGE;
            } else if (StringUtils.equals("TAGC", msg.getTags())) {
                System.out.println("[ICBCTransactionListener]:我收到的消息不确定，到底要不要扣款呢" );
            }
            return LocalTransactionState.UNKNOW;
        } catch (Exception e) {
            System.out.println("[ICBCTransactionListener]:error："+e.getMessage());
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    // 消息回查方法
    // 引发消息回查的原因最常见的有两个：
    // 1)回调操作返回UNKNWON
    // 2)TC没有接收到TM的最终全局事务确认指令
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        // 这一步可以查数据库，看看是否正确扣款，如果正确扣款返回COMMIT_MESSAGE；否则返回ROLLBACK_MESSAGE
        System.out.println("[ICBCTransactionListener]:执行消息回查:" + msg.getTags());
        System.out.println("[ICBCTransactionListener]:我查了一下数据库，发现扣款时成功的");
        return LocalTransactionState.COMMIT_MESSAGE;
    }
}
```
##### 消费者
```java
public class TransactionConsumer {
    public static void main(String[] args) throws MQClientException {
        // 定义一个pull消费者
        // DefaultLitePullConsumer consumer = new DefaultLitePullConsumer("cg");
        // 定义一个push消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("cg");
        // 指定nameServer
        consumer.setNamesrvAddr("192.168.124.160:9876");
        // 指定从第一条消息开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET );
        // 指定消费topic与tag
        consumer.subscribe("TopicTran", "*");
        // 指定采用“广播模式”进行消费，默认为“集群模式”
        // consumer.setMessageModel(MessageModel.BROADCASTING);
        // 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            // 一旦broker中有了其订阅的消息就会触发该方法的执行，
            // 其返回值为当前consumer消费的状态
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                // 逐条消费消息
                for (MessageExt msg : msgs) {
                    System.out.println(msg);
                }
                // 返回消费状态：消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 开启消费者消费
        consumer.start();
        System.out.println("Consumer Started");
    }
}
```
##### 效果
![017](img/017.png)
三条消息都是SEND_OK，说明3条消息都发送到了Broker
![018](img/018.png)
消费者方面只消费了两条消息
![019](img/019.png)
在控制台上也只能看到2条消息。
因为TAGB在扣款的时候发生了异常，回调方法ICBCTransactionListener.executeLocalTransaction返回了ROLLBACK_MESSAGE。<br>
所以Broker回滚了TAGB这条消息。
### 批量消息
#### 批量发送消息
##### 发送限制
- 批量发送的消息必须具有相同的Topic
- 批量发送的消息必须具有相同的刷盘策略
- 批量发送的消息不能是延时消息与事务消息
##### 批量发送大小
一批发送的消息总大小不能超过4MB字节。如果想超出该值，有两种解决方案：
1. 将批量消息进行拆分，拆分为若干不大于4M的消息集合分多次批量发送
2. 在Producer端与Broker端修改属性
   - Producer端需要在发送之前设置Producer的maxMessageSize属性
   - Broker端需要修改其加载的配置文件中的maxMessageSize属性
##### message的构成
![020](img/020.png)
生产者通过send()方法发送的Message是通过Message生成了的一个字符串发送出去的。<br>
这个字符串由四部分构成：Topic、消息Body、消息日志（占20字节），及用于描述消息的一堆属性key-value。<br>
这些属性中包含例如生产者地址、生产时间、要发送的QueueId等
#### 批量消费消息
##### 修改批量属性
- MessageListenerConcurrently监听接口的consumeMessage方法，第一个参数List<MessageExt>默认每次只消费一条消息
- 修改Consumer的consumeMessageBatchMaxSize属性可以一次消费多条消息
- 改值不能超过32条
- 超过32条需要修改改Consumer的pullBatchSize属性
- pullBatchSize和consumeMessageBatchMaxSize不是越大越好
#### 代码与效果
##### 定义批量消息生产者
```java
public class BatchProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("pg");
        producer.setNamesrvAddr("192.168.124.160:9876");
        // 指定要发送的消息的最大大小，默认是4M
        // 不过，仅修改该属性是不行的，还需要同时修改broker加载的配置文件中的
        // maxMessageSize属性
        // producer.setMaxMessageSize(8 * 1024 * 1024);
        producer.start();

        // 定义要发送的消息集合
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 500000; i++) {
            byte[] body = ("message：," + i).getBytes();
            Message msg = new Message("batchTopic", "batchTag", body);
            messages.add(msg);
        }
        // 定义消息列表分割器，将消息列表分割为多个不超出4M大小的小列表
        MessageListSplitter2 splitter = new MessageListSplitter2(messages);
        while (splitter.hasNext()) {
            try {
                List<Message> listItem = splitter.next();
                System.out.println("count of message:"+listItem.size());
                producer.send(listItem);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        producer.shutdown();
    }
}
```
##### 定义消息列表分割器
```java
// 消息列表分割器：其只会处理每条消息的大小不超4M的情况。
// 若存在某条消息，其本身大小大于4M，这个分割器无法处理，
// 其直接将这条消息构成一个子列表返回。并没有再进行分割
/**
 * 这个完全算错了
 */
public class MessageListSplitter implements Iterator<List<Message>> {

    // 指定极限值为 4M = 4 * 1024 * 1024 Bytes(字节)
    // 最大4M，最后不要指定4M，万一没算准呢
    private final int SIZE_LIMIT = 4 * 1024 * 1024;
    // 存放所有要发送的消息
    private final List<Message> messages;
    // 要进行批量发送消息的小集合起始索引
    private int currIndex;

    public MessageListSplitter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public boolean hasNext() {
        // 判断当前开始遍历的消息索引要小于消息总数
        return currIndex < messages.size();
    }

    @Override
    public List<Message> next() {
        int nextIndex = currIndex; // ① 刚开始：currIndex=0 nextIndex=0
        // 记录当前要发送的这一小批次消息列表的大小
        int totalSize = 0;

        for (; nextIndex < messages.size(); nextIndex++) {
            // 获取当前遍历的消息
            Message message = messages.get(nextIndex);

            // 统计当前遍历的message的大小：Topic只允许字母和数字（allowing only ^[%|a-zA-Z0-9_-]+$）所以字符串topic的length就是topic的大小
            int tmpSize = message.getTopic().length() + message.getBody().length;
            Map<String, String> properties = message.getProperties();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                // properties的key和value也是限制为字母和数字
                tmpSize += entry.getKey().length() + entry.getValue().length();
            }
            // 加的20为消息日志
            tmpSize = tmpSize + 20;


            /**
             * tmpSize 是一条消息的 大小
             * 如果一条消息的大小已经超过了最大限制，大致可以分成如下两种情况：
             *      情况一：当第一条消息就已经大于4M，此时 nextIndex=0；currIndex=0
             *              nextIndex - currIndex == 0 为 true： nextIndex=1；currIndex=0
             *              于是messages.subList[0, 1)，把这个大于4M的截取
             *              截取之后：nextIndex=1；currIndex=1
             *              producer.send(listItem（一条大于4m的消息）);// 可能会报错
             *      情况二：第二条消息才大于4M，此时 nextIndex=1；currIndex=0
             *               nextIndex - currIndex == 0 为 false： nextIndex=1；currIndex=0
             *               于是messages.subList[0, 1)，没有截取那条大于4M的消息
             *               截取之后：nextIndex=1；currIndex=1
             *                producer.send(listItem（一条小于4m的消息）)
             *                在后面接着截取，就有是情况一了。
             */
            // 判断当前消息本身是否大于4M
            if (tmpSize > SIZE_LIMIT) {
                if (nextIndex - currIndex == 0) {
                    nextIndex++;
                }
                break;
            }

            if (tmpSize + totalSize > SIZE_LIMIT) {
                break;
            } else {
                totalSize += tmpSize;
            }
        } // end-for

        // 获取当前messages列表的子集合[currIndex, nextIndex)
        List<Message> subList = messages.subList(currIndex, nextIndex);
        // 下次遍历的开始索引
        currIndex = nextIndex;
        System.out.println("totalSize=" + totalSize);
        return subList;
    }
}
```
**以上完全错了，代码（源码）运行过程如下**
> ![021](img/021.png)
>
> ![022](img/022.png)
>
> ![023](img/023.png)
> 
> ![024](img/024.png)
> 
> ![025](img/025.png)
> 
> ![026](img/026.png)
> 
> ![027](img/027.png)
> 
> ![028](img/028.png)
> 
> ![029](img/029.png)
> 
> ![030](img/030.png)
> 
> ![031](img/031.png)
> 
> ![032](img/032.png)
```java
// 首先是遍历messages列表，然后通过encodeMessage算出每个消息的长度
List<byte[]> encodedMessages = new ArrayList<byte[]>(messages.size());
int allSize = 0;
for (Message message : messages) {
    byte[] tmp = encodeMessage(message);
    encodedMessages.add(tmp);
    allSize += tmp.length;
}
byte[] allBytes = new byte[allSize];
```
```java
    /*主要算长度的方法*/
    public static byte[] encodeMessage(Message message) {
        //only need flag, body, properties
        byte[] body = message.getBody();
        int bodyLen = body.length;
        String properties = messageProperties2String(message.getProperties());
        byte[] propertiesBytes = properties.getBytes(CHARSET_UTF8);
        //note properties length must not more than Short.MAX
        short propertiesLength = (short) propertiesBytes.length;
        int sysFlag = message.getFlag();
        int storeSize = 4 // 1 TOTALSIZE
            + 4 // 2 MAGICCOD
            + 4 // 3 BODYCRC
            + 4 // 4 FLAG
            + 4 + bodyLen // 4 BODY
            + 2 + propertiesLength;
        ByteBuffer byteBuffer = ByteBuffer.allocate(storeSize);
        // 1 TOTALSIZE
        byteBuffer.putInt(storeSize);

        // 2 MAGICCODE
        byteBuffer.putInt(0);

        // 3 BODYCRC
        byteBuffer.putInt(0);

        // 4 FLAG
        int flag = message.getFlag();
        byteBuffer.putInt(flag);

        // 5 BODY
        byteBuffer.putInt(bodyLen);
        byteBuffer.put(body);

        // 6 properties
        byteBuffer.putShort(propertiesLength);
        byteBuffer.put(propertiesBytes);

        return byteBuffer.array();
    }
    
    public static String messageProperties2String(Map<String, String> properties) {
        StringBuilder sb = new StringBuilder();
        if (properties != null) {
            for (final Map.Entry<String, String> entry : properties.entrySet()) {
                final String name = entry.getKey();
                final String value = entry.getValue();

                if (value == null) {
                    continue;
                }
                sb.append(name);
                sb.append(NAME_VALUE_SEPARATOR);
                sb.append(value);
                sb.append(PROPERTY_SEPARATOR);
            }
        }
        return sb.toString();
    }
```
于是综合起来可以得到 MessageListSplitter2
```java
// 消息列表分割器：其只会处理每条消息的大小不超4M的情况。
// 若存在某条消息，其本身大小大于4M，这个分割器无法处理，
// 其直接将这条消息构成一个子列表返回。并没有再进行分割
public class MessageListSplitter2 implements Iterator<List<Message>> {

    // 指定极限值为 4M = 4 * 1024 * 1024 Bytes(字节)
    // 最大4M，最后不要指定4M，万一没算准呢
    //private final int SIZE_LIMIT = 4 * 1024 * 1024;
    /*目前已经是非常接近了，但实际上还差一些：设置成1M吧，累了，让世界毁灭吧*/
    private final int SIZE_LIMIT = 1 * 1024 * 1024;
    // 存放所有要发送的消息
    private final List<Message> messages;
    private final MessageBatch messageBatch;
    // 要进行批量发送消息的小集合起始索引
    private int currIndex;

    public MessageListSplitter2(List<Message> messages) {
        MessageBatch messageBatch = MessageBatch.generateFromList(messages);
        this.messageBatch = messageBatch;
        this.messages = messages;
    }

    @Override
    public boolean hasNext() {
        // 判断当前开始遍历的消息索引要小于消息总数
        return currIndex < messages.size();
    }

    @Override
    public List<Message> next() {
        int nextIndex = currIndex; // ① 刚开始：currIndex=0 nextIndex=0
        // 记录当前要发送的这一小批次消息列表的大小
        int totalSize = 0;

        for (Message message : messageBatch) {
            //这个是主要的差异：producer再send消息的时候
            // 多增加了PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX这个属性
            // 导致两边的差异
            MessageClientIDSetter.setUniqID(message);
            //message.setTopic(withNamespace(message.getTopic()));

            byte[] bytes = encodeMessage(message);
            int tmpSize=bytes.length;

            // 判断当前消息本身是否大于4M
            if (tmpSize > SIZE_LIMIT) {
                if (nextIndex - currIndex == 0) {
                    nextIndex++;
                }
                break;
            }

            if (tmpSize + totalSize > SIZE_LIMIT) {
                break;
            } else {
                totalSize += tmpSize;
                nextIndex++;
            }
        }// end-for


        // 获取当前messages列表的子集合[currIndex, nextIndex)
        if(nextIndex>messages.size()){
            nextIndex=messages.size();
        }
        List<Message> subList = messages.subList(currIndex, nextIndex);
        // 下次遍历的开始索引
        currIndex = nextIndex;
        System.out.println("totalSize="+totalSize);
        return subList;
    }
    public static byte[] encodeMessage(Message message) {
        //only need flag, body, properties
        byte[] body = message.getBody();
        int bodyLen = body.length;
        String properties = messageProperties2String(message.getProperties());
        byte[] propertiesBytes = properties.getBytes(CHARSET_UTF8);
        //note properties length must not more than Short.MAX
        short propertiesLength = (short) propertiesBytes.length;
        int sysFlag = message.getFlag();
        int storeSize = 4 // 1 TOTALSIZE
                + 4 // 2 MAGICCOD
                + 4 // 3 BODYCRC
                + 4 // 4 FLAG
                + 4 + bodyLen // 4 BODY
                + 2 + propertiesLength;
        ByteBuffer byteBuffer = ByteBuffer.allocate(storeSize);
        // 1 TOTALSIZE
        byteBuffer.putInt(storeSize);

        // 2 MAGICCODE
        byteBuffer.putInt(0);

        // 3 BODYCRC
        byteBuffer.putInt(0);

        // 4 FLAG
        int flag = message.getFlag();
        byteBuffer.putInt(flag);

        // 5 BODY
        byteBuffer.putInt(bodyLen);
        byteBuffer.put(body);

        // 6 properties
        byteBuffer.putShort(propertiesLength);
        byteBuffer.put(propertiesBytes);

        return byteBuffer.array();
    }
    public static String messageProperties2String(Map<String, String> properties) {
        StringBuilder sb = new StringBuilder();
        if (properties != null) {
            for (final Map.Entry<String, String> entry : properties.entrySet()) {
                final String name = entry.getKey();
                final String value = entry.getValue();

                if (value == null) {
                    continue;
                }
                sb.append(name);
                sb.append(NAME_VALUE_SEPARATOR);
                sb.append(value);
                sb.append(PROPERTY_SEPARATOR);
            }
        }
        return sb.toString();
    }
}
```
##### 定义批量消息消费者
```java
public class BatchConsumer {
    public static void main(String[] args) throws MQClientException {
        DefaultMQPushConsumer consumer = new  DefaultMQPushConsumer("cg");
        consumer.setNamesrvAddr("192.168.124.160:9876");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe("batchTopic", "*");
        // 指定每次可以消费10条消息，默认为1
        consumer.setConsumeMessageBatchMaxSize(10);
        // 指定每次可以从Broker拉取40条消息，默认为32
        consumer.setPullBatchSize(40);
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    System.out.println(msg);
                }
                // 消费成功的返回结果
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                // 消费异常时的返回结果
                // return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        });
        consumer.start();
        System.out.println("Consumer Started");
    }
}
```
### 消息过滤
#### Tag过滤
通过consumer的subscribe()方法指定要订阅消息的Tag。如果订阅多个Tag的消息，Tag间使用或运算符(双竖线||)连接。
```java
DefaultMQPushConsumer consumer = new
DefaultMQPushConsumer("CID_EXAMPLE");
consumer.subscribe("TOPIC", "TAGA || TAGB || TAGC");
```
#### SQL过滤
SQL过滤是一种通过特定表达式对事先埋入到消息中的用户属性进行筛选过滤的方式。通过SQL过滤，可以实现对消息的复杂过滤。不过，只有使用PUSH模式的消费者才能使用SQL过滤。

SQL过滤表达式中支持多种常量类型与运算符。
> 支持的常量类型：<br>
> 数值：比如：123，3.1415<br>
> 字符：必须用单引号包裹起来，比如：'abc'<br>
> 布尔：TRUE 或 FALSE<br>
NULL：特殊的常量，表示空<br>
> 
> 支持的运算符有：<br>
数值比较：>，>=，<，<=，BETWEEN，= <br>
字符比较：=，<>，IN <br>
逻辑运算 ：AND，OR，NOT <br>
NULL判断：IS NULL 或者 IS NOT NULL <br>

默认情况下Broker没有开启消息的SQL过滤功能，需要在Broker加载的配置文件中添加如下属性，以开启该功能：
```text
enablePropertyFilter  = true
```
在启动Broker时需要指定这个修改过的配置文件。例如对于单机Broker的启动，其修改的配置文件是conf/broker.conf，启动时使用如下命令：
```text
sh bin/mqbroker -n localhost:9876 -c conf/broker.conf &
```
#### 代码举例
##### 定义Tag过滤Producer
```java
public class FilterByTagProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("pg");
        producer.setNamesrvAddr("192.168.124.160:9876");
        producer.start();
        String[] tags = {"myTagA","myTagB","myTagC"};
        for (int i = 0; i < 6; i++) {
            byte[] body = ("Hi," + i).getBytes();
            String tag = tags[i%tags.length];
            Message msg = new Message("myTopic",tag,body);
            SendResult sendResult = producer.send(msg);
            System.out.println(sendResult);
        }
        producer.shutdown();
    }
}
```
##### 定义Tag过滤Consumer
```java
public class FilterByTagConsumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new  DefaultMQPushConsumer("pg");
        consumer.setNamesrvAddr("192.168.124.160:9876");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe("myTopic", "myTagA || myTagB");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus  consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt me:msgs){
                    System.out.println(me);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.out.println("Consumer Started");
    }
}
```
##### 效果
> ![036](img/036.png) 
> 
> ![037](img/037.png) 

##### 定义SQL过滤Producer
```java
public class FilterBySQLProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("pg");
        producer.setNamesrvAddr("192.168.124.160:9876");
        producer.start();
        for (int i = 0; i < 6; i++) {
            try {
                byte[] body = ("Hi," + i).getBytes();
                Message msg = new Message("myTopic", "myTag", body);
                // 这里指定了age
                msg.putUserProperty("age", i + "");
                SendResult sendResult = producer.send(msg);
                System.out.println(sendResult);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        producer.shutdown();
    }
}
```
##### 定义SQL过滤Consumer
```java
public class FilterBySQLConsumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("pg");
        consumer.setNamesrvAddr("192.168.124.160:9876");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET );
        // 这里通过age过滤,只会消费 属性age为 [0,3]的消息
        consumer.subscribe("myTopic", MessageSelector.bySql("age between 0 and 3"));
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt me:msgs){
                    System.out.println(me);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.out.println("Consumer Started");
    }
}
```
##### 配置
对于单机的需要配置broker.conf。对于集群的需要配置xxx.properties。
```shell
# 在broker.conf里面加入下面配置
enablePropertyFilter  = true
```

另外需要重启servername和broker
```shell
# 关闭
sh bin/mqshutdown broker
sh bin/mqshutdown namesrv

# 启动
nohup sh bin/mqnamesrv &
# 这是之前的启动方式
nohup sh bin/mqbroker -n localhost:9876 &
# 但是因为我们后来改了配置，启动broker的时候指定配置
nohup sh bin/mqbroker -n localhost:9876 -c conf/broker.conf &
```
配置之后的效果<br>
![041](img/041.png)

###### 配置之后遇到的环境问题
<font color=Brown>【问题一】</font>broker无法启动
```shell
# 在这里启动了broker
[root@localhost rocketmq-all-4.8.0-bin-release]# nohup sh bin/mqbroker -n localhost:9876 -c conf/broker.conf &
[2] 3319
nohup: 忽略输入并把输出追加到"nohup.out"


# 使用jps查看，发现broker没有启动 
[root@localhost rocketmq-all-4.8.0-bin-release]# jps
2643 NamesrvStartup
3370 Jps
[2]+  退出 253              nohup sh bin/mqbroker -n localhost:9876 -c conf/broker.conf


# 接着查看broker的日志
[root@localhost rocketmq-all-4.8.0-bin-release]# tail -f ~/logs/rocketmqlogs/broker.log
2023-04-09 20:25:37 INFO main - load exist subscription group, SubscriptionGroupConfig [groupName=pg, consumeEnable=true, consumeFromMinEnable=true, consumeBroadcastEnable=true, retryQueueNums=1, retryMaxTimes=16, brokerId=0, whichBrokerWhenConsumeSlowly=1, notifyConsumerIdsChangedEnable=true]
2023-04-09 20:25:37 INFO main - load exist subscription group, SubscriptionGroupConfig [groupName=CID_ONSAPI_PULL, consumeEnable=true, consumeFromMinEnable=true, consumeBroadcastEnable=true, retryQueueNums=1, retryMaxTimes=16, brokerId=0, whichBrokerWhenConsumeSlowly=1, notifyConsumerIdsChangedEnable=true]
2023-04-09 20:25:37 INFO main - load /root/store/config/subscriptionGroup.json OK
2023-04-09 20:25:37 INFO main - load /root/store/config/consumerFilter.json OK
2023-04-09 20:25:37 INFO main - Try to start service thread:AllocateMappedFileService started:false lastThread:null
2023-04-09 20:25:37 INFO main - load /root/store/config/delayOffset.json OK
2023-04-09 20:25:37 INFO main - Try to shutdown service thread:AllocateMappedFileService started:true lastThread:Thread[AllocateMappedFileService,5,main]
2023-04-09 20:25:37 INFO main - shutdown thread AllocateMappedFileService interrupt true
2023-04-09 20:25:37 INFO main - join thread AllocateMappedFileService elapsed time(ms) 1 90000
2023-04-09 20:25:37 INFO main - Try to shutdown service thread:PullRequestHoldService started:false lastThread:null
```
[网上的帖子说](https://blog.csdn.net/u011249920/article/details/108217411)：删除store就可以了。于是我删除之后重启。确实好了。我猜测是不是因为我在批量消息的时候发送了太多消息，导致我的运行内存不够。

<font color=Brown>【问题二】</font>网络问题
> Windows上一致ping不通<br>
> ping 192.168.124.160<br>
> telnet 192.168.124.160 9876<br>
> 出现下面的窗口说明 telnet成功了
> ![040](img/040.png) <br>
> 当时没有成功

然后我查看防火墙，认为是防火墙挡住了，可防火墙是关着的
```text
[root@localhost consumequeue]# systemctl status firewalld
● firewalld.service - firewalld - dynamic firewall daemon
   Loaded: loaded (/usr/lib/systemd/system/firewalld.service; disabled; vendor preset: enabled)
   Active: inactive (dead)
     Docs: man:firewalld(1)
```
于是我想看看ip,发现没有ens33
```text
[root@localhost consumequeue]# ifconfig
lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536
        inet 127.0.0.1  netmask 255.0.0.0
        inet6 ::1  prefixlen 128  scopeid 0x10<host>
        loop  txqueuelen 1  (Local Loopback)
        RX packets 2775  bytes 1170799 (1.1 MiB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 2775  bytes 1170799 (1.1 MiB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

virbr0: flags=4099<UP,BROADCAST,MULTICAST>  mtu 1500
        inet 192.168.122.1  netmask 255.255.255.0  broadcast 192.168.122.255
        ether 52:54:00:fe:dd:b2  txqueuelen 1000  (Ethernet)
        RX packets 0  bytes 0 (0.0 B)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 0  bytes 0 (0.0 B)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0
```
于是我想重启一下网络
```text
[root@localhost consumequeue]# service network restart 
Restarting network (via systemctl):  Job for network.service failed because the control process exited with error code. See "systemctl status network.service" and "journalctl -xe" for details.
                                                           [失败]
[root@localhost consumequeue]# systemctl status network.service
● network.service - LSB: Bring up/down networking
   Loaded: loaded (/etc/rc.d/init.d/network; bad; vendor preset: disabled)
   Active: failed (Result: exit-code) since 日 2023-04-09 20:57:08 CST; 53s ago
     Docs: man:systemd-sysv-generator(8)
  Process: 4381 ExecStart=/etc/rc.d/init.d/network start (code=exited, status=1/FAILURE)
```
[网上的帖子说](https://blog.csdn.net/Vone_66/article/details/119931139)：
需要禁用掉NetworkManager
```shell
[root@localhost consumequeue]# systemctl stop NetworkManager


[root@localhost consumequeue]# systemctl disable NetworkManager
Removed symlink /etc/systemd/system/multi-user.target.wants/NetworkManager.service.
Removed symlink /etc/systemd/system/dbus-org.freedesktop.NetworkManager.service.
Removed symlink /etc/systemd/system/dbus-org.freedesktop.nm-dispatcher.service.


[root@localhost consumequeue]# systemctl start network.service


[root@localhost consumequeue]# ifconfig
ens33: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 192.168.124.160  netmask 255.255.255.0  broadcast 192.168.124.255
        inet6 fe80::20c:29ff:fe08:8526  prefixlen 64  scopeid 0x20<link>
        ether 00:0c:29:08:85:26  txqueuelen 1000  (Ethernet)
        RX packets 38  bytes 4924 (4.8 KiB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 52  bytes 10951 (10.6 KiB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536
        inet 127.0.0.1  netmask 255.0.0.0
        inet6 ::1  prefixlen 128  scopeid 0x10<host>
        loop  txqueuelen 1  (Local Loopback)
        RX packets 2814  bytes 1203546 (1.1 MiB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 2814  bytes 1203546 (1.1 MiB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

virbr0: flags=4099<UP,BROADCAST,MULTICAST>  mtu 1500
        inet 192.168.122.1  netmask 255.255.255.0  broadcast 192.168.122.255
        ether 52:54:00:fe:dd:b2  txqueuelen 1000  (Ethernet)
        RX packets 0  bytes 0 (0.0 B)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 0  bytes 0 (0.0 B)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0
```
<font color=Brown>【问题三】</font>其他技巧
```shell
# 查看端口
netstat -anp | grep 9876
# 查看所有端口
netstat -anp 
```
### 消息发送重试机制
#### 说明
- Producer对发送失败的消息进行重新发送的机制，称为**消息发送重试机制**，也称为**消息重投机制**。
- 生产者在发送消息时，若采用**同步**或**异步**发送方式，发送失败会重试，但**oneway**消息发送方式发送失败是没有重试机制的
- 只有**普通消息**具有发送重试机制，**顺序消息**是没有的（因为顺序消息是往同一个broker的同一个queue里面发，没必要重发）
- 消息重投机制可以保证消息尽可能发送成功、不丢失，但可能会**造成消息重复**。消息重复在RocketMQ中是无法避免的问题
- 消息重复在一般情况下不会发生，当出现消息量大、网络抖动，消息重复就会成为大概率事件
- producer主动重发、consumer负载变化（发生Rebalance，不会导致消息重复，但可能出现重复消费）也会导致重复消息
- 消息发送重试有三种策略可以选择：同步发送失败策略、异步发送失败策略、消息刷盘失败策略
#### 同步发送失败策略
```java
// 创建一个producer，参数为Producer Group名称
DefaultMQProducer producer = new DefaultMQProducer("pg");
// 指定nameServer地址
producer.setNamesrvAddr("rocketmqOS:9876");
// 设置同步发送失败时重试发送的次数，默认为2次
producer.setRetryTimesWhenSendFailed(3);
// 设置发送超时时限为5s，默认3s
producer.setSendMsgTimeout(5000);
```
- 如果Producer消息发送失败，默认重试2次
- Producer重试时是不会选择上次发送失败的Broker，而是选择其它Broker
- 若只有一个Broker其也只能发送到该Broker，但Producer会尽量发送到该Broker上的其它Queue
- Broker具有失败隔离功能，可以让Producer把消息尽量发送到没有发生过异常情况的Broker上
- 如果超过重试次数，则抛出异常，由Producer来存储失败的消息，保证消息不丢失
#### 异步发送失败策略
```java
DefaultMQProducer producer = new DefaultMQProducer("pg");
producer.setNamesrvAddr("rocketmqOS:9876");
// 指定异步发送失败后不进行重试发送
producer.setRetryTimesWhenSendAsyncFailed(0);
```
- Producer异步发送失败重试时，异步重试不会选择其他broker，仅在同一个broker上做重试
- 如果超过重试次数，则抛出异常，由Producer来存储失败的消息，保证消息不丢失
#### 消息刷盘失败策略
- 消息刷盘超时或slave不可用时，默认是不会将消息尝试发送到其他Broker的（会发生消息丢失）
  - Master或Slave都可能发生消息刷盘超时
  - slave在做数据同步时向master返回状态不是SEND_OK
- 对于重要消息可以通过在Broker的配置文件设置retryAnotherBrokerWhenNotStoreOK属性为true来开启
### 消息消费重试机制
#### 顺序消息的消费重试
```java
DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("cg");
// 顺序消息消费失败的消费重试时间间隔，单位毫秒，默认为1000，其取值范围为[10,30000]
consumer.setSuspendCurrentQueueTimeMillis(100);
```
- 对于顺序消息，当Consumer消费消息失败后，为了保证消息的顺序性，其会自动不断地进行消息重试，直到消费成功
- 重试期间应用会出现消息消费被阻塞的情况
- 消费重试默认间隔时间为1000毫秒
- 对于顺序消息的消费，务必要保证应用能够及时监控并处理消费失败的情况，避免消费被永久性阻塞
#### 无序消息的消费重试
- 对于无序消息集群消费下的重试消费，每条消息默认最多重试16次，但每次重试的间隔时间是不同的，会逐渐变长。
  ![042](img/042.png) 
- 修改消费重试次数
    ```java
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("cg");
        // 修改消费重试次数
       consumer.setMaxReconsumeTimes(10);
   ```
- 对于修改过的重试次数，将按照以下策略执行：
  - 若修改值小于16，则按照指定间隔进行重试
  - 若修改值大于16，则超过16次的重试时间间隔均为2小时
- 对于Consumer Group
  - 若仅修改了一个Consumer的消费重试次数，则会应用到该Group中所有其它Consumer实例
  - 若出现多个Consumer均做了修改的情况，则采用覆盖方式生效。即最后被修改的值会覆盖前面设置的值
#### 重试队列
对于需要重试消费的消息，并不是Consumer在等待了指定时长后再次去拉取原来的消息进行消费，
而是将这些需要重试消费的消息放入到了一个特殊Topic的队列中，而后进行再次消费的。
这个特殊的队列就是**重试队列**。

当出现需要进行重试消费的消息时，Broker会为每个消费组都设置一个名称为%RETRY%consumerGroup@consumerGroup 的重试队列。

这个重试队列是针对消息才组的，而不是针对每个Topic设置的
（一个Topic的消息可以让多个消费者组进行消费，所以会为这些消费者组各创建一个重试队列）

Broker对于重试消息的处理是通过延时消息实现的。
先将消息保存到SCHEDULE_TOPIC_XXXX延迟队列中，
延迟时间到后，会将消息投递到%RETRY%consumerGroup@consumerGroup重试队列中。

#### 消费重试配置方式
![043](img/043.png)
集群消费方式下，消息消费失败后若希望消费重试，则需要在消息监听器接口的实现中明确进行如下三种方式之一的配置：
- 方式1：返回ConsumeConcurrentlyStatus.RECONSUME_LATER（推荐）
- 方式2：返回Null
- 方式3：抛出异常
#### 消费不重试配置方式
![044](img/044.png)
集群消费方式下，消息消费失败后若不希望消费重试，则在捕获到异常后同样也返回与消费成功后的相同的结果，
即ConsumeConcurrentlyStatus.CONSUME_SUCCESS，则不进行消费重试。
### 死信队列
#### 什么是死信队列
当一条消息初次消费失败，消息队列会自动进行消费重试；达到最大重试次数后，若消费依然失败，
则表明消费者在正常情况下无法正确地消费该消息，
此时，消息队列不会立刻将消息丢弃，而是将其发送到该消费者对应的特殊队列中。
这个队列就是**死信队列（Dead-Letter Queue，DLQ）**，而其中的消息则称为**死信消息（Dead-Letter Message，DLM）**。
#### 死信队列的特征
- 死信队列中的消息不会再被消费者正常消费，即DLQ对于消费者是不可见的
- 死信存储有效期与正常消息相同，均为 **3 天**（commitlog文件的过期时间），3 天后会被自动删除
- 死信队列就是一个特殊的**Topic**，名称为**%DLQ%consumerGroup@consumerGroup** ，即每个消费者组都有一个死信队列
- 如果⼀个消费者组未产生死信消息，则不会为其创建相应的死信队列
#### 死信消息的处理
实际上，当⼀条消息进入死信队列，就意味着系统中某些地方出现了问题，从而导致消费者无法正常消费该消息，比如代码中原本就存在Bug。
因此，对于死信消息，通常需要开发人员进行特殊处理。
最关键的步骤是要**排查可疑因素，解决代码中可能存在的Bug，然后再将原来的死信消息再次进行投递消费**。
