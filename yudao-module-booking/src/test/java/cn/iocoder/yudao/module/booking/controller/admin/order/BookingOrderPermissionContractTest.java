package cn.iocoder.yudao.module.booking.controller.admin.order;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookingOrderPermissionContractTest {

    @Test
    public void getOrder_shouldRequireCommunityBookingPermission() throws NoSuchMethodException {
        PreAuthorize preAuthorize = BookingOrderController.class
                .getDeclaredMethod("getOrder", Long.class)
                .getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("booking:order:query"));
    }
}
