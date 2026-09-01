package cn.iocoder.yudao.module.booking.controller.admin.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderPageReqVO;
import cn.iocoder.yudao.module.booking.enums.BookingOrderStatusEnum;
import cn.iocoder.yudao.module.booking.service.order.BookingOrderService;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_CONTEXT_NOT_EXISTS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookingOrderControllerTest extends BaseMockitoUnitTest {

    private static final Long ORDER_ID = 400L;
    private static final Long CURRENT_MERCHANT_ID = 100L;

    @InjectMocks
    private BookingOrderController bookingOrderController;
    @InjectMocks
    private BookingFrontDeskController bookingFrontDeskController;

    @Mock
    private BookingOrderService orderService;
    @Mock
    private BookingOrderResponseAssembler responseAssembler;
    @Mock
    private MerchantContextService merchantContextService;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testConfirmOrder_shouldUseCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);

        bookingOrderController.confirmOrder(ORDER_ID);

        verify(orderService).confirmOrder(ORDER_ID, CURRENT_MERCHANT_ID);
    }

    @Test
    public void testConfirmOrder_whenMerchantContextMissing_shouldReject() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(null);

        assertServiceException(() -> bookingOrderController.confirmOrder(ORDER_ID), MERCHANT_CONTEXT_NOT_EXISTS);
    }

    @Test
    public void testGetOrderPage_whenMerchantContextMissing_shouldReject() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(null);

        assertServiceException(() -> bookingOrderController.getOrderPage(new BookingOrderPageReqVO()),
                MERCHANT_CONTEXT_NOT_EXISTS);
    }

    @Test
    public void testGetOrderPage_shouldPassKeywordToService() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(orderService.getOrderPage(any(BookingOrderPageReqVO.class), eq(CURRENT_MERCHANT_ID)))
                .thenReturn(PageResult.empty());

        bookingOrderController.getOrderPage(new BookingOrderPageReqVO().setKeyword("陈团海"));

        verify(orderService).getOrderPage(argThat((BookingOrderPageReqVO reqVO) ->
                "陈团海".equals(reqVO.getKeyword())), eq(CURRENT_MERCHANT_ID));
    }

    @Test
    public void testConfirmOrderBody_shouldUseCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);

        bookingOrderController.confirmOrderBody(new BookingOrderController.OrderActionReqVO().setId(ORDER_ID));

        verify(orderService).confirmOrder(ORDER_ID, CURRENT_MERCHANT_ID);
    }

    @Test
    public void testCheckInOrder_shouldUseCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);

        bookingOrderController.checkInOrder(new BookingOrderController.OrderActionReqVO().setId(ORDER_ID));

        verify(orderService).checkInOrder(ORDER_ID, CURRENT_MERCHANT_ID);
    }

    @Test
    public void testManualPaymentActions_shouldUseCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        BookingOrderController.OrderActionReqVO reqVO =
                new BookingOrderController.OrderActionReqVO().setId(ORDER_ID);

        bookingOrderController.markPaidManually(reqVO);
        bookingOrderController.markRefundedManually(reqVO);

        verify(orderService).markPaidManually(ORDER_ID, CURRENT_MERCHANT_ID);
        verify(orderService).markRefundedManually(ORDER_ID, CURRENT_MERCHANT_ID);
    }

    @Test
    public void testOrderActionValidation_shouldRequireId() {
        Set<String> violationFields = validator.validate(new BookingOrderController.OrderActionReqVO()).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertFalse(violationFields.isEmpty());
        assertTrue(violationFields.contains("id"));
    }

    @Test
    public void testFrontDeskCheckOut_shouldUseCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);

        bookingFrontDeskController.checkOut(ORDER_ID, null);

        verify(orderService).completeOrder(ORDER_ID, CURRENT_MERCHANT_ID);
    }

    @Test
    public void testFrontDeskArrivals_shouldUseConfirmedStatusAndCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(orderService.getOrderPage(any(BookingOrderPageReqVO.class), eq(CURRENT_MERCHANT_ID)))
                .thenReturn(PageResult.empty());

        bookingFrontDeskController.getArrivals(new BookingOrderPageReqVO());

        verify(orderService).getOrderPage(argThat((BookingOrderPageReqVO reqVO) ->
                BookingOrderStatusEnum.CONFIRMED.getStatus().equals(reqVO.getStatus())), eq(CURRENT_MERCHANT_ID));
    }

    @Test
    public void testFrontDeskInHouse_shouldUseCheckedInStatusAndCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);
        when(orderService.getOrderPage(any(BookingOrderPageReqVO.class), eq(CURRENT_MERCHANT_ID)))
                .thenReturn(PageResult.empty());

        bookingFrontDeskController.getInHouse(new BookingOrderPageReqVO());

        verify(orderService).getOrderPage(argThat((BookingOrderPageReqVO reqVO) ->
                BookingOrderStatusEnum.CHECKED_IN.getStatus().equals(reqVO.getStatus())), eq(CURRENT_MERCHANT_ID));
    }

    @Test
    public void testFrontDeskSearchOrders_whenMerchantContextMissing_shouldReject() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(null);

        assertServiceException(() -> bookingFrontDeskController.getOrderPage(new BookingOrderPageReqVO()),
                MERCHANT_CONTEXT_NOT_EXISTS);
    }

    @Test
    public void testCancelOrder_shouldUseCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);

        bookingOrderController.cancelOrder(ORDER_ID);

        verify(orderService).cancelOrder(ORDER_ID, CURRENT_MERCHANT_ID);
    }

    @Test
    public void testCompleteOrder_shouldUseCurrentMerchantId() {
        when(merchantContextService.getCurrentMerchantId()).thenReturn(CURRENT_MERCHANT_ID);

        bookingOrderController.completeOrder(ORDER_ID);

        verify(orderService).completeOrder(ORDER_ID, CURRENT_MERCHANT_ID);
    }

    @Test
    public void testFrontDeskActions_shouldUseCommunityBookingPermissions() {
        assertPreAuthorizeContains(BookingFrontDeskController.class, "getOrderPage", "booking:order:query");
        assertPreAuthorizeContains(BookingFrontDeskController.class, "getArrivals", "booking:order:query");
        assertPreAuthorizeContains(BookingFrontDeskController.class, "getInHouse", "booking:order:query");
        assertPreAuthorizeContains(BookingFrontDeskController.class, "getDepartures", "booking:order:query");
        assertPreAuthorizeContains(BookingFrontDeskController.class, "assignRoom", "booking:order:update");
        assertPreAuthorizeContains(BookingFrontDeskController.class, "checkIn", "booking:order:update");
        assertPreAuthorizeContains(BookingFrontDeskController.class, "checkOut", "booking:order:update");
        assertPreAuthorizeContains(BookingFrontDeskController.class, "noShow", "booking:order:update");
    }

    @Test
    public void testMerchantFrontDeskGetMappings_shouldMatchFrontendPaths() {
        RequestMapping classMapping = BookingFrontDeskController.class.getAnnotation(RequestMapping.class);

        assertTrue(Arrays.asList(classMapping.value()).contains("/booking/front-desk"));
        assertGetMappingContains("getOrderPage", "/orders");
        assertGetMappingContains("getArrivals", "/arrivals");
        assertGetMappingContains("getInHouse", "/in-house");
        assertGetMappingContains("getDepartures", "/departures");
    }

    private void assertGetMappingContains(String methodName, String path) {
        String mappings = Arrays.stream(BookingFrontDeskController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .map(method -> method.getAnnotation(GetMapping.class))
                .map(GetMapping::value)
                .flatMap(Arrays::stream)
                .collect(Collectors.joining(";"));
        assertTrue(mappings.contains(path), methodName + " should map " + path + ", actual: " + mappings);
    }

    private void assertPreAuthorizeContains(Class<?> controllerClass, String methodName, String permission) {
        String expressions = Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .map(method -> method.getAnnotation(PreAuthorize.class))
                .map(PreAuthorize::value)
                .collect(Collectors.joining(";"));
        assertTrue(expressions.contains(permission), methodName + " should allow " + permission + ", actual: " + expressions);
    }
}
