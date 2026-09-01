package cn.iocoder.yudao.module.booking.controller.admin.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderRespVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderLockDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.resource.BookingRoomTypeDO;
import cn.iocoder.yudao.module.booking.dal.mysql.order.BookingOrderLockMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.resource.BookingRoomTypeMapper;
import cn.iocoder.yudao.module.booking.enums.BookingOrderStatusEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BookingOrderResponseAssembler {

    @Resource
    private BookingRoomTypeMapper roomTypeMapper;
    @Resource
    private BookingOrderLockMapper orderLockMapper;

    public BookingOrderRespVO toRespVO(BookingOrderDO order) {
        if (order == null) {
            return null;
        }
        BookingOrderRespVO respVO = BeanUtils.toBean(order, BookingOrderRespVO.class);
        BookingRoomTypeDO roomType = order.getRoomTypeId() == null ? null : roomTypeMapper.selectById(order.getRoomTypeId());
        BookingRoomTypeDO ownedRoomType = belongsToOrder(roomType, order.getTenantId(), order.getMerchantId())
                ? roomType : null;
        if (ownedRoomType != null) {
            respVO.setRoomType(ownedRoomType.getName());
        }
        respVO.setAmount(calculateAmount(respVO, ownedRoomType, orderLockMapper.selectListByOrderId(order.getId())));
        respVO.setVerificationCode(verificationCode(order));
        return respVO;
    }

    public PageResult<BookingOrderRespVO> toPageRespVO(PageResult<BookingOrderDO> pageResult) {
        PageResult<BookingOrderRespVO> respPage = BeanUtils.toBean(pageResult, BookingOrderRespVO.class);
        List<BookingOrderRespVO> orders = respPage.getList();
        if (orders == null || orders.isEmpty()) {
            return respPage;
        }
        Set<Long> roomTypeIds = orders.stream().map(BookingOrderRespVO::getRoomTypeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BookingRoomTypeDO> roomTypeMap = roomTypeIds.isEmpty() ? Collections.emptyMap()
                : roomTypeMapper.selectBatchIds(roomTypeIds).stream()
                .collect(Collectors.toMap(BookingRoomTypeDO::getId, Function.identity(), (left, right) -> left));
        Set<Long> orderIds = orders.stream().map(BookingOrderRespVO::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<BookingOrderLockDO> locks = orderLockMapper.selectListByOrderIds(orderIds);
        Map<Long, List<BookingOrderLockDO>> lockMap = locks == null ? Collections.emptyMap()
                : locks.stream().collect(Collectors.groupingBy(BookingOrderLockDO::getOrderId));
        for (BookingOrderRespVO order : orders) {
            BookingRoomTypeDO roomType = roomTypeMap.get(order.getRoomTypeId());
            BookingRoomTypeDO ownedRoomType = belongsToOrder(roomType, order.getTenantId(), order.getMerchantId())
                    ? roomType : null;
            if (ownedRoomType != null) {
                order.setRoomType(ownedRoomType.getName());
            }
            order.setAmount(calculateAmount(order, ownedRoomType,
                    lockMap.getOrDefault(order.getId(), Collections.emptyList())));
        }
        return respPage;
    }

    private Integer calculateAmount(BookingOrderRespVO order, BookingRoomTypeDO roomType,
                                    List<BookingOrderLockDO> locks) {
        int fallbackPrice = roomType != null && roomType.getInitialPrice() != null
                && roomType.getInitialPrice() > 0 ? roomType.getInitialPrice() : 0;
        long amount;
        if (locks != null && !locks.isEmpty()) {
            amount = locks.stream().mapToLong(lock -> {
                int price = lock.getPrice() != null && lock.getPrice() > 0 ? lock.getPrice() : fallbackPrice;
                int quantity = lock.getQuantity() != null && lock.getQuantity() > 0
                        ? lock.getQuantity() : positiveQuantity(order.getRoomQuantity());
                return (long) price * quantity;
            }).sum();
        } else if (fallbackPrice > 0 && order.getCheckInDate() != null && order.getCheckOutDate() != null) {
            long nights = ChronoUnit.DAYS.between(order.getCheckInDate(), order.getCheckOutDate());
            amount = nights > 0 ? (long) fallbackPrice * positiveQuantity(order.getRoomQuantity()) * nights : 0;
        } else {
            amount = 0;
        }
        return amount > 0 && amount <= Integer.MAX_VALUE ? (int) amount : null;
    }

    private int positiveQuantity(Integer quantity) {
        return quantity != null && quantity > 0 ? quantity : 1;
    }

    private String verificationCode(BookingOrderDO order) {
        Integer status = order.getStatus();
        return BookingOrderStatusEnum.CONFIRMED.getStatus().equals(status)
                || BookingOrderStatusEnum.CHECKED_IN.getStatus().equals(status)
                ? String.valueOf(order.getId()) : null;
    }

    private boolean belongsToOrder(BookingRoomTypeDO roomType, Long tenantId, Long merchantId) {
        return roomType != null && Objects.equals(roomType.getTenantId(), tenantId)
                && Objects.equals(roomType.getMerchantId(), merchantId);
    }
}
