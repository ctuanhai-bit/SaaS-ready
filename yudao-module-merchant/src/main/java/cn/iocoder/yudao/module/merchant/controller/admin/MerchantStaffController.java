package cn.iocoder.yudao.module.merchant.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.validation.Mobile;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.merchant.dal.dataobject.MerchantDO;
import cn.iocoder.yudao.module.merchant.service.MerchantService;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserUpdateStatusReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_CONTEXT_NOT_EXISTS;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;

@Tag(name = "管理后台 - 酒店员工")
@RestController
@RequestMapping("/merchant/staff")
@Validated
public class MerchantStaffController {

    @Resource
    private MerchantService merchantService;
    @Resource
    private AdminUserService userService;

    @GetMapping("/page")
    @Operation(summary = "获得当前酒店员工分页列表")
    @PreAuthorize("@ss.hasPermission('merchant:staff:query')")
    public CommonResult<PageResult<MerchantStaffRespVO>> getStaffPage(@Valid MerchantStaffPageReqVO pageReqVO) {
        validateCurrentMerchantTenant();
        PageResult<AdminUserDO> pageResult = userService.getUserPage(toUserPageReqVO(pageReqVO));
        return success(new PageResult<>(BeanUtils.toBean(pageResult.getList(), MerchantStaffRespVO.class),
                pageResult.getTotal()));
    }

    @GetMapping("/list")
    @Operation(summary = "获得当前酒店员工详情列表")
    @Parameter(name = "ids", description = "用户编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('merchant:staff:query')")
    public CommonResult<List<MerchantStaffRespVO>> getStaffList(@RequestParam("ids") @NotEmpty List<Long> ids) {
        validateCurrentMerchantTenant();
        List<AdminUserDO> users = userService.getUserList(ids);
        if (CollUtil.isEmpty(users)) {
            return success(Collections.emptyList());
        }
        return success(BeanUtils.toBean(users, MerchantStaffRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新增当前酒店员工")
    @PreAuthorize("@ss.hasPermission('merchant:staff:create')")
    public CommonResult<Long> createStaff(@Valid @RequestBody MerchantStaffSaveReqVO reqVO) {
        validateCurrentMerchantTenant();
        UserSaveReqVO userReqVO = toUserSaveReqVO(reqVO);
        userReqVO.setId(null);
        return success(userService.createUser(userReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改当前酒店员工")
    @PreAuthorize("@ss.hasPermission('merchant:staff:update')")
    public CommonResult<Boolean> updateStaff(@Valid @RequestBody MerchantStaffSaveReqVO reqVO) {
        validateCurrentMerchantTenant();
        validateCurrentTenantUser(reqVO.getId());
        userService.updateUser(toUserSaveReqVO(reqVO));
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改当前酒店员工状态")
    @PreAuthorize("@ss.hasPermission('merchant:staff:update')")
    public CommonResult<Boolean> updateStaffStatus(@Valid @RequestBody UserUpdateStatusReqVO reqVO) {
        validateCurrentMerchantTenant();
        validateCurrentTenantUser(reqVO.getId());
        userService.updateUserStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    private void validateCurrentMerchantTenant() {
        Long merchantId = merchantService.getCurrentMerchantId();
        if (merchantId == null) {
            throw exception(MERCHANT_CONTEXT_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(MERCHANT_CONTEXT_NOT_EXISTS);
        }
        merchantService.validateMerchantTenant(merchantId, tenantId);
    }

    private void validateCurrentTenantUser(Long id) {
        if (id == null || userService.getUser(id) == null) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    private UserSaveReqVO toUserSaveReqVO(MerchantStaffSaveReqVO reqVO) {
        UserSaveReqVO userReqVO = new UserSaveReqVO();
        userReqVO.setId(reqVO.getId());
        userReqVO.setUsername(reqVO.getUsername());
        userReqVO.setNickname(reqVO.getNickname());
        userReqVO.setMobile(reqVO.getMobile());
        userReqVO.setPassword(reqVO.getPassword());
        return userReqVO;
    }

    private UserPageReqVO toUserPageReqVO(MerchantStaffPageReqVO pageReqVO) {
        UserPageReqVO userPageReqVO = new UserPageReqVO();
        userPageReqVO.setPageNo(pageReqVO.getPageNo());
        userPageReqVO.setPageSize(pageReqVO.getPageSize());
        userPageReqVO.setUsername(pageReqVO.getUsername());
        userPageReqVO.setMobile(pageReqVO.getMobile());
        userPageReqVO.setStatus(pageReqVO.getStatus());
        userPageReqVO.setCreateTime(pageReqVO.getCreateTime());
        return userPageReqVO;
    }

    @Schema(description = "管理后台 - 商户人员分页 Request VO")
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MerchantStaffPageReqVO extends PageParam {
        @Schema(description = "用户账号，模糊匹配", example = "zhangsan")
        private String username;
        @Schema(description = "手机号码，模糊匹配", example = "15601691300")
        private String mobile;
        @Schema(description = "展示状态，参见 CommonStatusEnum 枚举类", example = "1")
        private Integer status;
        @Schema(description = "创建时间")
        private LocalDateTime[] createTime;
    }

    @Schema(description = "管理后台 - 商户人员创建/修改 Request VO")
    @Data
    public static class MerchantStaffSaveReqVO {

        @Schema(description = "用户编号", example = "1024")
        private Long id;

        @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "yudao")
        @NotBlank(message = "用户账号不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "用户账号由 数字、字母 组成")
        @Size(min = 4, max = 30, message = "用户账号长度为 4-30 个字符")
        private String username;

        @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
        @Size(max = 30, message = "用户昵称长度不能超过30个字符")
        private String nickname;

        @Schema(description = "手机号码", example = "15601691300")
        @Mobile
        private String mobile;

        @Schema(description = "状态，参见 CommonStatusEnum 枚举类", example = "1")
        private Integer status;

        @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
        @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
        private String password;

        @AssertTrue(message = "密码不能为空")
        @JsonIgnore
        public boolean isPasswordValid() {
            return id != null || ObjectUtil.isAllNotEmpty(password);
        }
    }

    @Schema(description = "管理后台 - 商户人员 Response VO")
    @Data
    public static class MerchantStaffRespVO {
        @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long id;
        @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
        private String username;
        @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
        private String nickname;
        @Schema(description = "手机号码", example = "15601691300")
        private String mobile;
        @Schema(description = "状态，参见 CommonStatusEnum 枚举类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Integer status;
        @Schema(description = "创建时间")
        private LocalDateTime createTime;
    }

}
