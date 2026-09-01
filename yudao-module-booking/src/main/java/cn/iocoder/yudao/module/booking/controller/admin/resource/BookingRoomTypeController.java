package cn.iocoder.yudao.module.booking.controller.admin.resource;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.booking.controller.admin.resource.vo.BookingRoomTypeCreateReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.resource.vo.BookingRoomTypePageReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.resource.vo.BookingRoomTypeRespVO;
import cn.iocoder.yudao.module.booking.controller.admin.resource.vo.BookingRoomTypeStatusReqVO;
import cn.iocoder.yudao.module.booking.controller.admin.resource.vo.BookingRoomTypeUpdateReqVO;
import cn.iocoder.yudao.module.booking.dal.dataobject.resource.BookingRoomTypeDO;
import cn.iocoder.yudao.module.booking.dal.mysql.resource.BookingRoomTypeMapper;
import cn.iocoder.yudao.module.merchant.service.MerchantContextService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.booking.enums.ErrorCodeConstants.BOOKING_ROOM_TYPE_NOT_EXISTS;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_CONTEXT_NOT_EXISTS;

@Tag(name = "管理后台 - 房型")
@RestController
@RequestMapping("/booking/room-type")
@Validated
public class BookingRoomTypeController {

    private static final String CUSTOM_FACILITY_PREFIX = "custom:";

    @Resource
    private BookingRoomTypeMapper roomTypeMapper;
    @Resource
    private MerchantContextService merchantContextService;

    @GetMapping("/page")
    @Operation(summary = "获得房型分页")
    @PreAuthorize("@ss.hasPermission('booking:room-type:query')")
    public CommonResult<PageResult<BookingRoomTypeRespVO>> getRoomTypePage(@Valid BookingRoomTypePageReqVO pageReqVO) {
        pageReqVO.setMerchantId(currentMerchantId());
        LambdaQueryWrapperX<BookingRoomTypeDO> queryWrapper = new LambdaQueryWrapperX<BookingRoomTypeDO>()
                .likeIfPresent(BookingRoomTypeDO::getName, pageReqVO.getName())
                .eq(BookingRoomTypeDO::getMerchantId, pageReqVO.getMerchantId())
                .eq(pageReqVO.getStatus() != null, BookingRoomTypeDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(BookingRoomTypeDO::getId);
        PageResult<BookingRoomTypeDO> pageResult = roomTypeMapper.selectPage(pageReqVO, queryWrapper);
        return success(BeanUtils.toBean(pageResult, BookingRoomTypeRespVO.class));
    }

    @GetMapping("/facility-options")
    @Operation(summary = "获得当前商户共享的自定义房型设施")
    @PreAuthorize("@ss.hasPermission('booking:room-type:query')")
    public CommonResult<List<String>> getSharedFacilityOptions() {
        Long merchantId = currentMerchantId();
        List<BookingRoomTypeDO> roomTypes = roomTypeMapper.selectList(
                new QueryWrapper<BookingRoomTypeDO>()
                        .select("facility_codes")
                        .eq("merchant_id", merchantId)
                        .isNotNull("facility_codes"));
        Set<String> customFacilityCodes = new LinkedHashSet<>();
        roomTypes.forEach(roomType -> collectCustomFacilityCodes(roomType.getFacilityCodes(), customFacilityCodes));
        return success(new ArrayList<>(customFacilityCodes));
    }

    @GetMapping("/get")
    @Operation(summary = "获得房型")
    @Parameter(name = "id", description = "房型编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:room-type:query')")
    public CommonResult<BookingRoomTypeRespVO> getRoomType(@RequestParam("id") Long id) {
        Long merchantId = currentMerchantId();
        BookingRoomTypeDO roomType = roomTypeMapper.selectById(id);
        if (roomType == null || !merchantId.equals(roomType.getMerchantId())) {
            return success(null);
        }
        return success(BeanUtils.toBean(roomType, BookingRoomTypeRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建房型")
    @PreAuthorize("@ss.hasPermission('booking:room-type:create')")
    public CommonResult<Long> createRoomType(@Valid @RequestBody BookingRoomTypeCreateReqVO createReqVO) {
        createReqVO.setTenantId(TenantContextHolder.getRequiredTenantId());
        createReqVO.setMerchantId(currentMerchantId());
        BookingRoomTypeDO roomType = BeanUtils.toBean(createReqVO, BookingRoomTypeDO.class);
        normalizeAreaRange(roomType, createReqVO.getAreaSqm(), createReqVO.getAreaSqmMin(), createReqVO.getAreaSqmMax());
        if (roomType.getStatus() == null) {
            roomType.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        roomTypeMapper.insert(roomType);
        return success(roomType.getId());
    }

    @PutMapping("/update")
    @Operation(summary = "更新房型")
    @PreAuthorize("@ss.hasPermission('booking:room-type:update')")
    public CommonResult<Boolean> updateRoomType(@Valid @RequestBody BookingRoomTypeUpdateReqVO updateReqVO) {
        validateCurrentMerchantRoomType(updateReqVO.getId());
        BookingRoomTypeDO roomType = BeanUtils.toBean(updateReqVO, BookingRoomTypeDO.class);
        normalizeAreaRange(roomType, updateReqVO.getAreaSqm(), updateReqVO.getAreaSqmMin(), updateReqVO.getAreaSqmMax());
        roomTypeMapper.updateById(roomType);
        return success(true);
    }

    @PutMapping("/status")
    @Operation(summary = "更新房型状态")
    @PreAuthorize("@ss.hasPermission('booking:room-type:update')")
    public CommonResult<Boolean> updateRoomTypeStatus(@Valid @RequestBody BookingRoomTypeStatusReqVO updateReqVO) {
        validateCurrentMerchantRoomType(updateReqVO.getId());
        roomTypeMapper.updateById(new BookingRoomTypeDO().setId(updateReqVO.getId()).setStatus(updateReqVO.getStatus()));
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除房型")
    @Parameter(name = "id", description = "房型编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:room-type:delete')")
    public CommonResult<Boolean> deleteRoomType(@RequestParam("id") Long id) {
        validateCurrentMerchantRoomType(id);
        roomTypeMapper.deleteById(id);
        return success(true);
    }

    private void validateCurrentMerchantRoomType(Long id) {
        Long merchantId = currentMerchantId();
        BookingRoomTypeDO roomType = roomTypeMapper.selectById(id);
        if (roomType == null || !merchantId.equals(roomType.getMerchantId())) {
            throw exception(BOOKING_ROOM_TYPE_NOT_EXISTS);
        }
    }

    private Long currentMerchantId() {
        Long merchantId = merchantContextService.getCurrentMerchantId();
        if (merchantId == null) {
            throw exception(MERCHANT_CONTEXT_NOT_EXISTS);
        }
        return merchantId;
    }

    private void collectCustomFacilityCodes(String facilityCodes, Set<String> target) {
        String[] codes = JsonUtils.parseObjectQuietly(facilityCodes, String[].class);
        if (codes == null && facilityCodes != null) {
            codes = facilityCodes.split(",");
        }
        if (codes == null) {
            return;
        }
        for (String code : codes) {
            String normalized = code == null ? "" : code.trim();
            if (normalized.startsWith(CUSTOM_FACILITY_PREFIX)
                    && normalized.length() > CUSTOM_FACILITY_PREFIX.length()) {
                target.add(normalized);
            }
        }
    }

    private void normalizeAreaRange(BookingRoomTypeDO roomType, BigDecimal legacyArea,
                                    BigDecimal areaMin, BigDecimal areaMax) {
        BigDecimal normalizedMin = areaMin != null ? areaMin : legacyArea;
        BigDecimal normalizedMax = areaMax != null ? areaMax : legacyArea;
        roomType.setAreaSqm(normalizedMin);
        roomType.setAreaSqmMin(normalizedMin);
        roomType.setAreaSqmMax(normalizedMax);
    }
}
