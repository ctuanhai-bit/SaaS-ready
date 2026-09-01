package cn.iocoder.yudao.module.booking.controller.admin.order.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Schema(description = "管理后台 - 订房订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class BookingOrderPageReqVO extends PageParam {
    @Schema(description = "关键词，匹配订单号、入住人姓名或手机号")
    private String keyword;
    private Integer status;
    private Long roomTypeId;
    private LocalDate[] checkInDate;
    private LocalDate[] checkOutDate;
    @Schema(description = "前台业务日期")
    private LocalDate serviceDate;
    @Schema(description = "是否查询业务日期内的在住订单", hidden = true)
    private Boolean activeOnServiceDate;
}
