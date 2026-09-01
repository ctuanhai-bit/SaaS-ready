package cn.iocoder.yudao.module.merchant.framework.context;

public class MerchantContextHolder {

    private static final ThreadLocal<Long> MERCHANT_ID = new ThreadLocal<>();

    public static Long getMerchantId() {
        return MERCHANT_ID.get();
    }

    public static Long getRequiredMerchantId() {
        Long merchantId = getMerchantId();
        if (merchantId == null) {
            throw new NullPointerException("MerchantContextHolder 不存在商户编号！");
        }
        return merchantId;
    }

    public static void setMerchantId(Long merchantId) {
        MERCHANT_ID.set(merchantId);
    }

    public static void clear() {
        MERCHANT_ID.remove();
    }

}
