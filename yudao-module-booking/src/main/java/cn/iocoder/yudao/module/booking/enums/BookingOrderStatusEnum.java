package cn.iocoder.yudao.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BookingOrderStatusEnum {

    PENDING_PAYMENT(5, "待支付"),
    PENDING_CONFIRM(10, "待确认"),
    PAID_WAIT_CONFIRM(12, "待商户确认"),
    CONFIRMED(20, "已确认"),
    CHECKED_IN(25, "已入住"),
    CANCELED(30, "已取消"),
    CLOSED_TIMEOUT(31, "支付超时关闭"),
    CLOSED_CANCELLED(32, "用户取消关闭"),
    NO_SHOW(35, "未到店"),
    COMPLETED(40, "已完成"),
    REFUND_APPLYING(50, "退款申请中"),
    REFUNDED(60, "已退款"),
    REJECTED_REFUNDED(70, "商户拒绝已退款");

    private final Integer status;
    private final String name;
}
