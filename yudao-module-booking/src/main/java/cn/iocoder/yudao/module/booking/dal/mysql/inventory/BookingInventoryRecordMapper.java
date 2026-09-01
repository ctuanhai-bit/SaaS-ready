package cn.iocoder.yudao.module.booking.dal.mysql.inventory;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookingInventoryRecordMapper extends BaseMapperX<BookingInventoryRecordDO> {
}
