package cn.iocoder.yudao.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BookingOrderLockStatusEnum {

    LOCKED(10, "已锁定"),
    RELEASED(20, "已释放"),
    SOLD(30, "已售出");

    private final Integer status;
    private final String name;
}
