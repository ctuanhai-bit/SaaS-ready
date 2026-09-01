package cn.iocoder.yudao.module.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MerchantAuditStatusEnum {

    PENDING(0, "待初审"),
    REVIEWING(4, "复核中"),
    APPROVED(1, "已通过"),
    REJECTED(3, "已驳回"),
    SUPPLEMENT(5, "补件"),
    MANUAL_REVIEW(6, "人工复核"),
    DISABLED(2, "停用"),
    FROZEN(7, "冻结");

    private final Integer status;
    private final String name;

}
