package cn.iocoder.yudao.module.booking.dal.mysql.order;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderLockDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface BookingOrderLockMapper extends BaseMapperX<BookingOrderLockDO> {

    default List<BookingOrderLockDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<BookingOrderLockDO>()
                .eq(BookingOrderLockDO::getOrderId, orderId));
    }

    default List<BookingOrderLockDO> selectListByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<BookingOrderLockDO>()
                .in(BookingOrderLockDO::getOrderId, orderIds));
    }
}
