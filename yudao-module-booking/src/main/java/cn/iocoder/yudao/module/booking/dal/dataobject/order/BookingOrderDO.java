package cn.iocoder.yudao.module.booking.dal.dataobject.order;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("booking_order")
@KeySequence("booking_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingOrderDO extends TenantBaseDO {
    @TableId
    private Long id;
    private String orderNo;
    private Long merchantId;
    private Long roomTypeId;
    private Long inventoryId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer roomQuantity;
    private String roomNo;
    private String guestName;
    private String guestMobile;
    private String clientSubmitToken;
    private Integer status;
    /** 支付状态预留字段：0 未支付，不接真实支付。 */
    private Integer payStatus;
    private Long payOrderId;
    private LocalDateTime expireTime;
    private Boolean autoConfirmSnapshot;
    private Integer refundStatus;
}
