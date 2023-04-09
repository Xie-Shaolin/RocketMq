package com.lin.order;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.List;

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

