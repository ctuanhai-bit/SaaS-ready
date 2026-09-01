package cn.iocoder.yudao.module.infra.controller.admin.config;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.security.PermitAll;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ConfigController configController;

    @Mock
    private ConfigService configService;

    @Test
    void testGetPlatformBrand_shouldOnlyReadFixedPublicBrandKey() {
        ConfigDO config = new ConfigDO()
                .setVisible(true)
                .setValue("{\"name\":\"OpenHotel\"}");
        when(configService.getConfigByKey("saas.platform.brand")).thenReturn(config);

        assertEquals(config.getValue(), configController.getPlatformBrand().getData());
        verify(configService).getConfigByKey("saas.platform.brand");
    }

    @Test
    void testGetPlatformBrand_shouldAllowAnonymousAndIgnoreTenant() throws NoSuchMethodException {
        Method method = ConfigController.class.getMethod("getPlatformBrand");

        assertNotNull(AnnotationUtils.findAnnotation(method, PermitAll.class));
        assertNotNull(AnnotationUtils.findAnnotation(method, TenantIgnore.class));
    }
}
