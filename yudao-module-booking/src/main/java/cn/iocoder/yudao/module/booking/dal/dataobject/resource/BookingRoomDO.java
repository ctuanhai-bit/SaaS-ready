package cn.iocoder.yudao.module.booking.dal.dataobject.resource;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("booking_room")
@KeySequence("booking_room_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRoomDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long merchantId;
    private Long roomTypeId;
    private String roomNo;
    private Integer status;
}
