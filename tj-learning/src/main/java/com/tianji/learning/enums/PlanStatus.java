package com.tianji.learning.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.tianji.common.enums.BaseEnum;
import lombok.Getter;

@Getter
public enum PlanStatus implements BaseEnum {
    NO_PLAIN(0,"没有计划"),
    PLANING(1,"计划中")
    ;
    int value;
    String desc;

    PlanStatus(int value, String desc) {
        this.value=value;
        this.desc=desc;
    }
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PlanStatus of(Integer value){
        if (value==null){
            return null;
        }
        for (PlanStatus status : values()) {
            if (status.equalsValue(value)){
                return status;
            }
        }
        return null;
    }
}
