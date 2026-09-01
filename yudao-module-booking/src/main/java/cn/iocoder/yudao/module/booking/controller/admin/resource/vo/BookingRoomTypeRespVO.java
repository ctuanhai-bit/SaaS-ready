package cn.iocoder.yudao.module.booking.controller.admin.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 房型 Response VO")
@Data
public class BookingRoomTypeRespVO {
    private Long id;
    private Long tenantId;
    private Long merchantId;
    private String name;
    private Integer maxOccupancy;
    private BigDecimal areaSqm;
    private BigDecimal areaSqmMin;
    private BigDecimal areaSqmMax;
    private Boolean breakfastIncluded;
    private Integer initialPrice;
    private Integer status;
    private String coverUrl;
    private String imageUrls;
    private String facilityCodes;
}
