package cn.iocoder.yudao.module.merchant.service;

import cn.iocoder.yudao.module.merchant.controller.admin.vo.MerchantUpdateReqVO;
import cn.iocoder.yudao.module.merchant.dal.dataobject.MerchantDO;

public interface MerchantService extends MerchantContextService {

    void updateMerchant(MerchantUpdateReqVO updateReqVO);

    MerchantDO getMerchant(Long id);

}
