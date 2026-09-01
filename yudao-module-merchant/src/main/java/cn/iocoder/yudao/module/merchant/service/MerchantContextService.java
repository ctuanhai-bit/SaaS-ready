package cn.iocoder.yudao.module.merchant.service;

public interface MerchantContextService {

    Long getCurrentMerchantId();

    void validateMerchantOwner(Long merchantId);

    void validateMerchantTenant(Long merchantId, Long tenantId);

}
