package cn.iocoder.yudao.module.booking.dal.dataobject.order;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@TableName("booking_order_lock")
@KeySequence("booking_order_lock_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingOrderLockDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long orderId;
    private Long inventoryId;
    private Long merchantId;
    private Long roomTypeId;
    private LocalDate bizDate;
    private Integer quantity;
    private Integer price;
    private Integer lockStatus;
}
