package cn.iocoder.yudao.module.booking.controller.admin.resource.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 房型更新 Request VO")
@Data
public class BookingRoomTypeUpdateReqVO {

    @Schema(description = "房型编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房型编号不能为空")
    private Long id;

    @Schema(description = "房型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "房型名称不能为空")
    private String name;

    @Schema(description = "最大入住人数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "最大入住人数不能为空")
    private Integer maxOccupancy;

    @Schema(description = "旧版房型面积，单位平方米", example = "32.5")
    @DecimalMin(value = "0.01", message = "房型面积必须大于 0")
    private BigDecimal areaSqm;

    @Schema(description = "房型最小面积，单位平方米", example = "28")
    @DecimalMin(value = "0.01", message = "房型最小面积必须大于 0")
    private BigDecimal areaSqmMin;

    @Schema(description = "房型最大面积，单位平方米", example = "32")
    @DecimalMin(value = "0.01", message = "房型最大面积必须大于 0")
    private BigDecimal areaSqmMax;

    @Schema(description = "是否含早餐", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择是否含早餐")
    private Boolean breakfastIncluded;

    @Schema(description = "初始价格，单位分", requiredMode = Schema.RequiredMode.REQUIRED, example = "19900")
    @NotNull(message = "初始价格不能为空")
    @Min(value = 1, message = "初始价格必须大于 0")
    private Integer initialPrice;

    @Schema(description = "状态，0 启用，1 停用")
    private Integer status;

    @Schema(description = "房型封面图 URL 或媒体路径")
    private String coverUrl;

    @Schema(description = "房型多图 URL/路径 JSON 数组")
    private String imageUrls;

    @Schema(description = "房型设施编码 JSON 数组")
    private String facilityCodes;

    @JsonIgnore
    @AssertTrue(message = "请输入完整的房型面积区间，且最大面积不能小于最小面积")
    public boolean isAreaRangeValid() {
        BigDecimal min = areaSqmMin != null ? areaSqmMin : areaSqm;
        BigDecimal max = areaSqmMax != null ? areaSqmMax : areaSqm;
        return min != null && max != null && min.signum() > 0 && max.compareTo(min) >= 0;
    }

}
