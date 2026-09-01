package cn.iocoder.yudao.module.booking.controller.admin.inventory;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryRecordDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.resource.BookingRoomTypeDO;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryRecordMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.resource.BookingRoomTypeMapper;
import cn.iocoder.yudao.module.booking.enums.BookingInventoryActionTypeEnum;
import cn.iocoder.yudao.module.booking.service.stock.BookingStockService;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_CONTEXT_NOT_EXISTS;
import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.BOOKING_INVENTORY_BATCH_TOO_LARGE;
import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.BOOKING_ROOM_TYPE_NOT_EXISTS;

@Tag(name = "管理后台 - 房态库存")
@RestController
@RequestMapping("/booking/inventory")
@Validated
public class BookingInventoryController {

    @Resource
    private BookingInventoryMapper inventoryMapper;
    @Resource
    private BookingInventoryRecordMapper inventoryRecordMapper;
    @Resource
    private BookingRoomTypeMapper roomTypeMapper;
    @Resource
    private BookingStockService stockService;
    @Resource
    private MerchantContextService merchantContextService;

    @GetMapping("/calendar")
    @Operation(summary = "获得房态库存日历")
    @PreAuthorize("@ss.hasPermission('booking:inventory:query')")
    public CommonResult<List<InventoryCalendarRespVO>> getInventoryCalendar(
            @RequestParam(value = "roomTypeId", required = false) Long roomTypeId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long merchantId = currentMerchantId();
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? start.plusDays(30) : endDate;

        List<BookingRoomTypeDO> roomTypes = roomTypeMapper.selectList(new LambdaQueryWrapperX<BookingRoomTypeDO>()
                .eq(BookingRoomTypeDO::getMerchantId, merchantId)
                .eq(roomTypeId != null, BookingRoomTypeDO::getId, roomTypeId)
                .orderByAsc(BookingRoomTypeDO::getId));
        if (roomTypes.isEmpty()) {
            return success(Collections.emptyList());
        }

        List<Long> roomTypeIds = roomTypes.stream().map(BookingRoomTypeDO::getId).collect(Collectors.toList());
        Map<Long, BookingRoomTypeDO> roomTypeMap = roomTypes.stream()
                .collect(Collectors.toMap(BookingRoomTypeDO::getId, roomType -> roomType));
        List<BookingInventoryDO> inventories = inventoryMapper.selectList(new LambdaQueryWrapperX<BookingInventoryDO>()
                .eq(BookingInventoryDO::getMerchantId, merchantId)
                .in(BookingInventoryDO::getRoomTypeId, roomTypeIds)
                .ge(BookingInventoryDO::getBizDate, start)
                .le(BookingInventoryDO::getBizDate, end)
                .orderByAsc(BookingInventoryDO::getBizDate));

        return success(inventories.stream().map(inventory -> {
            BookingRoomTypeDO roomType = roomTypeMap.get(inventory.getRoomTypeId());
            int total = nvl(inventory.getTotalQuantity());
            int locked = nvl(inventory.getLockedQuantity());
            int sold = nvl(inventory.getSoldQuantity());
            return new InventoryCalendarRespVO(inventory.getId(), inventory.getTenantId(), inventory.getMerchantId(),
                    inventory.getRoomTypeId(), roomType == null ? "-" : roomType.getName(), inventory.getBizDate(),
                    total, Math.max(0, total - locked - sold), locked, inventory.getPrice(), roomType == null ? null : roomType.getStatus());
        }).collect(Collectors.toList()));
    }

    @PutMapping("/calendar")
    @Operation(summary = "调整房态库存")
    @PreAuthorize("@ss.hasPermission('booking:inventory:update')")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Boolean> updateInventoryCalendar(@RequestBody InventoryUpdateReqVO reqVO) {
        Long merchantId = currentMerchantId();
        List<Long> roomTypeIds = resolveRoomTypeIds(reqVO);
        List<LocalDate> dates = resolveDates(reqVO);
        if (roomTypeIds.isEmpty() || dates.isEmpty()) {
            return success(false);
        }
        if ((long) roomTypeIds.size() * dates.size() > 1000) {
            throw exception(BOOKING_INVENTORY_BATCH_TOO_LARGE);
        }
        List<BookingRoomTypeDO> roomTypes = roomTypeMapper.selectList(new LambdaQueryWrapperX<BookingRoomTypeDO>()
                .eq(BookingRoomTypeDO::getMerchantId, merchantId)
                .in(BookingRoomTypeDO::getId, roomTypeIds));
        if (roomTypes.size() != roomTypeIds.size()) {
            throw exception(BOOKING_ROOM_TYPE_NOT_EXISTS);
        }
        Map<Long, BookingRoomTypeDO> roomTypeMap = roomTypes.stream()
                .collect(Collectors.toMap(BookingRoomTypeDO::getId, roomType -> roomType));
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        for (Long roomTypeId : roomTypeIds) {
            for (LocalDate date : dates) {
                updateInventory(tenantId, merchantId, roomTypeId, date,
                        roomTypeMap.get(roomTypeId).getInitialPrice(), reqVO);
            }
        }
        return success(true);
    }

    private void updateInventory(Long tenantId, Long merchantId, Long roomTypeId, LocalDate date,
                                 Integer initialPrice, InventoryUpdateReqVO reqVO) {
        BookingInventoryDO inventory = inventoryMapper.selectByRoomTypeIdAndDateForUpdate(
                tenantId, merchantId, roomTypeId, date);
        if (inventory == null) {
            inventory = new BookingInventoryDO();
            inventory.setTenantId(tenantId);
            inventory.setMerchantId(merchantId);
            inventory.setRoomTypeId(roomTypeId);
            inventory.setBizDate(date);
            inventory.setLockedQuantity(0);
            inventory.setSoldQuantity(0);
            inventory.setTotalQuantity(Math.max(0, nvl(reqVO.getAvailable())));
            inventory.setPrice(resolveInitialPrice(reqVO.getPrice(), initialPrice));
            inventoryMapper.insert(inventory);
            inventoryRecordMapper.insert(buildManualAdjustRecord(inventory, "total=0,locked=0,sold=0,price=-",
                    snapshot(inventory), nvl(reqVO.getAvailable())));
        } else {
            int locked = nvl(inventory.getLockedQuantity());
            int sold = nvl(inventory.getSoldQuantity());
            int available = Math.max(0, nvl(reqVO.getAvailable()));
            String beforeSnapshot = snapshot(inventory);
            Integer nextTotal = available + locked + sold;
            Integer nextPrice = normalizePrice(reqVO.getPrice());
            inventoryMapper.updateById(new BookingInventoryDO()
                    .setId(inventory.getId())
                    .setTotalQuantity(nextTotal)
                    .setPrice(nextPrice));
            inventoryRecordMapper.insert(buildManualAdjustRecord(inventory, beforeSnapshot,
                    "total=" + nextTotal + ",locked=" + locked + ",sold=" + sold
                            + ",price=" + (nextPrice == null ? "-" : nextPrice),
                    available));
        }
    }

    private List<Long> resolveRoomTypeIds(InventoryUpdateReqVO reqVO) {
        if (reqVO == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> roomTypeIds = new LinkedHashSet<>();
        if (reqVO.getRoomTypeIds() != null) {
            roomTypeIds.addAll(reqVO.getRoomTypeIds().stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (roomTypeIds.isEmpty() && reqVO.getRoomTypeId() != null) {
            roomTypeIds.add(reqVO.getRoomTypeId());
        }
        return roomTypeIds.stream().collect(Collectors.toList());
    }

    private List<LocalDate> resolveDates(InventoryUpdateReqVO reqVO) {
        if (reqVO == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<LocalDate> dates = new LinkedHashSet<>();
        if (reqVO.getDates() != null) {
            dates.addAll(reqVO.getDates().stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (dates.isEmpty() && reqVO.getDate() != null) {
            dates.add(reqVO.getDate());
        }
        return dates.stream().collect(Collectors.toList());
    }

    @GetMapping("/change-logs")
    @Operation(summary = "获得房态库存变更记录")
    @PreAuthorize("@ss.hasPermission('booking:inventory:query')")
    public CommonResult<PageResult<InventoryChangeLogRespVO>> getInventoryChangeLogs(PageParam pageReqVO,
            @RequestParam(value = "roomTypeId", required = false) Long roomTypeId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long merchantId = currentMerchantId();
        PageResult<BookingInventoryRecordDO> page = inventoryRecordMapper.selectPage(pageReqVO,
                new LambdaQueryWrapperX<BookingInventoryRecordDO>()
                        .eq(BookingInventoryRecordDO::getMerchantId, merchantId)
                        .eqIfPresent(BookingInventoryRecordDO::getRoomTypeId, roomTypeId)
                        .geIfPresent(BookingInventoryRecordDO::getBizDate, startDate)
                        .leIfPresent(BookingInventoryRecordDO::getBizDate, endDate)
                        .orderByDesc(BookingInventoryRecordDO::getId));
        return success(new PageResult<>(page.getList().stream()
                .map(this::toChangeLogResp)
                .collect(Collectors.toList()), page.getTotal()));
    }

    @PostMapping("/lock")
    @Operation(summary = "手工锁房")
    @PreAuthorize("@ss.hasPermission('booking:inventory:update')")
    public CommonResult<Boolean> lockInventory(@Valid @RequestBody InventoryActionReqVO reqVO) {
        Long merchantId = currentMerchantId();
        stockService.manualLock(TenantContextHolder.getRequiredTenantId(), merchantId, reqVO.getRoomTypeId(),
                reqVO.getDate(), reqVO.getQuantity(), getLoginUserId());
        return success(true);
    }

    @PostMapping("/unlock")
    @Operation(summary = "手工解锁")
    @PreAuthorize("@ss.hasPermission('booking:inventory:update')")
    public CommonResult<Boolean> unlockInventory(@Valid @RequestBody InventoryActionReqVO reqVO) {
        Long merchantId = currentMerchantId();
        stockService.manualUnlock(TenantContextHolder.getRequiredTenantId(), merchantId, reqVO.getRoomTypeId(),
                reqVO.getDate(), reqVO.getQuantity(), getLoginUserId());
        return success(true);
    }

    private Long currentMerchantId() {
        Long merchantId = merchantContextService.getCurrentMerchantId();
        if (merchantId == null) {
            throw exception(MERCHANT_CONTEXT_NOT_EXISTS);
        }
        return merchantId;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer normalizePrice(Integer price) {
        return price == null || price <= 0 ? null : price;
    }

    private Integer resolveInitialPrice(Integer price, Integer initialPrice) {
        Integer normalizedPrice = normalizePrice(price);
        return normalizedPrice != null ? normalizedPrice : normalizePrice(initialPrice);
    }

    private BookingInventoryRecordDO buildManualAdjustRecord(BookingInventoryDO inventory, String beforeSnapshot,
                                                             String afterSnapshot, Integer quantity) {
        BookingInventoryRecordDO record = new BookingInventoryRecordDO()
                .setMerchantId(inventory.getMerchantId())
                .setRoomTypeId(inventory.getRoomTypeId())
                .setInventoryId(inventory.getId())
                .setBizDate(inventory.getBizDate())
                .setQuantity(quantity)
                .setActionType(BookingInventoryActionTypeEnum.MANUAL_ADJUST.getType())
                .setBizType("MANUAL_STOCK")
                .setBeforeSnapshot(beforeSnapshot)
                .setAfterSnapshot(afterSnapshot)
                .setOperatorType("ADMIN_USER")
                .setOperatorId(getLoginUserId());
        record.setTenantId(inventory.getTenantId());
        return record;
    }

    private InventoryChangeLogRespVO toChangeLogResp(BookingInventoryRecordDO record) {
        return new InventoryChangeLogRespVO(formatCreateTime(record), resolveOperator(record),
                translateActionType(record.getActionType()),
                record.getBizDate() + "，数量 " + nvl(record.getQuantity()) + "，"
                        + record.getBeforeSnapshot() + " -> " + record.getAfterSnapshot());
    }

    private String formatCreateTime(BookingInventoryRecordDO record) {
        return record.getCreateTime() == null ? "-" : record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String resolveOperator(BookingInventoryRecordDO record) {
        return record.getOperatorId() == null ? record.getOperatorType() : record.getOperatorType() + " " + record.getOperatorId();
    }

    private String translateActionType(String actionType) {
        for (BookingInventoryActionTypeEnum value : BookingInventoryActionTypeEnum.values()) {
            if (value.getType().equals(actionType)) {
                return value.getName();
            }
        }
        return actionType;
    }

    private String snapshot(BookingInventoryDO inventory) {
        return "total=" + nvl(inventory.getTotalQuantity())
                + ",locked=" + nvl(inventory.getLockedQuantity())
                + ",sold=" + nvl(inventory.getSoldQuantity())
                + ",price=" + (inventory.getPrice() == null ? "-" : inventory.getPrice());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryCalendarRespVO {
        private Long id;
        private Long tenantId;
        private Long merchantId;
        private Long roomTypeId;
        private String roomType;
        private LocalDate date;
        private Integer total;
        private Integer available;
        private Integer locked;
        private Integer price;
        private Integer status;
    }

    @Data
    public static class InventoryUpdateReqVO {
        private Long roomTypeId;
        private List<Long> roomTypeIds;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private List<LocalDate> dates;
        private Integer available;
        private Integer price;
        private Integer status;
    }

    @Data
    public static class InventoryActionReqVO {
        @NotNull
        private Long roomTypeId;
        @NotNull
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        @NotNull
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryChangeLogRespVO {
        private String createTime;
        private String operator;
        private String type;
        private String remark;
    }
}
