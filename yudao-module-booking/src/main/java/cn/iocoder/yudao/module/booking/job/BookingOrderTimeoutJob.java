package cn.iocoder.yudao.module.booking.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.booking.service.order.BookingOrderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 订房订单支付超时释放库存 Job。
 */
@Component
public class BookingOrderTimeoutJob implements JobHandler {

    @Resource
    private BookingOrderService bookingOrderService;

    @Override
    @TenantJob
    public String execute(String param) {
        int count = bookingOrderService.releaseExpiredPendingPaymentOrders(LocalDateTime.now());
        return StrUtil.format("订房订单超时释放 {} 个", count);
    }
}
