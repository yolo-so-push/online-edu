package com.tianji.learning.mq;

import com.tianji.api.dto.trade.LessonDeleteDTO;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 课程变更监听器
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class LessonChangeListener {

    private final ILearningLessonService learningLessonService;

    @RabbitListener(bindings =@QueueBinding(
            value =@Queue(value = "learning.lesson.pay.queue",durable = "true"),
            exchange =@Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE,type = ExchangeTypes.TOPIC),
            key =MqConstants.Key.ORDER_PAY_KEY))
    public void listenLessonPay(OrderBasicDTO orderBasicDTO){
        if (orderBasicDTO==null||orderBasicDTO.getUserId()==null|| CollUtils.isEmpty(orderBasicDTO.getCourseIds())){
            log.error("接收到的MQ消息有误，订单数据为空");
            return;
        }
        //添加课程
        log.debug("监听到用户{}的订单，需要添加课程{}到课程表中",orderBasicDTO.getUserId(),orderBasicDTO.getOrderId(),orderBasicDTO.getCourseIds());
        learningLessonService.addUserLessons(orderBasicDTO.getUserId(),orderBasicDTO.getCourseIds());
    }

    @RabbitListener(bindings = @QueueBinding(value =@Queue(value = ""),exchange =@Exchange(name =MqConstants.Exchange.ORDER_EXCHANGE),key =MqConstants.Key.ORDER_REFUND_KEY))
    public void listenLessonRefund(OrderBasicDTO orderBasicDTO){
        if (orderBasicDTO==null||orderBasicDTO.getUserId()==null||CollUtils.isEmpty(orderBasicDTO.getCourseIds())){
            log.error("接收到的MQ消息有误，订单数据为空");
            return;
        }
        //删除课程
        log.debug("从用户{}课程表中删除课程{}",orderBasicDTO.getUserId(),orderBasicDTO.getCourseIds());
        learningLessonService.deleteLesson(orderBasicDTO.getUserId(),orderBasicDTO.getCourseIds());
    }
}
