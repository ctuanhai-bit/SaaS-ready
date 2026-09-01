package cn.iocoder.yudao.module.booking.controller.admin.resource.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 房型分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class BookingRoomTypePageReqVO extends PageParam {
    private String name;
    private Long merchantId;
    private Integer status;
}
