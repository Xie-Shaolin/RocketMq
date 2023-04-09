package com.lin.Batch;


import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.apache.rocketmq.common.message.MessageClientIDSetter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.rocketmq.common.message.MessageDecoder.*;


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

