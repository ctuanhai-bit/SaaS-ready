package cn.iocoder.yudao.module.booking.controller.admin.order;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderPageReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderRespVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderDO;
import cn.iocoder.yudao.module.booking.enums.BookingOrderStatusEnum;
import cn.iocoder.yudao.module.booking.service.order.BookingOrderService;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_CONTEXT_NOT_EXISTS;

@Tag(name = "管理后台 - 订房前台")
@RestController
@RequestMapping("/booking/front-desk")
@Validated
public class BookingFrontDeskController {

    @Resource
    private BookingOrderService orderService;
    @Resource
    private BookingOrderResponseAssembler responseAssembler;
    @Resource
    private MerchantContextService merchantContextService;
    @GetMapping("/orders")
    @Operation(summary = "前台搜索订房订单")
    @PreAuthorize("@ss.hasPermission('booking:order:query')")
    public CommonResult<PageResult<BookingOrderRespVO>> getOrderPage(@Valid BookingOrderPageReqVO pageReqVO) {
        pageReqVO.setActiveOnServiceDate(true);
        return getFrontDeskOrderPage(pageReqVO, null);
    }

    @GetMapping("/arrivals")
    @Operation(summary = "前台今日到店订房订单")
    @PreAuthorize("@ss.hasPermission('booking:order:query')")
    public CommonResult<PageResult<BookingOrderRespVO>> getArrivals(@Valid BookingOrderPageReqVO pageReqVO) {
        LocalDate serviceDate = defaultServiceDate(pageReqVO);
        pageReqVO.setCheckInDate(new LocalDate[]{serviceDate, serviceDate});
        pageReqVO.setServiceDate(null);
        return getFrontDeskOrderPage(pageReqVO, BookingOrderStatusEnum.CONFIRMED);
    }

    @GetMapping("/in-house")
    @Operation(summary = "前台在住订房订单")
    @PreAuthorize("@ss.hasPermission('booking:order:query')")
    public CommonResult<PageResult<BookingOrderRespVO>> getInHouse(@Valid BookingOrderPageReqVO pageReqVO) {
        defaultServiceDate(pageReqVO);
        pageReqVO.setActiveOnServiceDate(true);
        return getFrontDeskOrderPage(pageReqVO, BookingOrderStatusEnum.CHECKED_IN);
    }

    @GetMapping("/departures")
    @Operation(summary = "前台今日离店订房订单")
    @PreAuthorize("@ss.hasPermission('booking:order:query')")
    public CommonResult<PageResult<BookingOrderRespVO>> getDepartures(@Valid BookingOrderPageReqVO pageReqVO) {
        LocalDate serviceDate = defaultServiceDate(pageReqVO);
        pageReqVO.setCheckOutDate(new LocalDate[]{serviceDate, serviceDate});
        pageReqVO.setServiceDate(null);
        return getFrontDeskOrderPage(pageReqVO, BookingOrderStatusEnum.CHECKED_IN);
    }

    @PostMapping("/orders/{id}/assign-room")
    @Operation(summary = "前台分房")
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> assignRoom(@PathVariable("id") Long id, @Valid @RequestBody AssignRoomReqVO reqVO) {
        orderService.assignRoom(id, currentMerchantId(), reqVO.getRoomNo());
        return success(true);
    }

    @PostMapping("/orders/{id}/check-in")
    @Operation(summary = "前台入住")
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> checkIn(@PathVariable("id") Long id, @RequestBody(required = false) BookingOrderController.OrderActionReqVO reqVO) {
        orderService.checkInOrder(id, currentMerchantId());
        return success(true);
    }

    @PostMapping("/orders/{id}/check-out")
    @Operation(summary = "前台退房")
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> checkOut(@PathVariable("id") Long id, @RequestBody(required = false) BookingOrderController.OrderActionReqVO reqVO) {
        orderService.completeOrder(id, currentMerchantId());
        return success(true);
    }

    @PostMapping("/orders/{id}/no-show")
    @Operation(summary = "前台标记未到店")
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> noShow(@PathVariable("id") Long id, @RequestBody(required = false) BookingOrderController.OrderActionReqVO reqVO) {
        orderService.noShowOrder(id, currentMerchantId());
        return success(true);
    }

    private CommonResult<PageResult<BookingOrderRespVO>> getFrontDeskOrderPage(BookingOrderPageReqVO pageReqVO, BookingOrderStatusEnum status) {
        if (status != null) {
            pageReqVO.setStatus(status.getStatus());
        }
        Long merchantId = currentMerchantId();
        PageResult<BookingOrderDO> pageResult = orderService.getOrderPage(pageReqVO, merchantId);
        return success(responseAssembler.toPageRespVO(pageResult));
    }

    private LocalDate defaultServiceDate(BookingOrderPageReqVO pageReqVO) {
        if (pageReqVO.getServiceDate() == null) {
            pageReqVO.setServiceDate(LocalDate.now());
        }
        return pageReqVO.getServiceDate();
    }

    @lombok.Data
    public static class AssignRoomReqVO {
        @NotBlank(message = "房号不能为空")
        private String roomNo;
    }

    private Long currentMerchantId() {
        Long merchantId = merchantContextService.getCurrentMerchantId();
        if (merchantId == null) {
            throw exception(MERCHANT_CONTEXT_NOT_EXISTS);
        }
        return merchantId;
    }
}
