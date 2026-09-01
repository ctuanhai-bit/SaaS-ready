SET NAMES utf8mb4;

CREATE TABLE biz_merchant (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'Hotel identifier',
    tenant_id bigint NOT NULL COMMENT 'Tenant identifier',
    name varchar(64) NOT NULL COMMENT 'Hotel name',
    business_type varchar(32) NOT NULL DEFAULT 'hotel',
    logo_url varchar(512) DEFAULT NULL,
    cover_url varchar(512) DEFAULT NULL,
    image_urls text DEFAULT NULL,
    video_url varchar(512) DEFAULT NULL,
    video_cover_url varchar(512) DEFAULT NULL,
    theme_color varchar(32) DEFAULT NULL,
    license_image_url varchar(512) DEFAULT NULL,
    qualification_image_url varchar(512) DEFAULT NULL,
    contact_name varchar(64) DEFAULT NULL,
    contact_mobile varchar(32) DEFAULT NULL,
    address varchar(255) DEFAULT NULL,
    longitude decimal(10,6) DEFAULT NULL,
    latitude decimal(9,6) DEFAULT NULL,
    description varchar(500) DEFAULT NULL,
    status tinyint NOT NULL DEFAULT 1,
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_merchant_tenant (tenant_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hotel profile';

CREATE TABLE booking_room_type (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    name varchar(100) NOT NULL,
    max_occupancy int NOT NULL DEFAULT 1,
    area_sqm decimal(6,2) DEFAULT NULL,
    area_sqm_min decimal(6,2) DEFAULT NULL,
    area_sqm_max decimal(6,2) DEFAULT NULL,
    breakfast_included bit(1) NOT NULL DEFAULT b'0',
    initial_price int NOT NULL DEFAULT 0 COMMENT 'Price in cents',
    status tinyint NOT NULL DEFAULT 0,
    cover_url varchar(512) DEFAULT NULL,
    image_urls text DEFAULT NULL,
    facility_codes text DEFAULT NULL,
    auto_confirm_enabled bit(1) NOT NULL DEFAULT b'0',
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_room_type_owner (tenant_id, merchant_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Room type';

CREATE TABLE booking_room (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    room_type_id bigint NOT NULL,
    room_no varchar(32) NOT NULL,
    status tinyint NOT NULL DEFAULT 0,
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_booking_room_no (tenant_id, merchant_id, room_no),
    KEY idx_booking_room_type (room_type_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Physical room';

CREATE TABLE booking_inventory (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    room_type_id bigint NOT NULL,
    biz_date date NOT NULL,
    total_quantity int NOT NULL DEFAULT 0,
    locked_quantity int NOT NULL DEFAULT 0,
    sold_quantity int NOT NULL DEFAULT 0,
    price int DEFAULT NULL COMMENT 'Night price in cents',
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_booking_inventory_date (tenant_id, merchant_id, room_type_id, biz_date),
    KEY idx_booking_inventory_calendar (merchant_id, biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Daily room inventory and price';

CREATE TABLE booking_inventory_record (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    room_type_id bigint NOT NULL,
    inventory_id bigint NOT NULL,
    biz_date date NOT NULL,
    quantity int NOT NULL,
    action_type varchar(32) NOT NULL,
    biz_type varchar(32) DEFAULT NULL,
    biz_id bigint DEFAULT NULL,
    biz_no varchar(64) DEFAULT NULL,
    before_snapshot varchar(500) DEFAULT NULL,
    after_snapshot varchar(500) DEFAULT NULL,
    operator_type varchar(32) DEFAULT NULL,
    operator_id bigint DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_inventory_record_inventory (inventory_id, create_time),
    KEY idx_inventory_record_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inventory change record';

CREATE TABLE booking_order (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    order_no varchar(64) NOT NULL,
    merchant_id bigint NOT NULL,
    room_type_id bigint NOT NULL,
    inventory_id bigint DEFAULT NULL COMMENT 'Compatibility pointer to the first night',
    check_in_date date NOT NULL,
    check_out_date date NOT NULL,
    room_quantity int NOT NULL DEFAULT 1,
    room_no varchar(32) DEFAULT NULL,
    guest_name varchar(64) NOT NULL,
    guest_mobile varchar(32) NOT NULL,
    client_submit_token varchar(64) DEFAULT NULL,
    status tinyint NOT NULL,
    pay_status tinyint NOT NULL DEFAULT 0 COMMENT 'Manual payment state; no third-party payment implementation',
    pay_order_id bigint DEFAULT NULL COMMENT 'External payment reference reserved for adapters',
    expire_time datetime DEFAULT NULL,
    auto_confirm_snapshot bit(1) NOT NULL DEFAULT b'0',
    refund_status tinyint DEFAULT 0 COMMENT 'Manual refund state reserved for adapters',
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_booking_order_no (order_no),
    UNIQUE KEY uk_booking_order_submit_token (tenant_id, merchant_id, client_submit_token),
    KEY idx_booking_order_front_desk (tenant_id, merchant_id, status, check_in_date, check_out_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hotel booking order';

CREATE TABLE booking_order_lock (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    order_id bigint NOT NULL,
    inventory_id bigint NOT NULL,
    merchant_id bigint NOT NULL,
    room_type_id bigint NOT NULL,
    biz_date date NOT NULL,
    quantity int NOT NULL,
    price int DEFAULT NULL COMMENT 'Night price in cents',
    lock_status tinyint NOT NULL,
    creator varchar(64) DEFAULT '',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_booking_order_lock_night (order_id, inventory_id),
    KEY idx_booking_order_lock_owner (tenant_id, merchant_id, room_type_id, biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Per-night inventory lock';

INSERT INTO biz_merchant
    (id, tenant_id, name, business_type, contact_name, contact_mobile, address, description,
     status, creator, updater, deleted)
VALUES
    (1, 1, 'Community Demo Hotel', 'hotel', 'Demo Admin', '18800000000',
     'No. 1 Community Road', 'A fictional hotel created for local development.',
     1, 'bootstrap', 'bootstrap', b'0');

INSERT INTO booking_room_type
    (id, tenant_id, merchant_id, name, max_occupancy, area_sqm_min, area_sqm_max,
     breakfast_included, initial_price, status, facility_codes, auto_confirm_enabled,
     creator, updater, deleted)
VALUES
    (1, 1, 1, 'Community King Room', 2, 24.00, 28.00, b'0', 19900, 0,
     '["WIFI","AIR_CONDITIONING"]', b'0', 'bootstrap', 'bootstrap', b'0');

INSERT INTO booking_room
    (id, tenant_id, merchant_id, room_type_id, room_no, status, creator, updater, deleted)
VALUES
    (1, 1, 1, 1, '101', 0, 'bootstrap', 'bootstrap', b'0');
