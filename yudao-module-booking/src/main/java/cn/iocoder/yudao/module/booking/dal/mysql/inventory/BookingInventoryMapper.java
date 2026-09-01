package cn.iocoder.yudao.module.booking.dal.mysql.inventory;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BookingInventoryMapper extends BaseMapperX<BookingInventoryDO> {

    default BookingInventoryDO selectByRoomTypeIdAndDateForUpdate(Long tenantId, Long merchantId, Long roomTypeId, LocalDate bizDate) {
        return selectOne(new LambdaQueryWrapperX<BookingInventoryDO>()
                .eq(BookingInventoryDO::getTenantId, tenantId)
                .eq(BookingInventoryDO::getMerchantId, merchantId)
                .eq(BookingInventoryDO::getRoomTypeId, roomTypeId)
                .eq(BookingInventoryDO::getBizDate, bizDate)
                .last("FOR UPDATE"));
    }

    default List<BookingInventoryDO> selectListByRoomTypeIdAndDateRangeForUpdate(Long tenantId, Long merchantId, Long roomTypeId,
                                                                           LocalDate checkInDate, LocalDate checkOutDate) {
        return selectList(new LambdaQueryWrapperX<BookingInventoryDO>()
                .eq(BookingInventoryDO::getTenantId, tenantId)
                .eq(BookingInventoryDO::getMerchantId, merchantId)
                .eq(BookingInventoryDO::getRoomTypeId, roomTypeId)
                .ge(BookingInventoryDO::getBizDate, checkInDate)
                .lt(BookingInventoryDO::getBizDate, checkOutDate)
                .last("FOR UPDATE"));
    }

    default BookingInventoryDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<BookingInventoryDO>()
                .eq(BookingInventoryDO::getId, id)
                .last("FOR UPDATE"));
    }
}
