package cn.iocoder.yudao.module.booking.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 订房订单创建 Request VO")
@Data
@Accessors(chain = true)
public class BookingOrderCreateReqVO {
    @Schema(hidden = true)
    private Long tenantId;
    @Schema(hidden = true)
    private Long merchantId;
    @NotNull(message = "房型不能为空")
    private Long roomTypeId;
    @NotNull(message = "入住日期不能为空")
    private LocalDate checkInDate;
    @NotNull(message = "离店日期不能为空")
    private LocalDate checkOutDate;
    @NotNull(message = "房间数量不能为空")
    private Integer roomQuantity;
    @NotBlank(message = "提交令牌不能为空")
    private String clientSubmitToken;
    private String guestName;
    private String guestMobile;
}
