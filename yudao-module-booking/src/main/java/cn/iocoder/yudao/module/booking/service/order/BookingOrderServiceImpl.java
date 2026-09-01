package cn.iocoder.yudao.module.booking.service.order;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderCreateReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.order.vo.BookingOrderPageReqVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryRecordDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.order.BookingOrderLockDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.resource.BookingRoomTypeDO;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryRecordMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.order.BookingOrderLockMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.order.BookingOrderMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.resource.BookingRoomTypeMapper;
import cn.iocoder.yudao.module.booking.enums.BookingInventoryActionTypeEnum;
import cn.iocoder.yudao.module.booking.enums.BookingOrderLockStatusEnum;
import cn.iocoder.yudao.module.booking.enums.BookingOrderStatusEnum;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.*;

@Service
@Validated
public class BookingOrderServiceImpl implements BookingOrderService {

    private static final Integer REFUND_STATUS_PROCESSING = 1;

    @Resource
    private BookingOrderMapper orderMapper;
    @Resource
    private BookingOrderLockMapper orderLockMapper;
    @Resource
    private BookingInventoryMapper inventoryMapper;
    @Resource
    private BookingInventoryRecordMapper inventoryRecordMapper;
    @Resource
    private BookingRoomTypeMapper roomTypeMapper;
    @Resource
    private MerchantContextService merchantContextService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(BookingOrderCreateReqVO createReqVO) {
        validateCreateReq(createReqVO);
        validateMerchantOwnerIfPresent(createReqVO.getMerchantId());
        merchantContextService.validateMerchantTenant(createReqVO.getMerchantId(), createReqVO.getTenantId());

        if (StrUtil.isBlank(createReqVO.getClientSubmitToken())) {
            throw exception(BOOKING_ORDER_DUPLICATE_SUBMIT);
        }
        BookingOrderDO existingOrder = orderMapper.selectByClientSubmitToken(createReqVO.getTenantId(), createReqVO.getMerchantId(),
                createReqVO.getClientSubmitToken());
        if (existingOrder != null) {
            return existingOrder.getId();
        }

        List<BookingInventoryDO> inventories = inventoryMapper.selectListByRoomTypeIdAndDateRangeForUpdate(createReqVO.getTenantId(),
                createReqVO.getMerchantId(), createReqVO.getRoomTypeId(), createReqVO.getCheckInDate(), createReqVO.getCheckOutDate());
        inventories.sort(Comparator.comparing(BookingInventoryDO::getBizDate));
        validateContinuousInventories(inventories, createReqVO.getTenantId(), createReqVO.getMerchantId(), createReqVO.getCheckInDate(),
                createReqVO.getCheckOutDate(), createReqVO.getRoomQuantity());

        for (BookingInventoryDO inventory : inventories) {
            inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId())
                    .setLockedQuantity(nullToZero(inventory.getLockedQuantity()) + createReqVO.getRoomQuantity()));
        }

        BookingOrderDO order = new BookingOrderDO();
        order.setTenantId(createReqVO.getTenantId());
        order.setOrderNo(generateOrderNo());
        order.setMerchantId(createReqVO.getMerchantId());
        order.setRoomTypeId(createReqVO.getRoomTypeId());
        order.setInventoryId(inventories.get(0).getId());
        order.setCheckInDate(createReqVO.getCheckInDate());
        order.setCheckOutDate(createReqVO.getCheckOutDate());
        order.setRoomQuantity(createReqVO.getRoomQuantity());
        order.setGuestName(createReqVO.getGuestName());
        order.setGuestMobile(createReqVO.getGuestMobile());
        order.setClientSubmitToken(createReqVO.getClientSubmitToken());
        order.setStatus(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
        order.setPayStatus(0);
        order.setExpireTime(LocalDateTime.now().plusMinutes(15));
        order.setAutoConfirmSnapshot(resolveAutoConfirmSnapshot(createReqVO));
        order.setRefundStatus(0);
        orderMapper.insert(order);
        orderLockMapper.insertBatch(inventories.stream()
                .map(inventory -> buildOrderLock(order, inventory))
                .collect(Collectors.toList()));
        inventoryRecordMapper.insertBatch(buildLockRecords(order, inventories));
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaySuccess(Long id, Long payOrderId) {
        BookingOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        applyPaySuccess(order, payOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaidManually(Long id, Long merchantId) {
        applyPaySuccess(validateOrderOwnerForUpdate(id, merchantId), null);
    }

    private void applyPaySuccess(BookingOrderDO order, Long payOrderId) {
        if (Integer.valueOf(10).equals(order.getPayStatus())) {
            return;
        }
        validateStatus(order, BookingOrderStatusEnum.PENDING_PAYMENT);
        Integer nextStatus = Boolean.TRUE.equals(order.getAutoConfirmSnapshot())
                ? BookingOrderStatusEnum.CONFIRMED.getStatus()
                : BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus();
        if (Boolean.TRUE.equals(order.getAutoConfirmSnapshot())) {
            moveLockedInventoryToSold(order);
        }
        orderMapper.updateById(new BookingOrderDO().setId(order.getId())
                .setPayOrderId(payOrderId)
                .setPayStatus(10)
                .setStatus(nextStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int releaseExpiredPendingPaymentOrders(LocalDateTime now) {
        List<BookingOrderDO> orders = orderMapper.selectListExpiredPendingPayment(now);
        int count = 0;
        for (BookingOrderDO order : orders) {
            if (!BookingOrderStatusEnum.PENDING_PAYMENT.getStatus().equals(order.getStatus())) {
                continue;
            }
            releaseLockedInventory(order, true, BookingInventoryActionTypeEnum.AUTO_RELEASE, "SYSTEM_JOB");
            orderMapper.updateById(new BookingOrderDO().setId(order.getId())
                    .setStatus(BookingOrderStatusEnum.CLOSED_TIMEOUT.getStatus()));
            count++;
        }
        return count;
    }

    @Override
    public void confirmOrder(Long id, Long merchantId) {
        BookingOrderDO order = validateOrderOwner(id, merchantId);
        if (!BookingOrderStatusEnum.PENDING_CONFIRM.getStatus().equals(order.getStatus())
                && !BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_INVALID);
        }
        if (BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus().equals(order.getStatus())) {
            moveLockedInventoryToSold(order);
        }
        orderMapper.updateById(new BookingOrderDO().setId(id).setStatus(BookingOrderStatusEnum.CONFIRMED.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, Long merchantId) {
        cancelOrder(id, merchantId, BookingInventoryActionTypeEnum.MERCHANT_REJECT_RELEASE, "ADMIN_USER");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderByUser(Long id, Long merchantId) {
        cancelOrder(id, merchantId, BookingInventoryActionTypeEnum.USER_CANCEL_RELEASE, "APP_USER");
    }

    private void cancelOrder(Long id, Long merchantId, BookingInventoryActionTypeEnum actionType, String operatorType) {
        BookingOrderDO order = validateOrderOwnerForUpdate(id, merchantId);
        if (isCancellationTerminal(order.getStatus())) {
            return;
        }
        if (!BookingOrderStatusEnum.PENDING_PAYMENT.getStatus().equals(order.getStatus())
                && !BookingOrderStatusEnum.PENDING_CONFIRM.getStatus().equals(order.getStatus())
                && !BookingOrderStatusEnum.PAID_WAIT_CONFIRM.getStatus().equals(order.getStatus())
                && !BookingOrderStatusEnum.CONFIRMED.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_INVALID);
        }
        if (BookingOrderStatusEnum.CONFIRMED.getStatus().equals(order.getStatus())) {
            releaseConfirmedInventory(order, true, actionType, operatorType);
        } else {
            releaseLockedInventory(order, true, actionType, operatorType);
        }
        boolean paid = Integer.valueOf(10).equals(order.getPayStatus());
        orderMapper.updateById(new BookingOrderDO().setId(id)
                .setStatus(paid ? BookingOrderStatusEnum.REFUND_APPLYING.getStatus()
                        : BookingOrderStatusEnum.CLOSED_CANCELLED.getStatus())
                .setRefundStatus(paid ? Integer.valueOf(0) : null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveRefund(Long id, Long merchantId) {
        BookingOrderDO order = validateOrderOwnerForUpdate(id, merchantId);
        if (BookingOrderStatusEnum.REFUNDED.getStatus().equals(order.getStatus())
                || REFUND_STATUS_PROCESSING.equals(order.getRefundStatus())) {
            return false;
        }
        if (!BookingOrderStatusEnum.REFUND_APPLYING.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_INVALID);
        }
        orderMapper.updateById(new BookingOrderDO().setId(id).setRefundStatus(REFUND_STATUS_PROCESSING));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRefunded(Long id) {
        BookingOrderDO order = orderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        applyRefunded(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRefundedManually(Long id, Long merchantId) {
        applyRefunded(validateOrderOwnerForUpdate(id, merchantId));
    }

    private void applyRefunded(BookingOrderDO order) {
        if (BookingOrderStatusEnum.REFUNDED.getStatus().equals(order.getStatus())) {
            return;
        }
        if (!BookingOrderStatusEnum.REFUND_APPLYING.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_INVALID);
        }
        orderMapper.updateById(new BookingOrderDO().setId(order.getId())
                .setStatus(BookingOrderStatusEnum.REFUNDED.getStatus())
                .setPayStatus(20)
                .setRefundStatus(10));
    }

    @Override
    public void checkInOrder(Long id, Long merchantId) {
        BookingOrderDO order = validateOrderOwner(id, merchantId);
        validateStatus(order, BookingOrderStatusEnum.CONFIRMED);
        orderMapper.updateById(new BookingOrderDO().setId(id).setStatus(BookingOrderStatusEnum.CHECKED_IN.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void noShowOrder(Long id, Long merchantId) {
        BookingOrderDO order = validateOrderOwner(id, merchantId);
        validateStatus(order, BookingOrderStatusEnum.CONFIRMED);
        releaseLockedInventory(order);
        orderMapper.updateById(new BookingOrderDO().setId(id).setStatus(BookingOrderStatusEnum.NO_SHOW.getStatus()));
    }

    @Override
    public void assignRoom(Long id, Long merchantId, String roomNo) {
        BookingOrderDO order = validateOrderOwner(id, merchantId);
        if (!BookingOrderStatusEnum.CONFIRMED.getStatus().equals(order.getStatus())
                && !BookingOrderStatusEnum.CHECKED_IN.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_INVALID);
        }
        orderMapper.updateById(new BookingOrderDO().setId(id).setRoomNo(roomNo.trim()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long id, Long merchantId) {
        BookingOrderDO order = validateOrderOwner(id, merchantId);
        if (!BookingOrderStatusEnum.CONFIRMED.getStatus().equals(order.getStatus())
                && !BookingOrderStatusEnum.CHECKED_IN.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_INVALID);
        }
        List<BookingOrderLockDO> locks = orderLockMapper.selectListByOrderId(order.getId());
        if (locks.isEmpty()) {
            BookingInventoryDO inventory = selectOwnedInventoryForUpdate(order, order.getInventoryId());
            int roomQuantity = nullToZero(order.getRoomQuantity());
            inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId())
                    .setLockedQuantity(Math.max(0, nullToZero(inventory.getLockedQuantity()) - roomQuantity))
                    .setSoldQuantity(nullToZero(inventory.getSoldQuantity()) + roomQuantity));
        } else {
            for (BookingOrderLockDO lock : locks) {
                validateOrderLockOwner(order, lock);
                BookingInventoryDO inventory = selectOwnedInventoryForUpdate(order, lock.getInventoryId());
                int roomQuantity = nullToZero(lock.getQuantity());
                inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId())
                        .setLockedQuantity(Math.max(0, nullToZero(inventory.getLockedQuantity()) - roomQuantity))
                        .setSoldQuantity(nullToZero(inventory.getSoldQuantity()) + roomQuantity));
                orderLockMapper.updateById(new BookingOrderLockDO().setId(lock.getId())
                        .setLockStatus(BookingOrderLockStatusEnum.SOLD.getStatus()));
            }
        }
        orderMapper.updateById(new BookingOrderDO().setId(id).setStatus(BookingOrderStatusEnum.COMPLETED.getStatus()));
    }

    @Override
    public BookingOrderDO getOrder(Long id, Long merchantId) {
        return validateOrderOwner(id, merchantId);
    }

    @Override
    public PageResult<BookingOrderDO> getOrderPage(BookingOrderPageReqVO pageReqVO, Long merchantId) {
        if (merchantContextService.getCurrentMerchantId() != null) {
            merchantContextService.validateMerchantOwner(merchantId);
        }
        return orderMapper.selectPage(pageReqVO, merchantId);
    }

    private BookingOrderLockDO buildOrderLock(BookingOrderDO order, BookingInventoryDO inventory) {
        BookingOrderLockDO orderLock = new BookingOrderLockDO()
                .setOrderId(order.getId())
                .setInventoryId(inventory.getId())
                .setMerchantId(order.getMerchantId())
                .setRoomTypeId(order.getRoomTypeId())
                .setBizDate(inventory.getBizDate())
                .setQuantity(order.getRoomQuantity())
                .setPrice(inventory.getPrice())
                .setLockStatus(BookingOrderLockStatusEnum.LOCKED.getStatus());
        orderLock.setTenantId(order.getTenantId());
        return orderLock;
    }

    private List<BookingInventoryRecordDO> buildLockRecords(BookingOrderDO order, List<BookingInventoryDO> inventories) {
        List<BookingInventoryRecordDO> records = new ArrayList<>();
        for (BookingInventoryDO inventory : inventories) {
            int beforeLocked = nullToZero(inventory.getLockedQuantity());
            int afterLocked = beforeLocked + nullToZero(order.getRoomQuantity());
            BookingInventoryRecordDO record = new BookingInventoryRecordDO()
                    .setMerchantId(order.getMerchantId())
                    .setRoomTypeId(order.getRoomTypeId())
                    .setInventoryId(inventory.getId())
                    .setBizDate(inventory.getBizDate())
                    .setQuantity(order.getRoomQuantity())
                    .setActionType(BookingInventoryActionTypeEnum.LOCK.getType())
                    .setBizType("BOOKING_ORDER")
                    .setBizId(order.getId())
                    .setBizNo(order.getOrderNo())
                    .setBeforeSnapshot("locked=" + beforeLocked)
                    .setAfterSnapshot("locked=" + afterLocked)
                    .setOperatorType("APP_USER");
            record.setTenantId(order.getTenantId());
            records.add(record);
        }
        return records;
    }

    private void moveLockedInventoryToSold(BookingOrderDO order) {
        List<BookingOrderLockDO> locks = orderLockMapper.selectListByOrderId(order.getId());
        if (locks.isEmpty()) {
            BookingInventoryDO inventory = selectOwnedInventoryForUpdate(order, order.getInventoryId());
            int roomQuantity = nullToZero(order.getRoomQuantity());
            inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId())
                    .setLockedQuantity(Math.max(0, nullToZero(inventory.getLockedQuantity()) - roomQuantity))
                    .setSoldQuantity(nullToZero(inventory.getSoldQuantity()) + roomQuantity));
            inventoryRecordMapper.insert(buildInventoryRecord(order, inventory, roomQuantity,
                    BookingInventoryActionTypeEnum.PAY_CONFIRMED));
            return;
        }
        List<BookingInventoryRecordDO> records = new ArrayList<>();
        for (BookingOrderLockDO lock : locks) {
            validateOrderLockOwner(order, lock);
            BookingInventoryDO inventory = selectOwnedInventoryForUpdate(order, lock.getInventoryId());
            int roomQuantity = nullToZero(lock.getQuantity());
            inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId())
                    .setLockedQuantity(Math.max(0, nullToZero(inventory.getLockedQuantity()) - roomQuantity))
                    .setSoldQuantity(nullToZero(inventory.getSoldQuantity()) + roomQuantity));
            orderLockMapper.updateById(new BookingOrderLockDO().setId(lock.getId())
                    .setLockStatus(BookingOrderLockStatusEnum.SOLD.getStatus()));
            records.add(buildInventoryRecord(order, inventory, roomQuantity, BookingInventoryActionTypeEnum.PAY_CONFIRMED));
        }
        inventoryRecordMapper.insertBatch(records);
    }

    private BookingInventoryRecordDO buildInventoryRecord(BookingOrderDO order, BookingInventoryDO inventory, Integer quantity,
                                                         BookingInventoryActionTypeEnum actionType) {
        BookingInventoryRecordDO record = new BookingInventoryRecordDO()
                .setMerchantId(order.getMerchantId())
                .setRoomTypeId(order.getRoomTypeId())
                .setInventoryId(inventory.getId())
                .setBizDate(inventory.getBizDate())
                .setQuantity(quantity)
                .setActionType(actionType.getType())
                .setBizType("BOOKING_ORDER")
                .setBizId(order.getId())
                .setBizNo(order.getOrderNo())
                .setBeforeSnapshot("locked=" + nullToZero(inventory.getLockedQuantity()) + ",sold=" + nullToZero(inventory.getSoldQuantity()))
                .setAfterSnapshot("locked=" + Math.max(0, nullToZero(inventory.getLockedQuantity()) - nullToZero(quantity))
                        + ",sold=" + (nullToZero(inventory.getSoldQuantity()) + nullToZero(quantity)))
                .setOperatorType("SYSTEM_JOB");
        record.setTenantId(order.getTenantId());
        return record;
    }

    private String generateOrderNo() {
        return "BK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private Boolean resolveAutoConfirmSnapshot(BookingOrderCreateReqVO createReqVO) {
        BookingRoomTypeDO roomType = roomTypeMapper.selectById(createReqVO.getRoomTypeId());
        if (roomType == null || !createReqVO.getMerchantId().equals(roomType.getMerchantId())
                || !createReqVO.getTenantId().equals(roomType.getTenantId())) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE.equals(roomType.getAutoConfirmEnabled());
    }

    private void validateCreateReq(BookingOrderCreateReqVO createReqVO) {
        if (createReqVO.getCheckInDate() == null || createReqVO.getCheckOutDate() == null
                || !createReqVO.getCheckInDate().isBefore(createReqVO.getCheckOutDate())
                || nullToZero(createReqVO.getRoomQuantity()) <= 0) {
            throw exception(BOOKING_DATE_INVALID);
        }
    }

    private void validateInventory(BookingInventoryDO inventory, Long tenantId, Long merchantId, Integer roomQuantity) {
        if (inventory == null || !merchantId.equals(inventory.getMerchantId()) || !tenantId.equals(inventory.getTenantId())) {
            throw exception(BOOKING_INVENTORY_NOT_EXISTS);
        }
        int available = nullToZero(inventory.getTotalQuantity()) - nullToZero(inventory.getLockedQuantity()) - nullToZero(inventory.getSoldQuantity());
        if (available < roomQuantity) {
            throw exception(BOOKING_INVENTORY_NOT_ENOUGH);
        }
    }

    private void validateContinuousInventories(List<BookingInventoryDO> inventories, Long tenantId, Long merchantId,
                                               LocalDate checkInDate, LocalDate checkOutDate, Integer roomQuantity) {
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (inventories == null || inventories.size() != nights) {
            throw exception(BOOKING_INVENTORY_NOT_EXISTS);
        }
        Map<LocalDate, BookingInventoryDO> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(BookingInventoryDO::getBizDate, Function.identity()));
        for (LocalDate bizDate = checkInDate; bizDate.isBefore(checkOutDate); bizDate = bizDate.plusDays(1)) {
            BookingInventoryDO inventory = inventoryMap.get(bizDate);
            validateInventory(inventory, tenantId, merchantId, roomQuantity);
        }
    }

    private void releaseLockedInventory(BookingOrderDO order) {
        releaseLockedInventory(order, false, null, null);
    }

    private void releaseConfirmedInventory(BookingOrderDO order, boolean tolerateMissingInventory,
                                           BookingInventoryActionTypeEnum actionType, String operatorType) {
        List<BookingOrderLockDO> locks = orderLockMapper.selectListByOrderId(order.getId());
        if (locks.isEmpty()) {
            BookingInventoryDO inventory = selectOwnedInventoryForUpdate(order, order.getInventoryId(), tolerateMissingInventory);
            if (inventory == null) {
                return;
            }
            int requestedQuantity = nullToZero(order.getRoomQuantity());
            int soldRelease = Math.min(nullToZero(inventory.getSoldQuantity()), requestedQuantity);
            int lockedRelease = Math.min(nullToZero(inventory.getLockedQuantity()), requestedQuantity - soldRelease);
            updateReleasedInventory(inventory, lockedRelease, soldRelease);
            insertReleaseRecord(order, inventory, lockedRelease, soldRelease, actionType, operatorType);
            return;
        }
        List<BookingInventoryRecordDO> records = new ArrayList<>();
        for (BookingOrderLockDO lock : locks) {
            validateOrderLockOwner(order, lock);
            if (BookingOrderLockStatusEnum.RELEASED.getStatus().equals(lock.getLockStatus())) {
                continue;
            }
            BookingInventoryDO inventory = selectOwnedInventoryForUpdate(order, lock.getInventoryId(), tolerateMissingInventory);
            if (inventory != null) {
                boolean soldLock = BookingOrderLockStatusEnum.SOLD.getStatus().equals(lock.getLockStatus());
                int lockedRelease = soldLock ? 0
                        : Math.min(nullToZero(inventory.getLockedQuantity()), nullToZero(lock.getQuantity()));
                int soldRelease = soldLock
                        ? Math.min(nullToZero(inventory.getSoldQuantity()), nullToZero(lock.getQuantity())) : 0;
                updateReleasedInventory(inventory, lockedRelease, soldRelease);
                addReleaseRecord(records, order, inventory, lockedRelease, soldRelease, actionType, operatorType);
            }
            orderLockMapper.updateById(new BookingOrderLockDO().setId(lock.getId())
                    .setLockStatus(BookingOrderLockStatusEnum.RELEASED.getStatus()));
        }
        if (!records.isEmpty()) {
            inventoryRecordMapper.insertBatch(records);
        }
    }

    private void updateReleasedInventory(BookingInventoryDO inventory, int lockedRelease, int soldRelease) {
        if (lockedRelease <= 0 && soldRelease <= 0) {
            return;
        }
        inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId())
                .setLockedQuantity(nullToZero(inventory.getLockedQuantity()) - lockedRelease)
                .setSoldQuantity(nullToZero(inventory.getSoldQuantity()) - soldRelease));
    }

    private void releaseLockedInventory(BookingOrderDO order, boolean tolerateMissingInventory,
                                        BookingInventoryActionTypeEnum actionType, String operatorType) {
        List<BookingOrderLockDO> locks = orderLockMapper.selectListByOrderId(order.getId());
        if (locks.isEmpty()) {
            BookingInventoryDO inventory = selectOwnedInventoryForUpdate(order, order.getInventoryId(), tolerateMissingInventory);
            if (inventory == null) {
                return;
            }
            int releaseQuantity = Math.min(nullToZero(inventory.getLockedQuantity()), nullToZero(order.getRoomQuantity()));
            int lockedQuantity = nullToZero(inventory.getLockedQuantity()) - releaseQuantity;
            inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId()).setLockedQuantity(lockedQuantity));
            insertReleaseRecord(order, inventory, releaseQuantity, actionType, operatorType);
            return;
        }
        List<BookingInventoryRecordDO> records = new ArrayList<>();
        for (BookingOrderLockDO lock : locks) {
            validateOrderLockOwner(order, lock);
            BookingInventoryDO inventory = selectOwnedInventoryForUpdate(order, lock.getInventoryId(), tolerateMissingInventory);
            if (inventory != null) {
                int releaseQuantity = Math.min(nullToZero(inventory.getLockedQuantity()), nullToZero(lock.getQuantity()));
                int lockedQuantity = nullToZero(inventory.getLockedQuantity()) - releaseQuantity;
                inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId()).setLockedQuantity(lockedQuantity));
                addReleaseRecord(records, order, inventory, releaseQuantity, actionType, operatorType);
            }
            orderLockMapper.updateById(new BookingOrderLockDO().setId(lock.getId())
                    .setLockStatus(BookingOrderLockStatusEnum.RELEASED.getStatus()));
        }
        if (!records.isEmpty()) {
            inventoryRecordMapper.insertBatch(records);
        }
    }

    private void insertReleaseRecord(BookingOrderDO order, BookingInventoryDO inventory, int releaseQuantity,
                                     BookingInventoryActionTypeEnum actionType, String operatorType) {
        if (releaseQuantity <= 0 || actionType == null) {
            return;
        }
        inventoryRecordMapper.insert(buildReleaseRecord(order, inventory, releaseQuantity, actionType, operatorType));
    }

    private void insertReleaseRecord(BookingOrderDO order, BookingInventoryDO inventory,
                                     int lockedRelease, int soldRelease,
                                     BookingInventoryActionTypeEnum actionType, String operatorType) {
        if ((lockedRelease <= 0 && soldRelease <= 0) || actionType == null) {
            return;
        }
        inventoryRecordMapper.insert(buildReleaseRecord(order, inventory, lockedRelease, soldRelease,
                actionType, operatorType));
    }

    private void addReleaseRecord(List<BookingInventoryRecordDO> records, BookingOrderDO order,
                                  BookingInventoryDO inventory, int releaseQuantity,
                                  BookingInventoryActionTypeEnum actionType, String operatorType) {
        if (releaseQuantity <= 0 || actionType == null) {
            return;
        }
        records.add(buildReleaseRecord(order, inventory, releaseQuantity, actionType, operatorType));
    }

    private void addReleaseRecord(List<BookingInventoryRecordDO> records, BookingOrderDO order,
                                  BookingInventoryDO inventory, int lockedRelease, int soldRelease,
                                  BookingInventoryActionTypeEnum actionType, String operatorType) {
        if ((lockedRelease <= 0 && soldRelease <= 0) || actionType == null) {
            return;
        }
        records.add(buildReleaseRecord(order, inventory, lockedRelease, soldRelease, actionType, operatorType));
    }

    private BookingInventoryRecordDO buildReleaseRecord(BookingOrderDO order, BookingInventoryDO inventory,
                                                         int releaseQuantity,
                                                         BookingInventoryActionTypeEnum actionType,
                                                         String operatorType) {
        return buildReleaseRecord(order, inventory, releaseQuantity, 0, actionType, operatorType);
    }

    private BookingInventoryRecordDO buildReleaseRecord(BookingOrderDO order, BookingInventoryDO inventory,
                                                         int lockedRelease, int soldRelease,
                                                         BookingInventoryActionTypeEnum actionType,
                                                         String operatorType) {
        int beforeLocked = nullToZero(inventory.getLockedQuantity());
        int beforeSold = nullToZero(inventory.getSoldQuantity());
        int afterLocked = beforeLocked - lockedRelease;
        int afterSold = beforeSold - soldRelease;
        BookingInventoryRecordDO record = new BookingInventoryRecordDO()
                .setMerchantId(order.getMerchantId())
                .setRoomTypeId(order.getRoomTypeId())
                .setInventoryId(inventory.getId())
                .setBizDate(inventory.getBizDate())
                .setQuantity(-(lockedRelease + soldRelease))
                .setActionType(actionType.getType())
                .setBizType("BOOKING_ORDER")
                .setBizId(order.getId())
                .setBizNo(order.getOrderNo())
                .setBeforeSnapshot("locked=" + beforeLocked + ",sold=" + beforeSold)
                .setAfterSnapshot("locked=" + afterLocked + ",sold=" + afterSold)
                .setOperatorType(operatorType);
        record.setTenantId(order.getTenantId());
        return record;
    }

    private BookingInventoryDO selectOwnedInventoryForUpdate(BookingOrderDO order, Long inventoryId) {
        return selectOwnedInventoryForUpdate(order, inventoryId, false);
    }

    private BookingInventoryDO selectOwnedInventoryForUpdate(BookingOrderDO order, Long inventoryId, boolean tolerateMissingInventory) {
        BookingInventoryDO inventory = inventoryMapper.selectByIdForUpdate(inventoryId);
        if (inventory == null) {
            if (tolerateMissingInventory) {
                return null;
            }
            throw exception(BOOKING_INVENTORY_NOT_EXISTS);
        }
        if (!order.getMerchantId().equals(inventory.getMerchantId())
                || !order.getTenantId().equals(inventory.getTenantId())) {
            throw exception(BOOKING_INVENTORY_NOT_EXISTS);
        }
        return inventory;
    }

    private void validateOrderLockOwner(BookingOrderDO order, BookingOrderLockDO lock) {
        if (lock == null || !order.getMerchantId().equals(lock.getMerchantId())
                || !order.getTenantId().equals(lock.getTenantId())) {
            throw exception(BOOKING_INVENTORY_NOT_EXISTS);
        }
    }

    private BookingOrderDO validateOrderOwner(Long id, Long merchantId) {
        validateMerchantOwnerIfPresent(merchantId);
        BookingOrderDO order = orderMapper.selectById(id);
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private BookingOrderDO validateOrderOwnerForUpdate(Long id, Long merchantId) {
        validateMerchantOwnerIfPresent(merchantId);
        BookingOrderDO order = orderMapper.selectByIdForUpdate(id);
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private boolean isCancellationTerminal(Integer status) {
        return BookingOrderStatusEnum.CANCELED.getStatus().equals(status)
                || BookingOrderStatusEnum.CLOSED_CANCELLED.getStatus().equals(status)
                || BookingOrderStatusEnum.CLOSED_TIMEOUT.getStatus().equals(status)
                || BookingOrderStatusEnum.REFUND_APPLYING.getStatus().equals(status)
                || BookingOrderStatusEnum.REFUNDED.getStatus().equals(status)
                || BookingOrderStatusEnum.REJECTED_REFUNDED.getStatus().equals(status);
    }

    private void validateMerchantOwnerIfPresent(Long merchantId) {
        if (merchantContextService.getCurrentMerchantId() != null) {
            merchantContextService.validateMerchantOwner(merchantId);
        }
    }

    private void validateStatus(BookingOrderDO order, BookingOrderStatusEnum status) {
        if (!status.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_INVALID);
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
