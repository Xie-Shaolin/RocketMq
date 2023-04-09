package com.lin.Batch;


import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageClientIDSetter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


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
            MessageClientIDSetter.setUniqID(message);
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
        System.out.println("totalSize="+totalSize);
        return subList;
    }
}

