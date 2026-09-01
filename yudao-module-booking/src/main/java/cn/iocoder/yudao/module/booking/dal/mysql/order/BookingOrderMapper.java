package cn.iocoder.yudao.module.booking.dal.mysql.order;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderPageReqVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderDO;
import cn.iocoder.yudao.module.booking.enums.BookingOrderStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookingOrderMapper extends BaseMapperX<BookingOrderDO> {

    default PageResult<BookingOrderDO> selectPage(BookingOrderPageReqVO reqVO, Long merchantId) {
        LambdaQueryWrapperX<BookingOrderDO> query = new LambdaQueryWrapperX<BookingOrderDO>()
                .eqIfPresent(BookingOrderDO::getMerchantId, merchantId)
                .eqIfPresent(BookingOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(BookingOrderDO::getRoomTypeId, reqVO.getRoomTypeId())
                .betweenIfPresent(BookingOrderDO::getCheckInDate, reqVO.getCheckInDate())
                .betweenIfPresent(BookingOrderDO::getCheckOutDate, reqVO.getCheckOutDate())
                .orderByDesc(BookingOrderDO::getId);
        if (Boolean.TRUE.equals(reqVO.getActiveOnServiceDate()) && reqVO.getServiceDate() != null) {
            query.le(BookingOrderDO::getCheckInDate, reqVO.getServiceDate())
                    .gt(BookingOrderDO::getCheckOutDate, reqVO.getServiceDate());
        }
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            query.and(wrapper -> wrapper.like(BookingOrderDO::getOrderNo, keyword)
                    .or().like(BookingOrderDO::getGuestName, keyword)
                    .or().like(BookingOrderDO::getGuestMobile, keyword));
        }
        return selectPage(reqVO, query);
    }

    default BookingOrderDO selectByClientSubmitToken(Long tenantId, Long merchantId, String clientSubmitToken) {
        return selectOne(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getTenantId, tenantId)
                .eq(BookingOrderDO::getMerchantId, merchantId)
                .eq(BookingOrderDO::getClientSubmitToken, clientSubmitToken));
    }

    default BookingOrderDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getId, id)
                .last("FOR UPDATE"));
    }

    default List<BookingOrderDO> selectListExpiredPendingPayment(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getStatus, BookingOrderStatusEnum.PENDING_PAYMENT.getStatus())
                .le(BookingOrderDO::getExpireTime, now));
    }
}
