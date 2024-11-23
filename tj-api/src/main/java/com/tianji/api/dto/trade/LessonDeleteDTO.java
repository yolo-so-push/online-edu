package com.tianji.api.dto.trade;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LessonDeleteDTO {

    /**
     * 订单id
     */
    private Long orderId;
    /**
     * 订单用户id
     */
    private Long userId;
    /**
     * 退款订单课程id集合
     */
    private List<Long> courseIds;
}
