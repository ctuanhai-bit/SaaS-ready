package cn.iocoder.yudao.module.system.convert.auth;

import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthConvertTest {

    @Test
    void convert_shouldExposeAccountTenantId() {
        AdminUserDO user = new AdminUserDO().setId(20L).setUsername("merchant_operator");
        user.setTenantId(9002L);

        AuthPermissionInfoRespVO result = AuthConvert.INSTANCE.convert(
                user, new ArrayList<>(), new ArrayList<>());

        assertEquals(9002L, result.getUser().getTenantId());
    }

}
