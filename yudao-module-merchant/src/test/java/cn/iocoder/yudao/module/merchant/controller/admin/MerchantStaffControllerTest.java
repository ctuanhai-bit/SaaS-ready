package cn.iocoder.yudao.module.merchant.controller.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.merchant.service.MerchantService;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserUpdateStatusReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_CONTEXT_NOT_EXISTS;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MerchantStaffControllerTest extends BaseMockitoUnitTest {

    private static final Long MERCHANT_ID = 100L;
    private static final Long TENANT_ID = 200L;
    private static final Long USER_ID = 300L;

    @InjectMocks
    private MerchantStaffController merchantStaffController;

    @Mock
    private MerchantService merchantService;
    @Mock
    private AdminUserService userService;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testGetStaffPage_whenNoMerchantContext_shouldReject() {
        when(merchantService.getCurrentMerchantId()).thenReturn(null);
        TenantContextHolder.setTenantId(TENANT_ID);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> merchantStaffController.getStaffPage(new MerchantStaffController.MerchantStaffPageReqVO()));

        assertEquals(MERCHANT_CONTEXT_NOT_EXISTS.getCode(), exception.getCode());
        verify(userService, never()).getUserPage(any());
    }

    @Test
    public void testGetStaffPage_shouldUseCurrentTenantAndNotAcceptMerchantId() {
        mockCurrentMerchantTenant();
        MerchantStaffController.MerchantStaffPageReqVO reqVO = new MerchantStaffController.MerchantStaffPageReqVO();
        reqVO.setUsername("staff");
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(userService.getUserPage(any())).thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        merchantStaffController.getStaffPage(reqVO);

        verify(merchantService).validateMerchantTenant(MERCHANT_ID, TENANT_ID);
        ArgumentCaptor<cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO.class);
        verify(userService).getUserPage(captor.capture());
        assertEquals("staff", captor.getValue().getUsername());
        assertEquals(1, captor.getValue().getPageNo());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    public void testCreateStaff_shouldPassOnlyWhitelistedFieldsToAdminUserService() {
        mockCurrentMerchantTenant();
        MerchantStaffController.MerchantStaffSaveReqVO reqVO = new MerchantStaffController.MerchantStaffSaveReqVO();
        reqVO.setId(USER_ID);
        reqVO.setUsername("staff001");
        reqVO.setNickname("staff");
        reqVO.setMobile("15601691300");
        reqVO.setStatus(1);
        reqVO.setPassword("123456");
        when(userService.createUser(any(UserSaveReqVO.class))).thenReturn(USER_ID);

        merchantStaffController.createStaff(reqVO);

        ArgumentCaptor<UserSaveReqVO> captor = ArgumentCaptor.forClass(UserSaveReqVO.class);
        verify(userService).createUser(captor.capture());
        UserSaveReqVO userReqVO = captor.getValue();
        assertNull(userReqVO.getId());
        assertEquals("staff001", userReqVO.getUsername());
        assertEquals("staff", userReqVO.getNickname());
        assertEquals("15601691300", userReqVO.getMobile());
        assertEquals("123456", userReqVO.getPassword());
        assertNull(userReqVO.getDeptId());
        assertNull(userReqVO.getPostIds());
        assertNull(userReqVO.getAvatar());
        assertNull(userReqVO.getEmail());
        assertNull(userReqVO.getRemark());
        assertNull(userReqVO.getSex());
    }

    @Test
    public void testUpdateStaff_shouldValidateCurrentTenantUserAndPassOnlyWhitelistedFields() {
        mockCurrentMerchantTenant();
        when(userService.getUser(USER_ID)).thenReturn(new AdminUserDO().setId(USER_ID));
        MerchantStaffController.MerchantStaffSaveReqVO reqVO = new MerchantStaffController.MerchantStaffSaveReqVO();
        reqVO.setId(USER_ID);
        reqVO.setUsername("staff002");
        reqVO.setNickname("staff-new");
        reqVO.setMobile("15601691301");
        reqVO.setStatus(0);
        reqVO.setPassword("654321");

        merchantStaffController.updateStaff(reqVO);

        ArgumentCaptor<UserSaveReqVO> captor = ArgumentCaptor.forClass(UserSaveReqVO.class);
        verify(userService).getUser(USER_ID);
        verify(userService).updateUser(captor.capture());
        UserSaveReqVO userReqVO = captor.getValue();
        assertEquals(USER_ID, userReqVO.getId());
        assertEquals("staff002", userReqVO.getUsername());
        assertEquals("staff-new", userReqVO.getNickname());
        assertEquals("15601691301", userReqVO.getMobile());
        assertEquals("654321", userReqVO.getPassword());
        assertNull(userReqVO.getDeptId());
        assertNull(userReqVO.getPostIds());
        assertNull(userReqVO.getAvatar());
        assertNull(userReqVO.getEmail());
        assertNull(userReqVO.getRemark());
        assertNull(userReqVO.getSex());
    }

    @Test
    public void testMerchantStaffSaveReqVO_shouldNotExposeSystemFields() {
        Set<String> fieldNames = Arrays.stream(MerchantStaffController.MerchantStaffSaveReqVO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertEquals(MerchantStaffController.class, MerchantStaffController.MerchantStaffSaveReqVO.class.getDeclaringClass());
        assertFalse(UserSaveReqVO.class.isAssignableFrom(MerchantStaffController.MerchantStaffSaveReqVO.class));
        assertFalse(fieldNames.contains("deptId"));
        assertFalse(fieldNames.contains("postIds"));
        assertFalse(fieldNames.contains("avatar"));
        assertFalse(fieldNames.contains("email"));
        assertFalse(fieldNames.contains("remark"));
        assertFalse(fieldNames.contains("sex"));
    }

    @Test
    public void testUpdateStaffStatus_whenUserNotExists_shouldReject() {
        mockCurrentMerchantTenant();
        when(userService.getUser(USER_ID)).thenReturn(null);
        UserUpdateStatusReqVO reqVO = new UserUpdateStatusReqVO();
        reqVO.setId(USER_ID);
        reqVO.setStatus(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> merchantStaffController.updateStaffStatus(reqVO));

        assertEquals(USER_NOT_EXISTS.getCode(), exception.getCode());
        verify(userService, never()).updateUserStatus(any(), any());
    }

    @Test
    public void testUpdateStaffStatus_shouldValidateCurrentTenantUserBeforeUpdate() {
        mockCurrentMerchantTenant();
        when(userService.getUser(USER_ID)).thenReturn(new AdminUserDO().setId(USER_ID));
        UserUpdateStatusReqVO reqVO = new UserUpdateStatusReqVO();
        reqVO.setId(USER_ID);
        reqVO.setStatus(1);

        merchantStaffController.updateStaffStatus(reqVO);

        verify(merchantService).validateMerchantTenant(MERCHANT_ID, TENANT_ID);
        verify(userService).updateUserStatus(USER_ID, 1);
    }

    private void mockCurrentMerchantTenant() {
        when(merchantService.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

}
