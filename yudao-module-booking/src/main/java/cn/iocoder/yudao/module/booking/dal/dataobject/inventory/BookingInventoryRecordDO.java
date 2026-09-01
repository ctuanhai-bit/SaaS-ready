package cn.iocoder.yudao.module.booking.dal.dataobject.inventory;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

@TableName("booking_inventory_record")
@KeySequence("booking_inventory_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingInventoryRecordDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long merchantId;
    private Long roomTypeId;
    private Long inventoryId;
    private LocalDate bizDate;
    private Integer quantity;
    private String actionType;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String operatorType;
    private Long operatorId;
}
