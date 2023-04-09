package com.lin.Transaction;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

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
