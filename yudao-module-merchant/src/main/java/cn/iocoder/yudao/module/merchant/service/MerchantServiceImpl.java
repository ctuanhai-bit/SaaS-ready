package cn.iocoder.yudao.module.merchant.service;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.merchant.controller.admin.vo.MerchantUpdateReqVO;
import cn.iocoder.yudao.module.merchant.dal.dataobject.MerchantDO;
import cn.iocoder.yudao.module.merchant.dal.mysql.MerchantMapper;
import cn.iocoder.yudao.module.merchant.enums.MerchantStatusEnum;
import cn.iocoder.yudao.module.merchant.framework.context.MerchantContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_CONTEXT_NOT_EXISTS;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_DISABLED;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_NOT_EXISTS;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_TENANT_NOT_MATCH;

@Service
@Validated
public class MerchantServiceImpl implements MerchantService {

    @Resource
    private MerchantMapper merchantMapper;

    @Override
    public void updateMerchant(MerchantUpdateReqVO updateReqVO) {
        validateMerchantExists(updateReqVO.getId());
        validateNameDuplicate(updateReqVO.getName(), updateReqVO.getId());
        merchantMapper.updateById(BeanUtils.toBean(updateReqVO, MerchantDO.class));
    }

    @Override
    public MerchantDO getMerchant(Long id) {
        return merchantMapper.selectById(id);
    }

    @Override
    public Long getCurrentMerchantId() {
        Long merchantId = MerchantContextHolder.getMerchantId();
        if (merchantId != null) {
            return merchantId;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return null;
        }
        MerchantDO merchant = merchantMapper.selectEnabledByTenantId(tenantId);
        return merchant == null ? null : merchant.getId();
    }

    @Override
    public void validateMerchantOwner(Long merchantId) {
        Long currentMerchantId = getCurrentMerchantId();
        if (currentMerchantId == null || !currentMerchantId.equals(merchantId)) {
            throw exception(MERCHANT_CONTEXT_NOT_EXISTS);
        }
    }

    @Override
    public void validateMerchantTenant(Long merchantId, Long tenantId) {
        MerchantDO merchant = validateMerchantExists(merchantId);
        if (!tenantId.equals(merchant.getTenantId())) {
            throw exception(MERCHANT_TENANT_NOT_MATCH);
        }
        if (!tenantId.equals(TenantContextHolder.getRequiredTenantId())) {
            throw exception(MERCHANT_TENANT_NOT_MATCH);
        }
        if (!MerchantStatusEnum.ENABLE.getStatus().equals(merchant.getStatus())) {
            throw exception(MERCHANT_DISABLED);
        }
    }

    private MerchantDO validateMerchantExists(Long id) {
        MerchantDO merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw exception(MERCHANT_NOT_EXISTS);
        }
        return merchant;
    }

    private void validateNameDuplicate(String name, Long id) {
        MerchantDO merchant = merchantMapper.selectByName(name);
        if (merchant != null && !merchant.getId().equals(id)) {
            throw exception(MERCHANT_NAME_DUPLICATE);
        }
    }

}
