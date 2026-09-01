SET NAMES utf8mb4;

INSERT INTO system_tenant_package
    (id, name, status, remark, menu_ids, creator, updater, deleted)
VALUES
    (1, 'Community', 0, 'Hotel PMS Community default package',
     '[1,2,100,101,102,108,500,1243,7000,7001,7002,7003,7004,7005,7006,7011,7021,7022,7023,7031,7041,7042,7051,7061,7062]',
     'bootstrap', 'bootstrap', b'0');

INSERT INTO system_tenant
    (id, name, contact_user_id, contact_name, contact_mobile, status, websites, package_id,
     expire_time, account_count, creator, updater, deleted)
VALUES
    (1, 'Hotel PMS Community', 1, 'Demo Admin', '18800000000', 0, '["localhost"]', 1,
     '2099-12-31 23:59:59', 20, 'bootstrap', 'bootstrap', b'0');

INSERT INTO system_dept
    (id, name, parent_id, sort, leader_user_id, phone, email, status, creator, updater, deleted, tenant_id)
VALUES
    (1, 'Community Demo Hotel', 0, 1, 1, '18800000000', 'demo@example.invalid', 0,
     'bootstrap', 'bootstrap', b'0', 1);

-- The documented local password is admin123. Change it immediately outside local development.
INSERT INTO system_users
    (id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar, status,
     login_ip, login_date, creator, updater, deleted, tenant_id)
VALUES
    (1, 'admin', '$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG',
     'Community Admin', 'Local development account', 1, '[]', 'demo@example.invalid', '18800000000',
     0, '', 0, '', NULL, 'bootstrap', 'bootstrap', b'0', 1);

INSERT INTO system_role
    (id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
     creator, updater, deleted, tenant_id)
VALUES
    (1, 'Super Administrator', 'super_admin', 1, 1, '', 0, 1, 'Community bootstrap administrator',
     'bootstrap', 'bootstrap', b'0', 1);

INSERT INTO system_user_role
    (id, user_id, role_id, creator, updater, deleted, tenant_id)
VALUES
    (1, 1, 1, 'bootstrap', 'bootstrap', b'0', 1);

INSERT INTO system_oauth2_client
    (id, client_id, secret, name, logo, description, status, access_token_validity_seconds,
     refresh_token_validity_seconds, redirect_uris, authorized_grant_types, scopes,
     auto_approve_scopes, authorities, resource_ids, additional_information,
     creator, updater, deleted)
VALUES
    (1, 'default', 'admin123', 'Hotel PMS Community', '', 'Local management console', 0, 1800,
     2592000, '["http://localhost:3000"]', '["password","refresh_token"]', '["user.read"]',
     '[]', '[]', '[]', '{}', 'bootstrap', 'bootstrap', b'0');

INSERT INTO system_menu
    (id, name, permission, type, sort, parent_id, path, icon, component, component_name,
     status, visible, keep_alive, always_show, creator, updater, deleted)
VALUES
    (1, 'System', '', 1, 90, 0, '/system', 'ep:tools', NULL, NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (2, 'Infrastructure', '', 1, 91, 0, '/infra', 'ep:monitor', NULL, NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (100, 'Users', 'system:user:list', 2, 1, 1, 'user', 'ep:user', 'system/user/index', 'SystemUser', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (101, 'Roles', '', 2, 2, 1, 'role', 'ep:user-filled', 'system/role/index', 'SystemRole', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (102, 'Menus', '', 2, 3, 1, 'menu', 'ep:menu', 'system/menu/index', 'SystemMenu', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (108, 'Audit', '', 1, 4, 1, 'log', 'ep:document', NULL, NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (500, 'Operation Logs', '', 2, 1, 108, 'operate-log', 'ep:position', 'system/operatelog/index', 'SystemOperateLog', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (1243, 'Files', '', 2, 1, 2, 'file', 'ep:files', 'infra/file/index', 'InfraFile', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7000, 'Hotel Operations', '', 1, 10, 0, '/hotel', 'ep:house', NULL, NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7001, 'Hotel Profile', 'merchant:profile:query', 2, 1, 7000, 'profile', 'ep:location', 'saas/merchant/profile/index', 'CommunityHotelProfile', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7002, 'Room Types', 'booking:room-type:query', 2, 2, 7000, 'room-types', 'ep:calendar', 'saas/merchant/room/index', 'CommunityRoomTypes', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7003, 'Inventory', 'booking:inventory:query', 2, 3, 7000, 'inventory', 'ep:grid', 'saas/merchant/inventory/index', 'CommunityInventory', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7004, 'Orders', 'booking:order:query', 2, 4, 7000, 'orders', 'ep:list', 'saas/merchant/order/index', 'CommunityOrders', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7005, 'Front Desk', 'booking:order:query', 2, 5, 7000, 'front-desk', 'ep:service', 'saas/merchant/front-desk/index', 'CommunityFrontDesk', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7006, 'Staff', 'merchant:staff:query', 2, 6, 7000, 'staff', 'ep:user-filled', 'saas/merchant/staff/index', 'CommunityHotelStaff', 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7011, 'Update Hotel Profile', 'merchant:profile:update', 3, 1, 7001, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7021, 'Create Room Type', 'booking:room-type:create', 3, 1, 7002, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7022, 'Update Room Type', 'booking:room-type:update', 3, 2, 7002, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7023, 'Delete Room Type', 'booking:room-type:delete', 3, 3, 7002, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7031, 'Update Inventory', 'booking:inventory:update', 3, 1, 7003, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7041, 'Create Order', 'booking:order:create', 3, 1, 7004, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7042, 'Update Order', 'booking:order:update', 3, 2, 7004, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7051, 'Operate Front Desk', 'booking:order:update', 3, 1, 7005, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7061, 'Create Staff', 'merchant:staff:create', 3, 1, 7006, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0'),
    (7062, 'Update Staff', 'merchant:staff:update', 3, 2, 7006, '', '', '', NULL, 0, b'1', b'1', b'1', 'bootstrap', 'bootstrap', b'0');

INSERT INTO infra_file_config
    (id, name, storage, remark, master, config, creator, updater, deleted)
VALUES
    (1, 'Community database storage', 1, 'Portable local-development default', b'1',
     '{"@class":"cn.iocoder.yudao.module.infra.framework.file.core.client.db.DBFileClientConfig","domain":"http://localhost:48080"}',
     'bootstrap', 'bootstrap', b'0');
