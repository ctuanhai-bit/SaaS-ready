package cn.iocoder.yudao.module.booking.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 订房订单 Response VO")
@Data
public class BookingOrderRespVO {
    private Long id;
    private Long tenantId;
    private Long merchantId;
    private Long roomTypeId;
    private String roomType;
    private String orderNo;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer roomQuantity;
    private String roomNo;
    private Integer status;
    private Integer payStatus;
    private Integer refundStatus;
    private Integer amount;
    private String guestName;
    private String guestMobile;
    private String verificationCode;
}
