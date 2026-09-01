package cn.iocoder.yudao.module.booking.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode BOOKING_INVENTORY_NOT_EXISTS = new ErrorCode(1_030_000_000, "订房库存不存在");
    ErrorCode BOOKING_INVENTORY_NOT_ENOUGH = new ErrorCode(1_030_000_001, "订房库存不足");
    ErrorCode BOOKING_ORDER_NOT_EXISTS = new ErrorCode(1_030_000_002, "订房订单不存在");
    ErrorCode BOOKING_ORDER_STATUS_INVALID = new ErrorCode(1_030_000_003, "订房订单状态不正确");
    ErrorCode BOOKING_DATE_INVALID = new ErrorCode(1_030_000_004, "订房日期不正确");
    ErrorCode BOOKING_ORDER_DUPLICATE_SUBMIT = new ErrorCode(1_030_000_005, "订房订单重复提交");
    ErrorCode BOOKING_ROOM_TYPE_NOT_EXISTS = new ErrorCode(1_030_000_006, "房型不存在");
    ErrorCode BOOKING_INVENTORY_BATCH_TOO_LARGE = new ErrorCode(1_030_000_007, "单次批量调整最多支持 1000 条房态库存");
}
