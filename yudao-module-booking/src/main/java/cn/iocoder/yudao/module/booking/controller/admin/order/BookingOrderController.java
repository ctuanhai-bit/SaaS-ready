package cn.iocoder.yudao.module.booking.controller.admin.order;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderCreateReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderPageReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderRespVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderDO;
import cn.iocoder.yudao.module.booking.service.order.BookingOrderService;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_CONTEXT_NOT_EXISTS;

@Tag(name = "管理后台 - 订房订单")
@RestController
@RequestMapping("/booking/order")
@Validated
public class BookingOrderController {

    @Resource
    private BookingOrderService orderService;
    @Resource
    private BookingOrderResponseAssembler responseAssembler;
    @Resource
    private MerchantContextService merchantContextService;
    @PostMapping("/create")
    @Operation(summary = "创建订房订单")
    @PreAuthorize("@ss.hasPermission('booking:order:create')")
    public CommonResult<Long> createOrder(@Valid @RequestBody BookingOrderCreateReqVO createReqVO) {
        createReqVO.setTenantId(TenantContextHolder.getRequiredTenantId());
        createReqVO.setMerchantId(currentMerchantId());
        return success(orderService.createOrder(createReqVO));
    }

    private Long currentMerchantId() {
        Long merchantId = merchantContextService.getCurrentMerchantId();
        if (merchantId == null) {
            throw exception(MERCHANT_CONTEXT_NOT_EXISTS);
        }
        return merchantId;
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认订房订单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> confirmOrder(@RequestParam("id") Long id) {
        orderService.confirmOrder(id, currentMerchantId());
        return success(true);
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认订房订单（兼容前端 body 契约）")
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> confirmOrderBody(@Valid @RequestBody OrderActionReqVO reqVO) {
        orderService.confirmOrder(reqVO.getId(), currentMerchantId());
        return success(true);
    }

    @PostMapping("/check-in")
    @Operation(summary = "办理订房入住")
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> checkInOrder(@Valid @RequestBody OrderActionReqVO reqVO) {
        orderService.checkInOrder(reqVO.getId(), currentMerchantId());
        return success(true);
    }

    @PostMapping("/manual-paid")
    @Operation(summary = "登记线下收款完成（不调用支付渠道）")
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> markPaidManually(@Valid @RequestBody OrderActionReqVO reqVO) {
        orderService.markPaidManually(reqVO.getId(), currentMerchantId());
        return success(true);
    }

    @PostMapping("/manual-refunded")
    @Operation(summary = "登记线下退款完成（不调用支付渠道）")
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> markRefundedManually(@Valid @RequestBody OrderActionReqVO reqVO) {
        orderService.markRefundedManually(reqVO.getId(), currentMerchantId());
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消订房订单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> cancelOrder(@RequestParam("id") Long id) {
        orderService.cancelOrder(id, currentMerchantId());
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成订房订单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:order:update')")
    public CommonResult<Boolean> completeOrder(@RequestParam("id") Long id) {
        orderService.completeOrder(id, currentMerchantId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得订房订单")
    @PreAuthorize("@ss.hasPermission('booking:order:query')")
    public CommonResult<BookingOrderRespVO> getOrder(@RequestParam("id") Long id) {
        return success(responseAssembler.toRespVO(
                orderService.getOrder(id, currentMerchantId())));
    }

    @GetMapping("/page")
    @Operation(summary = "获得订房订单分页")
    @PreAuthorize("@ss.hasPermission('booking:order:query')")
    public CommonResult<PageResult<BookingOrderRespVO>> getOrderPage(@Valid BookingOrderPageReqVO pageReqVO) {
        Long merchantId = currentMerchantId();
        PageResult<BookingOrderDO> pageResult = orderService.getOrderPage(pageReqVO, merchantId);
        return success(responseAssembler.toPageRespVO(pageResult));
    }

    @Data
    @Accessors(chain = true)
    public static class OrderActionReqVO {
        @NotNull
        private Long id;
        private String roomNo;
        private String reason;
    }
}
