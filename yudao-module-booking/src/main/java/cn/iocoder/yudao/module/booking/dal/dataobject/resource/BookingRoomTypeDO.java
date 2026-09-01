package cn.iocoder.yudao.module.booking.dal.dataobject.resource;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("booking_room_type")
@KeySequence("booking_room_type_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRoomTypeDO extends TenantBaseDO {
    @TableId
    private Long id;
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
    private Boolean autoConfirmEnabled;
}
