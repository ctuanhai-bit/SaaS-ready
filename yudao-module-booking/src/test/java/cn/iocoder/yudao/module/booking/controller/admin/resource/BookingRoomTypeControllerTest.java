package cn.iocoder.yudao.module.booking.controller.admin.resource;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.booking.controller.admin.resource.vo.BookingRoomTypeCreateReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.resource.vo.BookingRoomTypePageReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.resource.vo.BookingRoomTypeStatusReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.resource.vo.BookingRoomTypeUpdateReqVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.resource.BookingRoomTypeDO;
import cn.iocoder.yudao.module.booking.dal.mysql.resource.BookingRoomTypeMapper;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.BOOKING_ROOM_TYPE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookingRoomTypeControllerTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 10L;
    private static final Long REQUEST_TENANT_ID = 20L;
    private static final Long CURRENT_MERCHANT_ID = 100L;
    private static final Long REQUEST_MERCHANT_ID = 200L;
    private static final Long ROOM_TYPE_ID = 300L;

    @InjectMocks
    private BookingRoomTypeController controller;

    @Mock
    private BookingRoomTypeMapper roomTypeMapper;
    @Mock
    private MerchantContextService merchantContextService;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testRoomTypeRead_shouldAllowInventoryPermission() throws NoSuchMethodException {
        Method pageMethod = BookingRoomTypeController.class.getMethod("getRoomTypePage", BookingRoomTypePageReqVO.class);
        Method getMethod = BookingRoomTypeController.class.getMethod("getRoomType", Long.class);

        PreAuthorize pagePreAuthorize = AnnotationUtils.findAnnotation(pageMethod, PreAuthorize.class);
        PreAuthorize getPreAuthorize = AnnotationUtils.findAnnotation(getMethod, PreAuthorize.class);

        assertTrue(pagePreAuthorize.value().contains("booking:room-type:query"));
        assertTrue(getPreAuthorize.value().contains("booking:room-type:query"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetRoomTypePage_shouldUseCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        BookingRoomTypePageReqVO pageReqVO = new BookingRoomTypePageReqVO();
        pageReqVO.setMerchantId(REQUEST_MERCHANT_ID);

        controller.getRoomTypePage(pageReqVO);

        assertEquals(CURRENT_MERCHANT_ID, pageReqVO.getMerchantId());
        verify(roomTypeMapper).selectPage(any(BookingRoomTypePageReqVO.class), any(Wrapper.class));
    }

    @Test
    public void testGetSharedFacilityOptions_shouldReturnDistinctCustomFacilities() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(roomTypeMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                new BookingRoomTypeDO().setFacilityCodes(
                        "[\"parking\",\"custom:儿童护栏\",\"custom:空气净化器\"]"),
                new BookingRoomTypeDO().setFacilityCodes(
                        "[\"custom:儿童护栏\",\"wifi\"]")));

        List<String> options = controller.getSharedFacilityOptions().getData();

        assertEquals(Arrays.asList("custom:儿童护栏", "custom:空气净化器"), options);
        verify(merchantContextService).getCurrentMerchantId();
        verify(roomTypeMapper).selectList(any(Wrapper.class));
    }

    @Test
    public void testCreateRoomType_shouldUseCurrentMerchantIdAndCurrentTenantId() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        BookingRoomTypeCreateReqVO createReqVO = new BookingRoomTypeCreateReqVO();
        createReqVO.setTenantId(REQUEST_TENANT_ID);
        createReqVO.setMerchantId(REQUEST_MERCHANT_ID);
        createReqVO.setName("大床房");
        createReqVO.setMaxOccupancy(2);
        createReqVO.setAreaSqmMin(new BigDecimal("28"));
        createReqVO.setAreaSqmMax(new BigDecimal("32.5"));
        createReqVO.setBreakfastIncluded(true);
        createReqVO.setInitialPrice(36800);
        createReqVO.setCoverUrl("saas-jd/room-type/100/deluxe-cover.jpg");
        createReqVO.setImageUrls("[\"saas-jd/room-type/100/detail-1.jpg\",\"saas-jd/room-type/100/detail-2.jpg\"]");
        createReqVO.setFacilityCodes("[\"parking\",\"charging_pile\"]");

        controller.createRoomType(createReqVO);

        assertEquals(CURRENT_MERCHANT_ID, createReqVO.getMerchantId());
        ArgumentCaptor<BookingRoomTypeDO> captor = ArgumentCaptor.forClass(BookingRoomTypeDO.class);
        verify(roomTypeMapper).insert(captor.capture());
        assertEquals(TENANT_ID, captor.getValue().getTenantId());
        assertEquals(new BigDecimal("28"), captor.getValue().getAreaSqm());
        assertEquals(new BigDecimal("28"), captor.getValue().getAreaSqmMin());
        assertEquals(new BigDecimal("32.5"), captor.getValue().getAreaSqmMax());
        assertTrue(captor.getValue().getBreakfastIncluded());
        assertEquals(36800, captor.getValue().getInitialPrice());
        assertEquals("saas-jd/room-type/100/deluxe-cover.jpg", captor.getValue().getCoverUrl());
        assertEquals("[\"saas-jd/room-type/100/detail-1.jpg\",\"saas-jd/room-type/100/detail-2.jpg\"]", captor.getValue().getImageUrls());
        assertEquals("[\"parking\",\"charging_pile\"]", captor.getValue().getFacilityCodes());
    }

    @Test
    public void testCreateRoomTypeValidation_shouldAllowContextDerivedTenantAndMerchant() {
        BookingRoomTypeCreateReqVO createReqVO = new BookingRoomTypeCreateReqVO();
        createReqVO.setName("大床房");
        createReqVO.setMaxOccupancy(2);
        createReqVO.setAreaSqmMin(new BigDecimal("28"));
        createReqVO.setAreaSqmMax(new BigDecimal("32.5"));
        createReqVO.setBreakfastIncluded(false);
        createReqVO.setInitialPrice(36800);

        Set<String> violationFields = validator.validate(createReqVO).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertFalse(violationFields.contains("tenantId"));
        assertFalse(violationFields.contains("merchantId"));
    }

    @Test
    public void testCreateRoomTypeValidation_shouldRequirePositiveInitialPrice() {
        BookingRoomTypeCreateReqVO createReqVO = new BookingRoomTypeCreateReqVO();
        createReqVO.setName("大床房");
        createReqVO.setMaxOccupancy(2);
        createReqVO.setAreaSqmMin(new BigDecimal("28"));
        createReqVO.setAreaSqmMax(new BigDecimal("32.5"));
        createReqVO.setBreakfastIncluded(false);
        createReqVO.setInitialPrice(0);

        Set<String> violationFields = validator.validate(createReqVO).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertTrue(violationFields.contains("initialPrice"));
    }

    @Test
    public void testCreateRoomTypeValidation_shouldRequireValidAreaRangeAndBreakfastSelection() {
        BookingRoomTypeCreateReqVO createReqVO = new BookingRoomTypeCreateReqVO();
        createReqVO.setName("大床房");
        createReqVO.setMaxOccupancy(2);
        createReqVO.setAreaSqmMin(new BigDecimal("35"));
        createReqVO.setAreaSqmMax(new BigDecimal("30"));
        createReqVO.setInitialPrice(36800);

        Set<String> violationFields = validator.validate(createReqVO).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertTrue(violationFields.contains("areaRangeValid"));
        assertTrue(violationFields.contains("breakfastIncluded"));
    }

    @Test
    public void testUpdateRoomTypeStatusValidation_shouldAllowIdAndStatusOnly() {
        BookingRoomTypeStatusReqVO reqVO = new BookingRoomTypeStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(0);

        Set<String> violationFields = validator.validate(reqVO).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertFalse(violationFields.contains("name"));
        assertFalse(violationFields.contains("maxOccupancy"));
    }

    @Test
    public void testUpdateRoomType_shouldUpdateCurrentMerchantRoomType() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(roomTypeMapper.selectById(ROOM_TYPE_ID)).thenReturn(new BookingRoomTypeDO()
                .setId(ROOM_TYPE_ID).setMerchantId(CURRENT_MERCHANT_ID));
        BookingRoomTypeUpdateReqVO reqVO = new BookingRoomTypeUpdateReqVO();
        reqVO.setId(ROOM_TYPE_ID);
        reqVO.setName("大床房");
        reqVO.setMaxOccupancy(2);
        reqVO.setAreaSqmMin(new BigDecimal("35"));
        reqVO.setAreaSqmMax(new BigDecimal("42"));
        reqVO.setBreakfastIncluded(true);
        reqVO.setInitialPrice(42800);
        reqVO.setStatus(0);
        reqVO.setCoverUrl("saas-jd/room-type/100/new-cover.jpg");
        reqVO.setImageUrls("[\"saas-jd/room-type/100/new-1.jpg\"]");
        reqVO.setFacilityCodes("[\"projection\"]");

        assertTrue(controller.updateRoomType(reqVO).getData());

        ArgumentCaptor<BookingRoomTypeDO> captor = ArgumentCaptor.forClass(BookingRoomTypeDO.class);
        verify(roomTypeMapper).updateById(captor.capture());
        assertEquals(ROOM_TYPE_ID, captor.getValue().getId());
        assertEquals("大床房", captor.getValue().getName());
        assertEquals(new BigDecimal("35"), captor.getValue().getAreaSqm());
        assertEquals(new BigDecimal("35"), captor.getValue().getAreaSqmMin());
        assertEquals(new BigDecimal("42"), captor.getValue().getAreaSqmMax());
        assertTrue(captor.getValue().getBreakfastIncluded());
        assertEquals(42800, captor.getValue().getInitialPrice());
        assertEquals("saas-jd/room-type/100/new-cover.jpg", captor.getValue().getCoverUrl());
        assertEquals("[\"saas-jd/room-type/100/new-1.jpg\"]", captor.getValue().getImageUrls());
        assertEquals("[\"projection\"]", captor.getValue().getFacilityCodes());
    }

    @Test
    public void testUpdateRoomType_whenCrossMerchant_shouldThrow() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(roomTypeMapper.selectById(ROOM_TYPE_ID)).thenReturn(new BookingRoomTypeDO()
                .setId(ROOM_TYPE_ID).setMerchantId(REQUEST_MERCHANT_ID));
        BookingRoomTypeUpdateReqVO reqVO = new BookingRoomTypeUpdateReqVO();
        reqVO.setId(ROOM_TYPE_ID);
        reqVO.setName("大床房");
        reqVO.setMaxOccupancy(2);
        reqVO.setInitialPrice(36800);

        assertServiceException(() -> controller.updateRoomType(reqVO), BOOKING_ROOM_TYPE_NOT_EXISTS);

        verify(roomTypeMapper, never()).updateById(any(BookingRoomTypeDO.class));
    }

    @Test
    public void testUpdateRoomTypeStatus_shouldUpdateCurrentMerchantRoomType() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(roomTypeMapper.selectById(ROOM_TYPE_ID)).thenReturn(new BookingRoomTypeDO()
                .setId(ROOM_TYPE_ID).setMerchantId(CURRENT_MERCHANT_ID).setStatus(0));
        BookingRoomTypeStatusReqVO reqVO = new BookingRoomTypeStatusReqVO();
        reqVO.setId(ROOM_TYPE_ID);
        reqVO.setStatus(1);

        assertTrue(controller.updateRoomTypeStatus(reqVO).getData());

        ArgumentCaptor<BookingRoomTypeDO> captor = ArgumentCaptor.forClass(BookingRoomTypeDO.class);
        verify(roomTypeMapper).updateById(captor.capture());
        assertEquals(ROOM_TYPE_ID, captor.getValue().getId());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    public void testUpdateRoomTypeStatus_whenNotFound_shouldThrow() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(roomTypeMapper.selectById(ROOM_TYPE_ID)).thenReturn(null);
        BookingRoomTypeStatusReqVO reqVO = new BookingRoomTypeStatusReqVO();
        reqVO.setId(ROOM_TYPE_ID);
        reqVO.setStatus(1);

        assertServiceException(() -> controller.updateRoomTypeStatus(reqVO), BOOKING_ROOM_TYPE_NOT_EXISTS);

        verify(roomTypeMapper, never()).updateById(any(BookingRoomTypeDO.class));
    }

    @Test
    public void testUpdateRoomTypeStatus_whenCrossMerchant_shouldThrow() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(roomTypeMapper.selectById(ROOM_TYPE_ID)).thenReturn(new BookingRoomTypeDO()
                .setId(ROOM_TYPE_ID).setMerchantId(REQUEST_MERCHANT_ID).setStatus(0));
        BookingRoomTypeStatusReqVO reqVO = new BookingRoomTypeStatusReqVO();
        reqVO.setId(ROOM_TYPE_ID);
        reqVO.setStatus(1);

        assertServiceException(() -> controller.updateRoomTypeStatus(reqVO), BOOKING_ROOM_TYPE_NOT_EXISTS);

        verify(roomTypeMapper, never()).updateById(any(BookingRoomTypeDO.class));
    }

    @Test
    public void testDeleteRoomType_shouldDeleteCurrentMerchantRoomType() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(roomTypeMapper.selectById(ROOM_TYPE_ID)).thenReturn(new BookingRoomTypeDO()
                .setId(ROOM_TYPE_ID).setMerchantId(CURRENT_MERCHANT_ID));

        assertTrue(controller.deleteRoomType(ROOM_TYPE_ID).getData());

        verify(roomTypeMapper).deleteById(ROOM_TYPE_ID);
    }

    @Test
    public void testDeleteRoomType_whenCrossMerchant_shouldThrow() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(roomTypeMapper.selectById(ROOM_TYPE_ID)).thenReturn(new BookingRoomTypeDO()
                .setId(ROOM_TYPE_ID).setMerchantId(REQUEST_MERCHANT_ID));

        assertServiceException(() -> controller.deleteRoomType(ROOM_TYPE_ID), BOOKING_ROOM_TYPE_NOT_EXISTS);

        verify(roomTypeMapper, never()).deleteById(ROOM_TYPE_ID);
    }

    @Test
    public void testDeleteRoomType_shouldRequireCommunityDeletePermission() throws NoSuchMethodException {
        Method method = BookingRoomTypeController.class.getMethod("deleteRoomType", Long.class);
        PreAuthorize preAuthorize = AnnotationUtils.findAnnotation(method, PreAuthorize.class);

        assertTrue(preAuthorize.value().contains("booking:room-type:delete"));
    }
}
