package cn.iocoder.yudao.module.booking.dal.mysql.resource;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.booking.dal.dataobject.resource.BookingRoomDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookingRoomMapper extends BaseMapperX<BookingRoomDO> {
}
