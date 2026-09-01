package cn.iocoder.yudao.module.booking.service.stock;

import java.time.LocalDate;

/**
 * 订房库存校验 contract。
 */
public interface BookingStockService {

    Long occupy(Long tenantId, Long merchantId, Long resourceId, LocalDate bizDate, Integer quantity);

    void release(Long tenantId, Long merchantId, Long inventoryId, Integer quantity);

    void manualLock(Long tenantId, Long merchantId, Long roomTypeId, LocalDate bizDate, Integer quantity, Long operatorId);

    void manualUnlock(Long tenantId, Long merchantId, Long roomTypeId, LocalDate bizDate, Integer quantity, Long operatorId);
}
