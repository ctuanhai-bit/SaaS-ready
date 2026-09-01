package cn.iocoder.yudao.module.booking.service.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryRecordDO;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryRecordMapper;
import cn.iocoder.yudao.module.booking.enums.BookingInventoryActionTypeEnum;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;

import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.BOOKING_INVENTORY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class BookingStockServiceImplTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 10L;
    private static final Long MERCHANT_ID = 100L;
    private static final Long ROOM_TYPE_ID = 200L;
    private static final Long INVENTORY_ID = 300L;
    private static final LocalDate BIZ_DATE = LocalDate.of(2026, 6, 4);

    @InjectMocks
    private BookingStockServiceImpl stockService;

    @Mock
    private BookingInventoryMapper inventoryMapper;
    @Mock
    private BookingInventoryRecordMapper inventoryRecordMapper;
    @Mock
    private MerchantContextService merchantContextService;

    @Test
    public void testOccupy_lockInventoryByTenantAndMerchant() {
        when(inventoryMapper.selectByRoomTypeIdAndDateForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID, BIZ_DATE))
                .thenReturn(inventory(TENANT_ID, MERCHANT_ID, 5, 1, 0));

        Long inventoryId = stockService.occupy(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID, BIZ_DATE, 2);

        assertEquals(INVENTORY_ID, inventoryId);
        verify(inventoryMapper).selectByRoomTypeIdAndDateForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID, BIZ_DATE);
        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(3, inventoryCaptor.getValue().getLockedQuantity());
    }

    @Test
    public void testRelease_crossMerchantDenied() {
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(TENANT_ID, 999L, 5, 2, 0));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> stockService.release(TENANT_ID, MERCHANT_ID, INVENTORY_ID, 1));

        assertEquals(BOOKING_INVENTORY_NOT_EXISTS.getCode(), exception.getCode());
        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));
    }

    @Test
    public void testRelease_crossTenantDenied() {
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(999L, MERCHANT_ID, 5, 2, 0));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> stockService.release(TENANT_ID, MERCHANT_ID, INVENTORY_ID, 1));

        assertEquals(BOOKING_INVENTORY_NOT_EXISTS.getCode(), exception.getCode());
        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));
    }

    @Test
    public void testRelease_sameTenantMerchant() {
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(TENANT_ID, MERCHANT_ID, 5, 2, 0));

        stockService.release(TENANT_ID, MERCHANT_ID, INVENTORY_ID, 1);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(INVENTORY_ID, inventoryCaptor.getValue().getId());
        assertEquals(1, inventoryCaptor.getValue().getLockedQuantity());
    }

    @Test
    public void testManualLock_shouldIncreaseLockedAndRecord() {
        when(inventoryMapper.selectByRoomTypeIdAndDateForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID, BIZ_DATE))
                .thenReturn(inventory(TENANT_ID, MERCHANT_ID, 5, 1, 1));

        stockService.manualLock(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID, BIZ_DATE, 2, 900L);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(3, inventoryCaptor.getValue().getLockedQuantity());

        ArgumentCaptor<BookingInventoryRecordDO> recordCaptor = ArgumentCaptor.forClass(BookingInventoryRecordDO.class);
        verify(inventoryRecordMapper).insert(recordCaptor.capture());
        assertEquals(BookingInventoryActionTypeEnum.MANUAL_LOCK.getType(), recordCaptor.getValue().getActionType());
        assertEquals(2, recordCaptor.getValue().getQuantity());
        assertEquals(MERCHANT_ID, recordCaptor.getValue().getMerchantId());
        assertEquals(TENANT_ID, recordCaptor.getValue().getTenantId());
    }

    @Test
    public void testManualUnlock_shouldDecreaseLockedAndRecord() {
        when(inventoryMapper.selectByRoomTypeIdAndDateForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID, BIZ_DATE))
                .thenReturn(inventory(TENANT_ID, MERCHANT_ID, 5, 3, 1));

        stockService.manualUnlock(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID, BIZ_DATE, 2, 900L);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(1, inventoryCaptor.getValue().getLockedQuantity());

        ArgumentCaptor<BookingInventoryRecordDO> recordCaptor = ArgumentCaptor.forClass(BookingInventoryRecordDO.class);
        verify(inventoryRecordMapper).insert(recordCaptor.capture());
        assertEquals(BookingInventoryActionTypeEnum.MANUAL_UNLOCK.getType(), recordCaptor.getValue().getActionType());
        assertEquals(2, recordCaptor.getValue().getQuantity());
        assertEquals(MERCHANT_ID, recordCaptor.getValue().getMerchantId());
        assertEquals(TENANT_ID, recordCaptor.getValue().getTenantId());
    }

    private BookingInventoryDO inventory(Long tenantId, Long merchantId, Integer total, Integer locked, Integer sold) {
        BookingInventoryDO inventory = new BookingInventoryDO().setId(INVENTORY_ID).setMerchantId(merchantId)
                .setRoomTypeId(ROOM_TYPE_ID).setBizDate(BIZ_DATE)
                .setTotalQuantity(total).setLockedQuantity(locked).setSoldQuantity(sold);
        inventory.setTenantId(tenantId);
        return inventory;
    }
}
