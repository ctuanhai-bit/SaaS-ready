package cn.iocoder.yudao.module.booking.service.order;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderCreateReqVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryRecordDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderLockDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.resource.BookingRoomTypeDO;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryRecordMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.order.BookingOrderMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.resource.BookingRoomTypeMapper;
import cn.iocoder.yudao.module.booking.enums.BookingInventoryActionTypeEnum;
import cn.iocoder.yudao.module.booking.enums.BookingOrderLockStatusEnum;
import cn.iocoder.yudao.module.booking.enums.BookingOrderStatusEnum;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BookingOrderServiceImplTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 10L;
    private static final Long MERCHANT_ID = 100L;
    private static final Long ROOM_TYPE_ID = 200L;
    private static final Long INVENTORY_ID = 300L;
    private static final Long ORDER_ID = 400L;

    @InjectMocks
    private BookingOrderServiceImpl orderService;

    @Mock
    private BookingOrderMapper orderMapper;
    @Mock
    private cn.iocoder.yudao.module.booking.dal.mysql.order.BookingOrderLockMapper orderLockMapper;
    @Mock
    private BookingInventoryMapper inventoryMapper;
    @Mock
    private BookingInventoryRecordMapper inventoryRecordMapper;
    @Mock
    private BookingRoomTypeMapper roomTypeMapper;
    @Mock
    private MerchantContextService merchantContextService;

    @Test
    public void testCreateOrder_lockInventoryForContinuousNights() {
        when(orderMapper.selectByClientSubmitToken(TENANT_ID, MERCHANT_ID, "submit-1")).thenReturn(null);
        when(inventoryMapper.selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 6)))
                .thenReturn(java.util.Arrays.asList(
                        inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID, LocalDate.of(2026, 6, 4), 5, 1, 0),
                        inventory(INVENTORY_ID + 1, TENANT_ID, MERCHANT_ID, LocalDate.of(2026, 6, 5), 4, 0, 1)));
        doAnswer(invocation -> {
            BookingOrderDO order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return null;
        }).when(orderMapper).insert(any(BookingOrderDO.class));

        Long id = orderService.createOrder(new BookingOrderCreateReqVO().setTenantId(TENANT_ID).setMerchantId(MERCHANT_ID)
                .setRoomTypeId(ROOM_TYPE_ID).setCheckInDate(LocalDate.of(2026, 6, 4)).setCheckOutDate(LocalDate.of(2026, 6, 6))
                .setRoomQuantity(2).setClientSubmitToken("submit-1").setGuestName("guest").setGuestMobile("13800000000"));

        assertEquals(ORDER_ID, id);
        verify(inventoryMapper).selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 6));

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper, times(2)).updateById(inventoryCaptor.capture());
        assertEquals(3, inventoryCaptor.getAllValues().get(0).getLockedQuantity());
        assertEquals(2, inventoryCaptor.getAllValues().get(1).getLockedQuantity());

        ArgumentCaptor<BookingOrderDO> orderCaptor = ArgumentCaptor.forClass(BookingOrderDO.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertEquals("submit-1", orderCaptor.getValue().getClientSubmitToken());
        assertEquals(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus(), orderCaptor.getValue().getStatus());
        assertEquals(0, orderCaptor.getValue().getPayStatus());
        assertEquals(MERCHANT_ID, orderCaptor.getValue().getMerchantId());
        assertNotNull(orderCaptor.getValue().getOrderNo());
        assertNotNull(orderCaptor.getValue().getExpireTime());
        assertTrue(orderCaptor.getValue().getExpireTime().isAfter(LocalDateTime.now().plusMinutes(14)));
        assertTrue(orderCaptor.getValue().getExpireTime().isBefore(LocalDateTime.now().plusMinutes(16)));

        ArgumentCaptor<java.util.Collection<BookingOrderLockDO>> lockCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(orderLockMapper).insertBatch(lockCaptor.capture());
        java.util.List<BookingOrderLockDO> locks = new java.util.ArrayList<>(lockCaptor.getValue());
        assertEquals(19900, locks.get(0).getPrice());
        assertEquals(20900, locks.get(1).getPrice());

        ArgumentCaptor<java.util.Collection<BookingInventoryRecordDO>> recordCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(inventoryRecordMapper).insertBatch(recordCaptor.capture());
        java.util.List<BookingInventoryRecordDO> records = new java.util.ArrayList<>(recordCaptor.getValue());
        assertEquals(2, records.size());
        assertEquals(BookingInventoryActionTypeEnum.LOCK.getType(), records.get(0).getActionType());
        assertEquals("BOOKING_ORDER", records.get(0).getBizType());
        assertEquals(ORDER_ID, records.get(0).getBizId());
        assertEquals(2, records.get(0).getQuantity());
    }

    @Test
    public void testCreateOrder_shouldSnapshotRoomTypeAutoConfirm() {
        when(orderMapper.selectByClientSubmitToken(TENANT_ID, MERCHANT_ID, "submit-auto")).thenReturn(null);
        when(roomTypeMapper.selectById(ROOM_TYPE_ID)).thenReturn(roomType(true));
        when(inventoryMapper.selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 5)))
                .thenReturn(Collections.singletonList(inventory(TENANT_ID, MERCHANT_ID, 5, 0, 0)));
        doAnswer(invocation -> {
            BookingOrderDO order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return null;
        }).when(orderMapper).insert(any(BookingOrderDO.class));

        orderService.createOrder(new BookingOrderCreateReqVO().setTenantId(TENANT_ID).setMerchantId(MERCHANT_ID)
                .setRoomTypeId(ROOM_TYPE_ID).setCheckInDate(LocalDate.of(2026, 6, 4)).setCheckOutDate(LocalDate.of(2026, 6, 5))
                .setRoomQuantity(1).setClientSubmitToken("submit-auto").setGuestName("guest").setGuestMobile("13800000000"));

        ArgumentCaptor<BookingOrderDO> orderCaptor = ArgumentCaptor.forClass(BookingOrderDO.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertEquals(Boolean.TRUE, orderCaptor.getValue().getAutoConfirmSnapshot());
    }

    @Test
    public void testCreateOrder_duplicateSubmitTokenReturnsExistingOrderAndDoesNotRelock() {
        when(orderMapper.selectByClientSubmitToken(TENANT_ID, MERCHANT_ID, "submit-1")).thenReturn(order(BookingOrderStatusEnum.PENDING_CONFIRM.getStatus()));

        Long id = orderService.createOrder(new BookingOrderCreateReqVO().setTenantId(TENANT_ID).setMerchantId(MERCHANT_ID)
                .setRoomTypeId(ROOM_TYPE_ID).setCheckInDate(LocalDate.of(2026, 6, 4)).setCheckOutDate(LocalDate.of(2026, 6, 6))
                .setRoomQuantity(2).setClientSubmitToken("submit-1"));

        assertEquals(ORDER_ID, id);
        verify(inventoryMapper, never()).selectListByRoomTypeIdAndDateRangeForUpdate(any(), any(), any(), any(), any());
        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));
        verify(orderMapper, never()).insert(any(BookingOrderDO.class));
    }

    @Test
    public void testCreateOrder_insufficientInventoryOnAnyNightDoesNotLockOrInsert() {
        when(orderMapper.selectByClientSubmitToken(TENANT_ID, MERCHANT_ID, "submit-2")).thenReturn(null);
        when(inventoryMapper.selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 6)))
                .thenReturn(java.util.Arrays.asList(
                        inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID, LocalDate.of(2026, 6, 4), 5, 1, 0),
                        inventory(INVENTORY_ID + 1, TENANT_ID, MERCHANT_ID, LocalDate.of(2026, 6, 5), 2, 1, 0)));

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.createOrder(new BookingOrderCreateReqVO()
                .setTenantId(TENANT_ID).setMerchantId(MERCHANT_ID).setRoomTypeId(ROOM_TYPE_ID)
                .setCheckInDate(LocalDate.of(2026, 6, 4)).setCheckOutDate(LocalDate.of(2026, 6, 6))
                .setRoomQuantity(2).setClientSubmitToken("submit-2")));

        assertEquals(BOOKING_INVENTORY_NOT_ENOUGH.getCode(), exception.getCode());
        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));
        verify(orderMapper, never()).insert(any(BookingOrderDO.class));
    }

    @Test
    public void testCreateOrder_lockInventoryByTenantAndMerchant() {
        BookingInventoryDO inventory = inventory(TENANT_ID, MERCHANT_ID, 5, 1, 0);
        when(inventoryMapper.selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 5))).thenReturn(java.util.Collections.singletonList(inventory));
        doAnswer(invocation -> {
            BookingOrderDO order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return null;
        }).when(orderMapper).insert(any(BookingOrderDO.class));

        Long id = orderService.createOrder(new BookingOrderCreateReqVO().setTenantId(TENANT_ID).setMerchantId(MERCHANT_ID)
                .setRoomTypeId(ROOM_TYPE_ID).setCheckInDate(LocalDate.of(2026, 6, 4)).setCheckOutDate(LocalDate.of(2026, 6, 5))
                .setRoomQuantity(2).setClientSubmitToken("submit-tenant-merchant").setGuestName("guest").setGuestMobile("13800000000"));

        assertEquals(ORDER_ID, id);
        verify(merchantContextService).validateMerchantOwner(MERCHANT_ID);
        verify(merchantContextService).validateMerchantTenant(MERCHANT_ID, TENANT_ID);
        verify(inventoryMapper).selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 5));

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(INVENTORY_ID, inventoryCaptor.getValue().getId());
        assertEquals(3, inventoryCaptor.getValue().getLockedQuantity());

        ArgumentCaptor<BookingOrderDO> orderCaptor = ArgumentCaptor.forClass(BookingOrderDO.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertEquals(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus(), orderCaptor.getValue().getStatus());
        assertEquals(0, orderCaptor.getValue().getPayStatus());
        assertEquals(MERCHANT_ID, orderCaptor.getValue().getMerchantId());
    }

    @Test
    public void testCreateOrder_crossMerchantInventoryDeniedBeforeLockWrongRow() {
        when(inventoryMapper.selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 5)))
                .thenReturn(java.util.Collections.emptyList());

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.createOrder(new BookingOrderCreateReqVO()
                .setTenantId(TENANT_ID).setMerchantId(MERCHANT_ID).setRoomTypeId(ROOM_TYPE_ID)
                .setCheckInDate(LocalDate.of(2026, 6, 4)).setCheckOutDate(LocalDate.of(2026, 6, 5)).setRoomQuantity(1).setClientSubmitToken("submit-cross-merchant")));

        assertEquals(BOOKING_INVENTORY_NOT_EXISTS.getCode(), exception.getCode());
        verify(inventoryMapper).selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 5));
        verify(orderMapper, never()).insert(any(BookingOrderDO.class));
    }

    @Test
    public void testCreateOrder_crossTenantInventoryDeniedBeforeLockWrongRow() {
        when(inventoryMapper.selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 5)))
                .thenReturn(java.util.Collections.emptyList());

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.createOrder(new BookingOrderCreateReqVO()
                .setTenantId(TENANT_ID).setMerchantId(MERCHANT_ID).setRoomTypeId(ROOM_TYPE_ID)
                .setCheckInDate(LocalDate.of(2026, 6, 4)).setCheckOutDate(LocalDate.of(2026, 6, 5)).setRoomQuantity(1).setClientSubmitToken("submit-cross-tenant")));

        assertEquals(BOOKING_INVENTORY_NOT_EXISTS.getCode(), exception.getCode());
        verify(inventoryMapper).selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 5));
        verify(orderMapper, never()).insert(any(BookingOrderDO.class));
    }

    @Test
    public void testCreateOrder_insufficientInventory() {
        when(inventoryMapper.selectListByRoomTypeIdAndDateRangeForUpdate(TENANT_ID, MERCHANT_ID, ROOM_TYPE_ID,
                LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 5)))
                .thenReturn(java.util.Collections.singletonList(inventory(TENANT_ID, MERCHANT_ID, 2, 1, 0)));

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.createOrder(new BookingOrderCreateReqVO()
                .setTenantId(TENANT_ID).setMerchantId(MERCHANT_ID).setRoomTypeId(ROOM_TYPE_ID)
                .setCheckInDate(LocalDate.of(2026, 6, 4)).setCheckOutDate(LocalDate.of(2026, 6, 5)).setRoomQuantity(2).setClientSubmitToken("submit-insufficient")));
        assertEquals(BOOKING_INVENTORY_NOT_ENOUGH.getCode(), exception.getCode());
        verify(orderMapper, never()).insert(any(BookingOrderDO.class));
    }

    @Test
    public void testConfirmCancelCompleteStatusFlow() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(BookingOrderStatusEnum.PENDING_CONFIRM.getStatus()));

        orderService.confirmOrder(ORDER_ID, MERCHANT_ID);
        assertLastOrderStatus(BookingOrderStatusEnum.CONFIRMED);

        BookingOrderDO confirmed = order(BookingOrderStatusEnum.CONFIRMED.getStatus());
        confirmed.setRoomQuantity(2);
        confirmed.setInventoryId(INVENTORY_ID);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(confirmed);
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(TENANT_ID, MERCHANT_ID, 5, 3, 1));

        orderService.completeOrder(ORDER_ID, MERCHANT_ID);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(INVENTORY_ID, inventoryCaptor.getValue().getId());
        assertEquals(1, inventoryCaptor.getValue().getLockedQuantity());
        assertEquals(3, inventoryCaptor.getValue().getSoldQuantity());
        assertLastOrderStatus(BookingOrderStatusEnum.COMPLETED);
    }

    @Test
    public void testCancelOrder_releaseInventory() {
        BookingOrderDO pending = order(BookingOrderStatusEnum.PENDING_CONFIRM.getStatus());
        pending.setPayStatus(0);
        pending.setRoomQuantity(2);
        pending.setInventoryId(INVENTORY_ID);
        BookingInventoryDO inventory = inventory(TENANT_ID, MERCHANT_ID, 5, 3, 0);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(pending);
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory);

        orderService.cancelOrder(ORDER_ID, MERCHANT_ID);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(1, inventoryCaptor.getValue().getLockedQuantity());
        ArgumentCaptor<BookingInventoryRecordDO> recordCaptor = ArgumentCaptor.forClass(BookingInventoryRecordDO.class);
        verify(inventoryRecordMapper).insert(recordCaptor.capture());
        assertEquals(BookingInventoryActionTypeEnum.MERCHANT_REJECT_RELEASE.getType(), recordCaptor.getValue().getActionType());
        assertEquals("ADMIN_USER", recordCaptor.getValue().getOperatorType());
        assertLastOrderStatus(BookingOrderStatusEnum.CLOSED_CANCELLED);
    }

    @Test
    public void testCancelOrder_paidWaitingConfirmShouldReleaseInventory() {
        BookingOrderDO paid = order(BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus());
        paid.setPayStatus(10);
        paid.setRoomQuantity(2);
        paid.setInventoryId(INVENTORY_ID);
        BookingInventoryDO inventory = inventory(TENANT_ID, MERCHANT_ID, 5, 3, 0);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(paid);
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory);

        orderService.cancelOrderByUser(ORDER_ID, MERCHANT_ID);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(1, inventoryCaptor.getValue().getLockedQuantity());
        ArgumentCaptor<BookingInventoryRecordDO> recordCaptor = ArgumentCaptor.forClass(BookingInventoryRecordDO.class);
        verify(inventoryRecordMapper).insert(recordCaptor.capture());
        assertEquals(BookingInventoryActionTypeEnum.USER_CANCEL_RELEASE.getType(), recordCaptor.getValue().getActionType());
        assertEquals(-2, recordCaptor.getValue().getQuantity());
        assertEquals("locked=3,sold=0", recordCaptor.getValue().getBeforeSnapshot());
        assertEquals("locked=1,sold=0", recordCaptor.getValue().getAfterSnapshot());
        assertEquals("APP_USER", recordCaptor.getValue().getOperatorType());
        assertLastOrderStatus(BookingOrderStatusEnum.REFUND_APPLYING);
    }

    @Test
    public void testCancelOrder_confirmedPaidShouldReleaseSoldInventoryForEveryNight() {
        BookingOrderDO confirmed = order(BookingOrderStatusEnum.CONFIRMED.getStatus());
        confirmed.setPayStatus(10);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(confirmed);
        BookingOrderLockDO firstLock = orderLock(501L, INVENTORY_ID, LocalDate.of(2026, 6, 4), 2)
                .setLockStatus(BookingOrderLockStatusEnum.SOLD.getStatus());
        BookingOrderLockDO secondLock = orderLock(502L, INVENTORY_ID + 1, LocalDate.of(2026, 6, 5), 2)
                .setLockStatus(BookingOrderLockStatusEnum.SOLD.getStatus());
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Arrays.asList(firstLock, secondLock));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 4), 5, 1, 2));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID + 1)).thenReturn(inventory(INVENTORY_ID + 1, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 5), 5, 0, 3));

        orderService.cancelOrderByUser(ORDER_ID, MERCHANT_ID);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper, times(2)).updateById(inventoryCaptor.capture());
        assertEquals(1, inventoryCaptor.getAllValues().get(0).getLockedQuantity());
        assertEquals(0, inventoryCaptor.getAllValues().get(0).getSoldQuantity());
        assertEquals(0, inventoryCaptor.getAllValues().get(1).getLockedQuantity());
        assertEquals(1, inventoryCaptor.getAllValues().get(1).getSoldQuantity());

        ArgumentCaptor<BookingOrderLockDO> lockCaptor = ArgumentCaptor.forClass(BookingOrderLockDO.class);
        verify(orderLockMapper, times(2)).updateById(lockCaptor.capture());
        assertTrue(lockCaptor.getAllValues().stream().allMatch(lock ->
                BookingOrderLockStatusEnum.RELEASED.getStatus().equals(lock.getLockStatus())));

        ArgumentCaptor<java.util.Collection<BookingInventoryRecordDO>> recordCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(inventoryRecordMapper).insertBatch(recordCaptor.capture());
        java.util.List<BookingInventoryRecordDO> records = new java.util.ArrayList<>(recordCaptor.getValue());
        assertEquals(2, records.size());
        assertEquals("locked=1,sold=2", records.get(0).getBeforeSnapshot());
        assertEquals("locked=1,sold=0", records.get(0).getAfterSnapshot());
        assertEquals(BookingInventoryActionTypeEnum.USER_CANCEL_RELEASE.getType(), records.get(0).getActionType());
        assertLastOrderStatus(BookingOrderStatusEnum.REFUND_APPLYING);
    }

    @Test
    public void testCancelOrderByUser_refundedOrderShouldNotReleaseOrRecordAgain() {
        BookingOrderDO paid = order(BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus());
        paid.setPayStatus(10);
        BookingOrderDO refunded = order(BookingOrderStatusEnum.REFUNDED.getStatus());
        refunded.setPayStatus(20);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(paid, refunded);
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(
                inventory(TENANT_ID, MERCHANT_ID, 5, 1, 0));

        orderService.cancelOrderByUser(ORDER_ID, MERCHANT_ID);
        orderService.cancelOrderByUser(ORDER_ID, MERCHANT_ID);

        verify(inventoryMapper, times(1)).updateById(any(BookingInventoryDO.class));
        verify(inventoryRecordMapper, times(1)).insert(any(BookingInventoryRecordDO.class));
    }

    @Test
    public void testCancelOrder_paidWaitingConfirmShouldCloseWhenLockedInventoryMissing() {
        BookingOrderDO paid = order(BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus());
        paid.setPayStatus(10);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(paid);
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Collections.singletonList(
                orderLock(501L, INVENTORY_ID, LocalDate.of(2026, 6, 4), 1)));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(null);

        orderService.cancelOrderByUser(ORDER_ID, MERCHANT_ID);

        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));
        ArgumentCaptor<BookingOrderLockDO> lockCaptor = ArgumentCaptor.forClass(BookingOrderLockDO.class);
        verify(orderLockMapper).updateById(lockCaptor.capture());
        assertEquals(BookingOrderLockStatusEnum.RELEASED.getStatus(), lockCaptor.getValue().getLockStatus());
        assertLastOrderStatus(BookingOrderStatusEnum.REFUND_APPLYING);
    }

    @Test
    public void testCancelOrder_legacyPaidOrderShouldCloseWhenFallbackInventoryMissing() {
        BookingOrderDO paid = order(BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus());
        paid.setPayStatus(10);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(paid);
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(null);

        orderService.cancelOrder(ORDER_ID, MERCHANT_ID);

        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));
        verify(orderLockMapper, never()).updateById(any(BookingOrderLockDO.class));
        assertLastOrderStatus(BookingOrderStatusEnum.REFUND_APPLYING);
    }

    @Test
    public void testMarkRefunded_shouldFinishRefundAndBeIdempotent() {
        BookingOrderDO applying = order(BookingOrderStatusEnum.REFUND_APPLYING.getStatus());
        applying.setPayStatus(10);
        BookingOrderDO refunded = order(BookingOrderStatusEnum.REFUNDED.getStatus());
        refunded.setPayStatus(20);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(applying, refunded);

        orderService.markRefunded(ORDER_ID);
        orderService.markRefunded(ORDER_ID);

        verify(orderMapper, times(1)).updateById(argThat((BookingOrderDO update) ->
                BookingOrderStatusEnum.REFUNDED.getStatus().equals(update.getStatus())
                        && Integer.valueOf(20).equals(update.getPayStatus())
                        && Integer.valueOf(10).equals(update.getRefundStatus())));
    }

    @Test
    public void testMarkRefundedManually_shouldValidateOwnerAndFinishRefund() {
        BookingOrderDO applying = order(BookingOrderStatusEnum.REFUND_APPLYING.getStatus());
        applying.setPayStatus(10);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(applying);

        orderService.markRefundedManually(ORDER_ID, MERCHANT_ID);

        verify(orderMapper).updateById(argThat((BookingOrderDO update) ->
                BookingOrderStatusEnum.REFUNDED.getStatus().equals(update.getStatus())
                        && Integer.valueOf(20).equals(update.getPayStatus())
                        && Integer.valueOf(10).equals(update.getRefundStatus())));
    }

    @Test
    public void testApproveRefund_shouldMovePendingApprovalToProcessing() {
        BookingOrderDO applying = order(BookingOrderStatusEnum.REFUND_APPLYING.getStatus());
        applying.setRefundStatus(0);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(applying);

        assertTrue(orderService.approveRefund(ORDER_ID, MERCHANT_ID));

        verify(orderMapper).updateById(argThat((BookingOrderDO update) ->
                ORDER_ID.equals(update.getId()) && Integer.valueOf(1).equals(update.getRefundStatus())));
    }

    @Test
    public void testApproveRefund_shouldBeIdempotentWhileProcessing() {
        BookingOrderDO applying = order(BookingOrderStatusEnum.REFUND_APPLYING.getStatus());
        applying.setRefundStatus(1);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(applying);

        assertFalse(orderService.approveRefund(ORDER_ID, MERCHANT_ID));

        verify(orderMapper, never()).updateById(any(BookingOrderDO.class));
    }

    @Test
    public void testCancelOrder_releaseOrderLocks() {
        BookingOrderDO pending = order(BookingOrderStatusEnum.PENDING_CONFIRM.getStatus());
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(pending);
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Arrays.asList(
                orderLock(501L, INVENTORY_ID, LocalDate.of(2026, 6, 4), 2),
                orderLock(502L, INVENTORY_ID + 1, LocalDate.of(2026, 6, 5), 2)));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 4), 5, 3, 0));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID + 1)).thenReturn(inventory(INVENTORY_ID + 1, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 5), 5, 2, 1));

        orderService.cancelOrderByUser(ORDER_ID, MERCHANT_ID);

        ArgumentCaptor<BookingOrderLockDO> lockCaptor = ArgumentCaptor.forClass(BookingOrderLockDO.class);
        verify(orderLockMapper, times(2)).updateById(lockCaptor.capture());
        assertEquals(BookingOrderLockStatusEnum.RELEASED.getStatus(), lockCaptor.getAllValues().get(0).getLockStatus());
        assertEquals(BookingOrderLockStatusEnum.RELEASED.getStatus(), lockCaptor.getAllValues().get(1).getLockStatus());
        ArgumentCaptor<java.util.Collection<BookingInventoryRecordDO>> recordCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(inventoryRecordMapper).insertBatch(recordCaptor.capture());
        java.util.List<BookingInventoryRecordDO> records = new java.util.ArrayList<>(recordCaptor.getValue());
        assertEquals(2, records.size());
        assertTrue(records.stream().allMatch(record -> BookingInventoryActionTypeEnum.USER_CANCEL_RELEASE.getType()
                .equals(record.getActionType())));
        assertTrue(records.stream().allMatch(record -> "APP_USER".equals(record.getOperatorType())));
    }

    @Test
    public void testHandlePaySuccess_autoConfirmMovesLockedToSoldAndConfirms() {
        BookingOrderDO pending = order(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        pending.setAutoConfirmSnapshot(true);
        pending.setPayStatus(0);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(pending);
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Arrays.asList(
                orderLock(501L, INVENTORY_ID, LocalDate.of(2026, 6, 4), 2),
                orderLock(502L, INVENTORY_ID + 1, LocalDate.of(2026, 6, 5), 2)));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 4), 5, 3, 0));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID + 1)).thenReturn(inventory(INVENTORY_ID + 1, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 5), 5, 2, 1));

        orderService.handlePaySuccess(ORDER_ID, 9001L);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper, times(2)).updateById(inventoryCaptor.capture());
        assertEquals(1, inventoryCaptor.getAllValues().get(0).getLockedQuantity());
        assertEquals(2, inventoryCaptor.getAllValues().get(0).getSoldQuantity());
        assertEquals(0, inventoryCaptor.getAllValues().get(1).getLockedQuantity());
        assertEquals(3, inventoryCaptor.getAllValues().get(1).getSoldQuantity());

        ArgumentCaptor<BookingOrderDO> orderCaptor = ArgumentCaptor.forClass(BookingOrderDO.class);
        verify(orderMapper, atLeastOnce()).updateById(orderCaptor.capture());
        BookingOrderDO update = orderCaptor.getAllValues().get(orderCaptor.getAllValues().size() - 1);
        assertEquals(BookingOrderStatusEnum.CONFIRMED.getStatus(), update.getStatus());
        assertEquals(10, update.getPayStatus());
        assertEquals(9001L, update.getPayOrderId());
    }

    @Test
    public void testAssignRoom_persistsTrimmedRoomNumber() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(BookingOrderStatusEnum.CONFIRMED.getStatus()));

        orderService.assignRoom(ORDER_ID, MERCHANT_ID, " 801 ");

        ArgumentCaptor<BookingOrderDO> captor = ArgumentCaptor.forClass(BookingOrderDO.class);
        verify(orderMapper).updateById(captor.capture());
        assertEquals(ORDER_ID, captor.getValue().getId());
        assertEquals("801", captor.getValue().getRoomNo());
    }

    @Test
    public void testHandlePaySuccess_manualConfirmKeepsLockedInventory() {
        BookingOrderDO pending = order(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        pending.setAutoConfirmSnapshot(false);
        pending.setPayStatus(0);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(pending);

        orderService.handlePaySuccess(ORDER_ID, 9002L);

        verify(orderLockMapper, never()).selectListByOrderId(any());
        verify(inventoryMapper, never()).selectByIdForUpdate(any());
        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));

        ArgumentCaptor<BookingOrderDO> orderCaptor = ArgumentCaptor.forClass(BookingOrderDO.class);
        verify(orderMapper).updateById(orderCaptor.capture());
        assertEquals(BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus(), orderCaptor.getValue().getStatus());
        assertEquals(10, orderCaptor.getValue().getPayStatus());
        assertEquals(9002L, orderCaptor.getValue().getPayOrderId());
    }

    @Test
    public void testMarkPaidManually_shouldValidateOwnerWithoutPaymentOrder() {
        BookingOrderDO pending = order(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        pending.setAutoConfirmSnapshot(false);
        pending.setPayStatus(0);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(pending);

        orderService.markPaidManually(ORDER_ID, MERCHANT_ID);

        ArgumentCaptor<BookingOrderDO> orderCaptor = ArgumentCaptor.forClass(BookingOrderDO.class);
        verify(orderMapper).updateById(orderCaptor.capture());
        assertEquals(BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus(), orderCaptor.getValue().getStatus());
        assertEquals(10, orderCaptor.getValue().getPayStatus());
        assertNull(orderCaptor.getValue().getPayOrderId());
    }

    @Test
    public void testConfirmOrder_paidWaitConfirmMovesLockedToSold() {
        BookingOrderDO waitingConfirm = order(BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus());
        when(orderMapper.selectById(ORDER_ID)).thenReturn(waitingConfirm);
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Collections.singletonList(
                orderLock(501L, INVENTORY_ID, LocalDate.of(2026, 6, 4), 1)));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 4), 5, 2, 0));

        orderService.confirmOrder(ORDER_ID, MERCHANT_ID);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(1, inventoryCaptor.getValue().getLockedQuantity());
        assertEquals(1, inventoryCaptor.getValue().getSoldQuantity());
        assertLastOrderStatus(BookingOrderStatusEnum.CONFIRMED);
    }

    @Test
    public void testReleaseExpiredPendingPaymentOrders_releasesOnceAndCloses() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 12, 0);
        BookingOrderDO expired = order(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        expired.setExpireTime(now.minusSeconds(1));
        when(orderMapper.selectListExpiredPendingPayment(now)).thenReturn(Collections.singletonList(expired));
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Collections.singletonList(
                orderLock(501L, INVENTORY_ID, LocalDate.of(2026, 6, 4), 1)));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 4), 5, 2, 0));

        orderService.releaseExpiredPendingPaymentOrders(now);

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(inventoryCaptor.capture());
        assertEquals(1, inventoryCaptor.getValue().getLockedQuantity());

        ArgumentCaptor<BookingOrderLockDO> lockCaptor = ArgumentCaptor.forClass(BookingOrderLockDO.class);
        verify(orderLockMapper).updateById(lockCaptor.capture());
        assertEquals(BookingOrderLockStatusEnum.RELEASED.getStatus(), lockCaptor.getValue().getLockStatus());
        ArgumentCaptor<java.util.Collection<BookingInventoryRecordDO>> recordCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(inventoryRecordMapper).insertBatch(recordCaptor.capture());
        BookingInventoryRecordDO record = new java.util.ArrayList<>(recordCaptor.getValue()).get(0);
        assertEquals(BookingInventoryActionTypeEnum.AUTO_RELEASE.getType(), record.getActionType());
        assertEquals(-1, record.getQuantity());
        assertEquals("SYSTEM_JOB", record.getOperatorType());
        assertLastOrderStatus(BookingOrderStatusEnum.CLOSED_TIMEOUT);
    }

    @Test
    public void testCompleteOrder_soldOrderLocks() {
        BookingOrderDO confirmed = order(BookingOrderStatusEnum.CONFIRMED.getStatus());
        when(orderMapper.selectById(ORDER_ID)).thenReturn(confirmed);
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Arrays.asList(
                orderLock(501L, INVENTORY_ID, LocalDate.of(2026, 6, 4), 2),
                orderLock(502L, INVENTORY_ID + 1, LocalDate.of(2026, 6, 5), 2)));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 4), 5, 3, 0));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID + 1)).thenReturn(inventory(INVENTORY_ID + 1, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 5), 5, 2, 1));

        orderService.completeOrder(ORDER_ID, MERCHANT_ID);

        ArgumentCaptor<BookingOrderLockDO> lockCaptor = ArgumentCaptor.forClass(BookingOrderLockDO.class);
        verify(orderLockMapper, times(2)).updateById(lockCaptor.capture());
        assertEquals(BookingOrderLockStatusEnum.SOLD.getStatus(), lockCaptor.getAllValues().get(0).getLockStatus());
        assertEquals(BookingOrderLockStatusEnum.SOLD.getStatus(), lockCaptor.getAllValues().get(1).getLockStatus());
    }

    @Test
    public void testCheckInOrder_shouldMoveConfirmedOrderToCheckedIn() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(BookingOrderStatusEnum.CONFIRMED.getStatus()));

        orderService.checkInOrder(ORDER_ID, MERCHANT_ID);

        assertLastOrderStatus(BookingOrderStatusEnum.CHECKED_IN);
    }

    @Test
    public void testNoShowOrder_shouldMoveConfirmedOrderToNoShow() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(BookingOrderStatusEnum.CONFIRMED.getStatus()));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(TENANT_ID, MERCHANT_ID, 5, 3, 0));

        orderService.noShowOrder(ORDER_ID, MERCHANT_ID);

        assertLastOrderStatus(BookingOrderStatusEnum.NO_SHOW);
    }

    @Test
    public void testNoShowOrder_shouldReleaseOrderLocks() {
        BookingOrderDO confirmed = order(BookingOrderStatusEnum.CONFIRMED.getStatus());
        when(orderMapper.selectById(ORDER_ID)).thenReturn(confirmed);
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Arrays.asList(
                orderLock(501L, INVENTORY_ID, LocalDate.of(2026, 6, 4), 2),
                orderLock(502L, INVENTORY_ID + 1, LocalDate.of(2026, 6, 5), 2)));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 4), 5, 3, 0));
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID + 1)).thenReturn(inventory(INVENTORY_ID + 1, TENANT_ID, MERCHANT_ID,
                LocalDate.of(2026, 6, 5), 5, 2, 1));

        orderService.noShowOrder(ORDER_ID, MERCHANT_ID);

        ArgumentCaptor<BookingOrderLockDO> lockCaptor = ArgumentCaptor.forClass(BookingOrderLockDO.class);
        verify(orderLockMapper, times(2)).updateById(lockCaptor.capture());
        assertEquals(BookingOrderLockStatusEnum.RELEASED.getStatus(), lockCaptor.getAllValues().get(0).getLockStatus());
        assertEquals(BookingOrderLockStatusEnum.RELEASED.getStatus(), lockCaptor.getAllValues().get(1).getLockStatus());

        ArgumentCaptor<BookingInventoryDO> inventoryCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper, times(2)).updateById(inventoryCaptor.capture());
        assertEquals(1, inventoryCaptor.getAllValues().get(0).getLockedQuantity());
        assertEquals(0, inventoryCaptor.getAllValues().get(1).getLockedQuantity());
        assertLastOrderStatus(BookingOrderStatusEnum.NO_SHOW);
    }

    @Test
    public void testCancelOrder_noShowShouldNotReleaseInventoryAgain() {
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(order(BookingOrderStatusEnum.NO_SHOW.getStatus()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.cancelOrder(ORDER_ID, MERCHANT_ID));

        assertEquals(BOOKING_ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(orderLockMapper, never()).selectListByOrderId(any());
        verify(inventoryMapper, never()).selectByIdForUpdate(any());
        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));
        verify(orderLockMapper, never()).updateById(any(BookingOrderLockDO.class));
        verify(orderMapper, never()).updateById(any(BookingOrderDO.class));
    }

    @Test
    public void testNoShowOrder_crossMerchantInventoryShouldNotReleaseFallbackInventory() {
        BookingOrderDO confirmed = order(BookingOrderStatusEnum.CONFIRMED.getStatus());
        when(orderMapper.selectById(ORDER_ID)).thenReturn(confirmed);
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(INVENTORY_ID, TENANT_ID, MERCHANT_ID + 1,
                LocalDate.of(2026, 6, 4), 5, 3, 0));

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.noShowOrder(ORDER_ID, MERCHANT_ID));

        assertEquals(BOOKING_INVENTORY_NOT_EXISTS.getCode(), exception.getCode());
        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));
        verify(orderLockMapper, never()).updateById(any(BookingOrderLockDO.class));
        verify(orderMapper, never()).updateById(any(BookingOrderDO.class));
    }

    @Test
    public void testNoShowOrder_crossTenantOrderLockShouldNotReleaseInventory() {
        BookingOrderDO confirmed = order(BookingOrderStatusEnum.CONFIRMED.getStatus());
        BookingOrderLockDO otherTenantLock = orderLock(501L, INVENTORY_ID, LocalDate.of(2026, 6, 4), 2);
        otherTenantLock.setTenantId(TENANT_ID + 1);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(confirmed);
        when(orderLockMapper.selectListByOrderId(ORDER_ID)).thenReturn(Collections.singletonList(otherTenantLock));

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.noShowOrder(ORDER_ID, MERCHANT_ID));

        assertEquals(BOOKING_INVENTORY_NOT_EXISTS.getCode(), exception.getCode());
        verify(inventoryMapper, never()).selectByIdForUpdate(any());
        verify(inventoryMapper, never()).updateById(any(BookingInventoryDO.class));
        verify(orderLockMapper, never()).updateById(any(BookingOrderLockDO.class));
        verify(orderMapper, never()).updateById(any(BookingOrderDO.class));
    }

    @Test
    public void testCompleteOrder_shouldAllowCheckedInOrder() {
        BookingOrderDO checkedIn = order(BookingOrderStatusEnum.CHECKED_IN.getStatus());
        checkedIn.setRoomQuantity(2);
        checkedIn.setInventoryId(INVENTORY_ID);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(checkedIn);
        when(inventoryMapper.selectByIdForUpdate(INVENTORY_ID)).thenReturn(inventory(TENANT_ID, MERCHANT_ID, 5, 3, 1));

        orderService.completeOrder(ORDER_ID, MERCHANT_ID);

        assertLastOrderStatus(BookingOrderStatusEnum.COMPLETED);
    }

    @Test
    public void testCreateOrder_blankClientSubmitTokenRejected() {
        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.createOrder(new BookingOrderCreateReqVO()
                .setTenantId(TENANT_ID).setMerchantId(MERCHANT_ID).setRoomTypeId(ROOM_TYPE_ID)
                .setCheckInDate(LocalDate.of(2026, 6, 4)).setCheckOutDate(LocalDate.of(2026, 6, 5))
                .setRoomQuantity(1).setClientSubmitToken(" ")));

        assertEquals(BOOKING_ORDER_DUPLICATE_SUBMIT.getCode(), exception.getCode());
        verify(orderMapper, never()).selectByClientSubmitToken(any(), any(), any());
        verify(inventoryMapper, never()).selectListByRoomTypeIdAndDateRangeForUpdate(any(), any(), any(), any(), any());
    }

    @Test
    public void testGetOrder_crossMerchantDenied() {
        BookingOrderDO order = order(BookingOrderStatusEnum.PENDING_CONFIRM.getStatus());
        order.setMerchantId(999L);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.getOrder(ORDER_ID, MERCHANT_ID));
        assertEquals(BOOKING_ORDER_NOT_EXISTS.getCode(), exception.getCode());
    }

    private void assertLastOrderStatus(BookingOrderStatusEnum status) {
        ArgumentCaptor<BookingOrderDO> orderCaptor = ArgumentCaptor.forClass(BookingOrderDO.class);
        verify(orderMapper, atLeastOnce()).updateById(orderCaptor.capture());
        assertEquals(status.getStatus(), orderCaptor.getValue().getStatus());
    }

    private BookingInventoryDO inventory(Long id, Long tenantId, Long merchantId, LocalDate bizDate, Integer total, Integer locked, Integer sold) {
        BookingInventoryDO inventory = new BookingInventoryDO().setId(id).setMerchantId(merchantId)
                .setRoomTypeId(ROOM_TYPE_ID).setBizDate(bizDate)
                .setTotalQuantity(total).setLockedQuantity(locked).setSoldQuantity(sold)
                .setPrice(id.equals(INVENTORY_ID) ? 19900 : 20900);
        inventory.setTenantId(tenantId);
        return inventory;
    }

    private BookingInventoryDO inventory(Long tenantId, Long merchantId, Integer total, Integer locked, Integer sold) {
        return inventory(INVENTORY_ID, tenantId, merchantId, LocalDate.of(2026, 6, 4), total, locked, sold);
    }

    private BookingOrderLockDO orderLock(Long id, Long inventoryId, LocalDate bizDate, Integer quantity) {
        BookingOrderLockDO lock = new BookingOrderLockDO().setId(id).setOrderId(ORDER_ID).setInventoryId(inventoryId)
                .setMerchantId(MERCHANT_ID).setRoomTypeId(ROOM_TYPE_ID).setBizDate(bizDate)
                .setQuantity(quantity).setLockStatus(BookingOrderLockStatusEnum.LOCKED.getStatus());
        lock.setTenantId(TENANT_ID);
        return lock;
    }

    private BookingOrderDO order(Integer status) {
        BookingOrderDO order = new BookingOrderDO().setId(ORDER_ID).setMerchantId(MERCHANT_ID)
                .setOrderNo("BK20260604001").setRoomTypeId(ROOM_TYPE_ID).setInventoryId(INVENTORY_ID)
                .setStatus(status).setRoomQuantity(1);
        order.setTenantId(TENANT_ID);
        return order;
    }

    private BookingRoomTypeDO roomType(boolean autoConfirm) {
        BookingRoomTypeDO roomType = new BookingRoomTypeDO().setId(ROOM_TYPE_ID).setMerchantId(MERCHANT_ID)
                .setName("湖景大床房").setAutoConfirmEnabled(autoConfirm);
        roomType.setTenantId(TENANT_ID);
        return roomType;
    }
}
