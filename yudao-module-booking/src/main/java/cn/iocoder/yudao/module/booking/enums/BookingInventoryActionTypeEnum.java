package cn.iocoder.yudao.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BookingInventoryActionTypeEnum {

    LOCK("LOCK", "锁定库存"),
    PAY_CONFIRMED("PAY_CONFIRMED", "支付确认"),
    AUTO_RELEASE("AUTO_RELEASE", "超时释放"),
    USER_CANCEL_RELEASE("USER_CANCEL_RELEASE", "用户取消释放"),
    MERCHANT_REJECT_RELEASE("MERCHANT_REJECT_RELEASE", "商户拒绝释放"),
    REFUND_RELEASE("REFUND_RELEASE", "退款释放"),
    MANUAL_ADJUST("MANUAL_ADJUST", "手工调整"),
    MANUAL_LOCK("MANUAL_LOCK", "手工锁房"),
    MANUAL_UNLOCK("MANUAL_UNLOCK", "手工解锁");

    private final String type;
    private final String name;
}
