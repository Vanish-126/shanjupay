package com.shanjupay.paymentagent.message;

import com.alibaba.fastjson.JSON;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 生产消息类
 * @author Administrator
 * @version 1.0
 **/
@Component
@Slf4j
public class PayProducer {
    /**
     * 订单结果查询主题
     */
    private static final String TOPIC_ORDER = "TP_PAYMENT_ORDER";

    /**
     * 订单结果 主题
     */
    private static final String TOPIC_RESULT = "TP_PAYMENT_RESULT";

    @Autowired
    RocketMQTemplate rocketMQTemplate;

    /**
     * 发送消息（查询支付宝订单状态）：延迟消息
     * @param paymentResponseDTO
     */
    public void payOrderNotice(PaymentResponseDTO paymentResponseDTO){
        //发送延迟消息
        Message<PaymentResponseDTO> message = MessageBuilder.withPayload(paymentResponseDTO).build();
        //延迟第3级发送（延迟10秒）
        rocketMQTemplate.syncSend(TOPIC_ORDER, message,3000,3);
        log.info("支付渠道代理服务向mq发送订单查询的消息：{}", JSON.toJSONString(paymentResponseDTO));
    }

    /**
     * 发送消息（支付结果）
     * @param paymentResponseDTO
     */
    public void payResultNotice(PaymentResponseDTO paymentResponseDTO){
        // 将DTO转为JSON
        rocketMQTemplate.convertAndSend(TOPIC_RESULT, paymentResponseDTO);
        log.info("支付渠道代理服务向mq支付结果消息：{}", JSON.toJSONString(paymentResponseDTO));
    }

}
