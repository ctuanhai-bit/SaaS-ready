package cn.iocoder.yudao.module.merchant.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.merchant.dal.dataobject.MerchantDO;
import cn.iocoder.yudao.module.merchant.enums.MerchantStatusEnum;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper extends BaseMapperX<MerchantDO> {

    default MerchantDO selectByName(String name) {
        return selectOne(MerchantDO::getName, name);
    }

    default MerchantDO selectEnabledByTenantId(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<MerchantDO>()
                .eq(MerchantDO::getTenantId, tenantId)
                .eq(MerchantDO::getStatus, MerchantStatusEnum.ENABLE.getStatus())
                .orderByDesc(MerchantDO::getId)
                .last("LIMIT 1"));
    }

    default MerchantDO selectByTenantId(Long tenantId) {
        return selectOne(MerchantDO::getTenantId, tenantId);
    }

}
