package cn.iocoder.yudao.module.merchant.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode MERCHANT_NOT_EXISTS = new ErrorCode(1_020_000_000, "商户不存在");
    ErrorCode MERCHANT_DISABLED = new ErrorCode(1_020_000_001, "商户已停用");
    ErrorCode MERCHANT_TENANT_NOT_MATCH = new ErrorCode(1_020_000_002, "商户租户不匹配");
    ErrorCode MERCHANT_CONTEXT_NOT_EXISTS = new ErrorCode(1_020_000_003, "商户上下文不存在");
    ErrorCode MERCHANT_NAME_DUPLICATE = new ErrorCode(1_020_000_004, "商户名称已存在");
    ErrorCode MERCHANT_AUDIT_STATUS_INVALID = new ErrorCode(1_020_000_005, "商户审核状态不允许当前操作");

}
