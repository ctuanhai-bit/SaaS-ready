package cn.iocoder.yudao.module.booking.controller.admin.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 房型状态更新 Request VO")
@Data
public class BookingRoomTypeStatusReqVO {

    @Schema(description = "房型编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房型编号不能为空")
    private Long id;

    @Schema(description = "状态，0 启用，1 停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

}
