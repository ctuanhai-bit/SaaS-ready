package cn.iocoder.yudao.module.booking.controller.admin.inventory;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryRecordDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.resource.BookingRoomTypeDO;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryRecordMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.resource.BookingRoomTypeMapper;
import cn.iocoder.yudao.module.booking.service.stock.BookingStockService;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.BOOKING_INVENTORY_BATCH_TOO_LARGE;
import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.BOOKING_ROOM_TYPE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookingInventoryControllerTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 10L;
    private static final Long MERCHANT_ID = 100L;
    private static final Long ROOM_TYPE_ID_1 = 301L;
    private static final Long ROOM_TYPE_ID_2 = 302L;
    private static final LocalDate DATE_1 = LocalDate.of(2026, 7, 18);
    private static final LocalDate DATE_2 = LocalDate.of(2026, 7, 19);

    @InjectMocks
    private BookingInventoryController controller;

    @Mock
    private BookingInventoryMapper inventoryMapper;
    @Mock
    private BookingInventoryRecordMapper inventoryRecordMapper;
    @Mock
    private BookingRoomTypeMapper roomTypeMapper;
    @Mock
    private BookingStockService stockService;
    @Mock
    private MerchantContextService merchantContextService;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateInventoryCalendar_shouldUpdateMultipleRoomTypesAndDates() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(merchantContextService.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
        when(roomTypeMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                new BookingRoomTypeDO().setId(ROOM_TYPE_ID_1).setMerchantId(MERCHANT_ID),
                new BookingRoomTypeDO().setId(ROOM_TYPE_ID_2).setMerchantId(MERCHANT_ID)));
        BookingInventoryDO existingInventory = new BookingInventoryDO()
                .setId(901L)
                .setMerchantId(MERCHANT_ID)
                .setRoomTypeId(ROOM_TYPE_ID_1)
                .setBizDate(DATE_1)
                .setTotalQuantity(8)
                .setLockedQuantity(1)
                .setSoldQuantity(2)
                .setPrice(29900);
        existingInventory.setTenantId(TENANT_ID);
        when(inventoryMapper.selectByRoomTypeIdAndDateForUpdate(
                anyLong(), anyLong(), anyLong(), any(LocalDate.class)))
                .thenAnswer(invocation -> Objects.equals(invocation.getArgument(2), ROOM_TYPE_ID_1)
                        && Objects.equals(invocation.getArgument(3), DATE_1) ? existingInventory : null);

        BookingInventoryController.InventoryUpdateReqVO reqVO = new BookingInventoryController.InventoryUpdateReqVO();
        reqVO.setRoomTypeIds(Arrays.asList(ROOM_TYPE_ID_1, ROOM_TYPE_ID_2));
        reqVO.setDates(Arrays.asList(DATE_1, DATE_2));
        reqVO.setAvailable(6);
        reqVO.setPrice(39900);

        assertTrue(controller.updateInventoryCalendar(reqVO).getData());

        verify(inventoryMapper, times(4)).selectByRoomTypeIdAndDateForUpdate(
                anyLong(), anyLong(), anyLong(), any(LocalDate.class));
        ArgumentCaptor<BookingInventoryDO> updateCaptor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).updateById(updateCaptor.capture());
        assertEquals(9, updateCaptor.getValue().getTotalQuantity());
        assertEquals(39900, updateCaptor.getValue().getPrice());
        verify(inventoryMapper, times(3)).insert(any(BookingInventoryDO.class));
        verify(inventoryRecordMapper, times(4)).insert(any(BookingInventoryRecordDO.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateInventoryCalendar_shouldRejectCrossMerchantRoomType() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
        when(roomTypeMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(
                new BookingRoomTypeDO().setId(ROOM_TYPE_ID_1).setMerchantId(MERCHANT_ID)));
        BookingInventoryController.InventoryUpdateReqVO reqVO = new BookingInventoryController.InventoryUpdateReqVO();
        reqVO.setRoomTypeIds(Arrays.asList(ROOM_TYPE_ID_1, ROOM_TYPE_ID_2));
        reqVO.setDates(Collections.singletonList(DATE_1));

        assertServiceException(() -> controller.updateInventoryCalendar(reqVO), BOOKING_ROOM_TYPE_NOT_EXISTS);

        verify(inventoryMapper, never()).selectByRoomTypeIdAndDateForUpdate(
                anyLong(), anyLong(), anyLong(), any(LocalDate.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateInventoryCalendar_shouldKeepSingleUpdateCompatibility() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(merchantContextService.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
        when(roomTypeMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(
                new BookingRoomTypeDO().setId(ROOM_TYPE_ID_1).setMerchantId(MERCHANT_ID)));
        BookingInventoryController.InventoryUpdateReqVO reqVO = new BookingInventoryController.InventoryUpdateReqVO();
        reqVO.setRoomTypeId(ROOM_TYPE_ID_1);
        reqVO.setDate(DATE_1);
        reqVO.setAvailable(5);
        reqVO.setPrice(28800);

        assertTrue(controller.updateInventoryCalendar(reqVO).getData());

        verify(inventoryMapper).insert(any(BookingInventoryDO.class));
        verify(inventoryRecordMapper).insert(any(BookingInventoryRecordDO.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateInventoryCalendar_shouldUseRoomTypeInitialPriceWhenPriceIsMissing() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(merchantContextService.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
        when(roomTypeMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(
                new BookingRoomTypeDO().setId(ROOM_TYPE_ID_1).setMerchantId(MERCHANT_ID).setInitialPrice(32800)));
        BookingInventoryController.InventoryUpdateReqVO reqVO = new BookingInventoryController.InventoryUpdateReqVO();
        reqVO.setRoomTypeId(ROOM_TYPE_ID_1);
        reqVO.setDate(DATE_1);
        reqVO.setAvailable(5);

        assertTrue(controller.updateInventoryCalendar(reqVO).getData());

        ArgumentCaptor<BookingInventoryDO> captor = ArgumentCaptor.forClass(BookingInventoryDO.class);
        verify(inventoryMapper).insert(captor.capture());
        assertEquals(32800, captor.getValue().getPrice());
    }

    @Test
    public void testUpdateInventoryCalendar_shouldRejectMoreThanOneThousandTargets() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
        List<LocalDate> dates = IntStream.rangeClosed(0, 1000)
                .mapToObj(DATE_1::plusDays)
                .collect(Collectors.toList());
        BookingInventoryController.InventoryUpdateReqVO reqVO = new BookingInventoryController.InventoryUpdateReqVO();
        reqVO.setRoomTypeIds(Collections.singletonList(ROOM_TYPE_ID_1));
        reqVO.setDates(dates);

        assertServiceException(() -> controller.updateInventoryCalendar(reqVO), BOOKING_INVENTORY_BATCH_TOO_LARGE);

        verify(roomTypeMapper, never()).selectList(any(Wrapper.class));
    }
}
