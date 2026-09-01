package cn.iocoder.yudao.module.booking.dal.dataobject.inventory;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

@TableName("booking_inventory")
@KeySequence("booking_inventory_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingInventoryDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long merchantId;
    private Long roomTypeId;
    private LocalDate bizDate;
    private Integer totalQuantity;
    private Integer lockedQuantity;
    private Integer soldQuantity;
    /**
     * 每晚价格，单位：分
     */
    private Integer price;
}
