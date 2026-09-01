package cn.iocoder.yudao.module.booking.service.stock;

import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryDO;
import cn.iocoder.yudao.module.booking.dal.dataobject.inventory.BookingInventoryRecordDO;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryMapper;
import cn.iocoder.yudao.module.booking.dal.mysql.inventory.BookingInventoryRecordMapper;
import cn.iocoder.yudao.module.booking.enums.BookingInventoryActionTypeEnum;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.*;

@Service
@Validated
public class BookingStockServiceImpl implements BookingStockService {

    @Resource
    private BookingInventoryMapper inventoryMapper;
    @Resource
    private BookingInventoryRecordMapper inventoryRecordMapper;
    @Resource
    private MerchantContextService merchantContextService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long occupy(Long tenantId, Long merchantId, Long resourceId, LocalDate bizDate, Integer quantity) {
        merchantContextService.validateMerchantOwner(merchantId);
        merchantContextService.validateMerchantTenant(merchantId, tenantId);
        BookingInventoryDO inventory = inventoryMapper.selectByRoomTypeIdAndDateForUpdate(tenantId, merchantId, resourceId, bizDate);
        if (inventory == null || !merchantId.equals(inventory.getMerchantId()) || !tenantId.equals(inventory.getTenantId())) {
            throw exception(BOOKING_INVENTORY_NOT_EXISTS);
        }
        int available = nullToZero(inventory.getTotalQuantity()) - nullToZero(inventory.getLockedQuantity()) - nullToZero(inventory.getSoldQuantity());
        if (available < nullToZero(quantity)) {
            throw exception(BOOKING_INVENTORY_NOT_ENOUGH);
        }
        inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId())
                .setLockedQuantity(nullToZero(inventory.getLockedQuantity()) + nullToZero(quantity)));
        return inventory.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long tenantId, Long merchantId, Long inventoryId, Integer quantity) {
        merchantContextService.validateMerchantOwner(merchantId);
        merchantContextService.validateMerchantTenant(merchantId, tenantId);
        BookingInventoryDO inventory = inventoryMapper.selectByIdForUpdate(inventoryId);
        if (inventory == null || !merchantId.equals(inventory.getMerchantId()) || !tenantId.equals(inventory.getTenantId())) {
            throw exception(BOOKING_INVENTORY_NOT_EXISTS);
        }
        inventoryMapper.updateById(new BookingInventoryDO().setId(inventoryId)
                .setLockedQuantity(Math.max(0, nullToZero(inventory.getLockedQuantity()) - nullToZero(quantity))));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualLock(Long tenantId, Long merchantId, Long roomTypeId, LocalDate bizDate, Integer quantity, Long operatorId) {
        BookingInventoryDO inventory = selectOwnedInventoryForUpdate(tenantId, merchantId, roomTypeId, bizDate);
        int lockQuantity = positiveQuantity(quantity);
        int available = nullToZero(inventory.getTotalQuantity()) - nullToZero(inventory.getLockedQuantity())
                - nullToZero(inventory.getSoldQuantity());
        if (available < lockQuantity) {
            throw exception(BOOKING_INVENTORY_NOT_ENOUGH);
        }
        int nextLocked = nullToZero(inventory.getLockedQuantity()) + lockQuantity;
        inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId()).setLockedQuantity(nextLocked));
        inventoryRecordMapper.insert(buildManualRecord(inventory, lockQuantity,
                BookingInventoryActionTypeEnum.MANUAL_LOCK, operatorId, nextLocked));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualUnlock(Long tenantId, Long merchantId, Long roomTypeId, LocalDate bizDate, Integer quantity, Long operatorId) {
        BookingInventoryDO inventory = selectOwnedInventoryForUpdate(tenantId, merchantId, roomTypeId, bizDate);
        int unlockQuantity = positiveQuantity(quantity);
        if (nullToZero(inventory.getLockedQuantity()) < unlockQuantity) {
            throw exception(BOOKING_INVENTORY_NOT_ENOUGH);
        }
        int nextLocked = nullToZero(inventory.getLockedQuantity()) - unlockQuantity;
        inventoryMapper.updateById(new BookingInventoryDO().setId(inventory.getId()).setLockedQuantity(nextLocked));
        inventoryRecordMapper.insert(buildManualRecord(inventory, unlockQuantity,
                BookingInventoryActionTypeEnum.MANUAL_UNLOCK, operatorId, nextLocked));
    }

    private BookingInventoryDO selectOwnedInventoryForUpdate(Long tenantId, Long merchantId, Long roomTypeId, LocalDate bizDate) {
        merchantContextService.validateMerchantOwner(merchantId);
        merchantContextService.validateMerchantTenant(merchantId, tenantId);
        BookingInventoryDO inventory = inventoryMapper.selectByRoomTypeIdAndDateForUpdate(tenantId, merchantId, roomTypeId, bizDate);
        if (inventory == null || !merchantId.equals(inventory.getMerchantId()) || !tenantId.equals(inventory.getTenantId())) {
            throw exception(BOOKING_INVENTORY_NOT_EXISTS);
        }
        return inventory;
    }

    private BookingInventoryRecordDO buildManualRecord(BookingInventoryDO inventory, Integer quantity,
                                                       BookingInventoryActionTypeEnum actionType, Long operatorId,
                                                       Integer nextLocked) {
        BookingInventoryRecordDO record = new BookingInventoryRecordDO()
                .setMerchantId(inventory.getMerchantId())
                .setRoomTypeId(inventory.getRoomTypeId())
                .setInventoryId(inventory.getId())
                .setBizDate(inventory.getBizDate())
                .setQuantity(quantity)
                .setActionType(actionType.getType())
                .setBizType("MANUAL_STOCK")
                .setBeforeSnapshot(snapshot(inventory))
                .setAfterSnapshot("total=" + nullToZero(inventory.getTotalQuantity())
                        + ",locked=" + nextLocked
                        + ",sold=" + nullToZero(inventory.getSoldQuantity())
                        + ",price=" + (inventory.getPrice() == null ? "-" : inventory.getPrice()))
                .setOperatorType("ADMIN_USER")
                .setOperatorId(operatorId);
        record.setTenantId(inventory.getTenantId());
        return record;
    }

    private String snapshot(BookingInventoryDO inventory) {
        return "total=" + nullToZero(inventory.getTotalQuantity())
                + ",locked=" + nullToZero(inventory.getLockedQuantity())
                + ",sold=" + nullToZero(inventory.getSoldQuantity())
                + ",price=" + (inventory.getPrice() == null ? "-" : inventory.getPrice());
    }

    private int positiveQuantity(Integer value) {
        int quantity = nullToZero(value);
        if (quantity <= 0) {
            throw exception(BOOKING_INVENTORY_NOT_ENOUGH);
        }
        return quantity;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
