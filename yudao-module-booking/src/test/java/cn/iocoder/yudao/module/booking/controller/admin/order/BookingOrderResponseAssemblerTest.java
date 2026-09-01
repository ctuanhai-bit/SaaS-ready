package cn.iocoder.yudao.module.booking.controller.admin.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderRespVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderLockDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.resource.BookingRoomTypeDO;
import cn.iocoder.yudao.module.booking.dal.mysql.order.BookingOrderLockMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.resource.BookingRoomTypeMapper;
import cn.iocoder.yudao.module.booking.enums.BookingOrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

public class BookingOrderResponseAssemblerTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 9014L;
    private static final Long MERCHANT_ID = 9014L;
    private static final Long ROOM_TYPE_ID = 901206L;

    @InjectMocks
    private BookingOrderResponseAssembler assembler;

    @Mock
    private BookingRoomTypeMapper roomTypeMapper;
    @Mock
    private BookingOrderLockMapper orderLockMapper;

    @Test
    public void toPageRespVO_shouldFillOwnedRoomTypeName() {
        BookingOrderDO order = order();
        BookingRoomTypeDO roomType = roomType(TENANT_ID, MERCHANT_ID, "商务大床房");
        when(roomTypeMapper.selectBatchIds(Collections.singleton(ROOM_TYPE_ID)))
                .thenReturn(Collections.singletonList(roomType));
        when(orderLockMapper.selectListByOrderIds(Collections.singleton(10035L)))
                .thenReturn(Collections.singletonList(new BookingOrderLockDO().setOrderId(10035L)
                        .setPrice(20000).setQuantity(2)));

        PageResult<BookingOrderRespVO> result = assembler.toPageRespVO(
                new PageResult<>(Collections.singletonList(order), 1L));

        assertEquals("商务大床房", result.getList().get(0).getRoomType());
        assertEquals(40000, result.getList().get(0).getAmount());
    }

    @Test
    public void toRespVO_shouldNotExposeCrossMerchantRoomTypeName() {
        when(roomTypeMapper.selectById(ROOM_TYPE_ID))
                .thenReturn(roomType(TENANT_ID, MERCHANT_ID + 1, "其他商户房型"));

        BookingOrderRespVO result = assembler.toRespVO(order());

        assertNull(result.getRoomType());
        assertNull(result.getAmount());
    }

    @Test
    public void toRespVO_shouldExposeGuestAndVerificationCodeForConfirmedOrder() {
        BookingOrderDO order = order().setStatus(BookingOrderStatusEnum.CONFIRMED.getStatus())
                .setGuestName("陈团海").setGuestMobile("15000007186");

        BookingOrderRespVO result = assembler.toRespVO(order);

        assertEquals("陈团海", result.getGuestName());
        assertEquals("15000007186", result.getGuestMobile());
        assertEquals("10035", result.getVerificationCode());
    }

    @Test
    public void toRespVO_shouldHideVerificationCodeForRefundedOrder() {
        BookingOrderDO order = order().setStatus(BookingOrderStatusEnum.REFUNDED.getStatus());

        BookingOrderRespVO result = assembler.toRespVO(order);

        assertNull(result.getVerificationCode());
    }

    private BookingOrderDO order() {
        BookingOrderDO order = new BookingOrderDO().setId(10035L).setMerchantId(MERCHANT_ID)
                .setRoomTypeId(ROOM_TYPE_ID).setOrderNo("BK20260815152135596");
        order.setTenantId(TENANT_ID);
        return order;
    }

    private BookingRoomTypeDO roomType(Long tenantId, Long merchantId, String name) {
        BookingRoomTypeDO roomType = new BookingRoomTypeDO().setId(ROOM_TYPE_ID)
                .setMerchantId(merchantId).setName(name);
        roomType.setTenantId(tenantId);
        return roomType;
    }
}
