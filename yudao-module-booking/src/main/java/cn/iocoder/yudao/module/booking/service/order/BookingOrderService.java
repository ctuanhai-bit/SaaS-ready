package cn.iocoder.yudao.module.booking.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderCreateReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderPageReqVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderDO;

import java.time.LocalDateTime;

public interface BookingOrderService {

    Long createOrder(BookingOrderCreateReqVO createReqVO);

    void handlePaySuccess(Long id, Long payOrderId);

    void markPaidManually(Long id, Long merchantId);

    int releaseExpiredPendingPaymentOrders(LocalDateTime now);

    void confirmOrder(Long id, Long merchantId);

    void cancelOrder(Long id, Long merchantId);

    void cancelOrderByUser(Long id, Long merchantId);

    boolean approveRefund(Long id, Long merchantId);

    void markRefunded(Long id);

    void markRefundedManually(Long id, Long merchantId);

    void checkInOrder(Long id, Long merchantId);

    void noShowOrder(Long id, Long merchantId);

    void assignRoom(Long id, Long merchantId, String roomNo);

    void completeOrder(Long id, Long merchantId);

    BookingOrderDO getOrder(Long id, Long merchantId);

    PageResult<BookingOrderDO> getOrderPage(BookingOrderPageReqVO pageReqVO, Long merchantId);
}
