<template>
  <div class="saas-room-page">
    <section class="saas-room-page__hero">
      <div>
        <div class="saas-room-page__crumb">Hotel PMS Community / 酒店管理 / 房型管理</div>
        <h1>房型管理</h1>
        <p>维护房型名称、面积、早餐、入住人数、初始价格、展示图片与销售状态；每日价格可在“房态管理”中调整。</p>
      </div>
      <div class="saas-room-page__context">
        <span>当前商户</span>
        <strong>{{ merchantContext.name || '未加载' }}</strong>
        <small
        >商户编号 {{ merchantContext.id || '-' }} / 租户编号 {{ merchantContext.tenantId || '-' }}</small
        >
      </div>
    </section>

    <ContentWrap>
      <el-form
        ref="queryFormRef"
        :inline="true"
        :model="queryParams"
        class="-mb-15px"
        label-width="96px"
      >
        <el-form-item label="房型名称" prop="name">
          <el-input
            v-model="queryParams.name"
            class="!w-220px"
            clearable
            placeholder="输入房型名称搜索"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="房型状态" prop="status">
          <el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="全部">
            <el-option label="启用" :value="CommonStatusEnum.ENABLE" />
            <el-option label="停用" :value="CommonStatusEnum.DISABLE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery">
            <Icon class="mr-5px" icon="ep:search" />
            查询
          </el-button>
          <el-button @click="resetQuery">
            <Icon class="mr-5px" icon="ep:refresh" />
            重置
          </el-button>
          <el-button plain type="primary" @click="openForm('create')">
            <Icon class="mr-5px" icon="ep:plus" />
            新增房型
          </el-button>
          <el-button plain type="warning" @click="router.push('/hotel/inventory')">
            <Icon class="mr-5px" icon="ep:calendar" />
            库存与价格日历
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap>
      <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
        <el-table-column align="center" label="房型编号" prop="id" width="100" />
        <el-table-column align="center" label="房型名称" prop="name" min-width="180" />
        <el-table-column align="center" label="封面" width="120">
          <template #default="scope">
            <el-image
              v-if="scope.row.coverUrl"
              :preview-src-list="[scope.row.coverUrl]"
              :src="scope.row.coverUrl"
              fit="cover"
              preview-teleported
              class="room-thumb"
            />
            <span v-else class="empty-cell">-</span>
          </template>
        </el-table-column>
        <el-table-column align="center" label="细节图" width="100">
          <template #default="scope">{{ parseImageUrls(scope.row.imageUrls).length }} 张</template>
        </el-table-column>
        <el-table-column align="center" label="每间最大入住" prop="maxOccupancy" width="130" />
        <el-table-column align="center" label="面积" width="130">
          <template #default="scope">
            {{ formatArea(scope.row.areaSqmMin, scope.row.areaSqmMax, scope.row.areaSqm) }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="早餐" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.breakfastIncluded ? 'success' : 'info'">
              {{ scope.row.breakfastIncluded ? '含早餐' : '无早餐' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column align="center" label="设施" min-width="180">
          <template #default="scope">{{ formatFacilityCodes(scope.row.facilityCodes) }}</template>
        </el-table-column>
        <el-table-column align="center" label="初始价格" width="120">
          <template #default="scope">{{ formatPrice(scope.row.initialPrice) }}</template>
        </el-table-column>
        <el-table-column align="center" label="状态" prop="status" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.status === CommonStatusEnum.ENABLE ? 'success' : 'info'">
              {{ scope.row.status === CommonStatusEnum.ENABLE ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column align="center" fixed="right" label="操作" width="330">
          <template #default="scope">
            <el-button plain type="info" @click="showRoomTypeDetail(scope.row)">查看</el-button>
            <el-button plain type="primary" @click="openForm('update', scope.row.id)"
            >修改</el-button
            >
            <el-button
              plain
              :type="scope.row.status === CommonStatusEnum.ENABLE ? 'warning' : 'success'"
              @click="toggleStatus(scope.row)"
            >
              {{ scope.row.status === CommonStatusEnum.ENABLE ? '下架' : '上架' }}
            </el-button>
            <el-button plain type="danger" @click="deleteRoomType(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:limit="queryParams.pageSize"
        v-model:page="queryParams.pageNo"
        :total="total"
        @pagination="getList"
      />
    </ContentWrap>

    <Dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form
        ref="formRef"
        v-loading="formLoading"
        :model="formData"
        :rules="formRules"
        label-width="110px"
      >
        <el-form-item v-if="formType === 'create'" label="商户上下文">
          <el-input
            :model-value="`${merchantContext.name || '-'}（商户编号 ${merchantContext.id || '-'} / 租户编号 ${
              merchantContext.tenantId || '-'
            }）`"
            disabled
          />
        </el-form-item>
        <el-form-item label="房型名称" prop="name">
          <el-input v-model="formData.name" maxlength="80" placeholder="请输入房型名称" />
        </el-form-item>
        <el-form-item label="每间最大入住人数" prop="maxOccupancy">
          <el-input-number v-model="formData.maxOccupancy" class="!w-1/1" :max="20" :min="1" />
        </el-form-item>
        <el-form-item label="房型面积" prop="areaSqmMin">
          <div class="area-range-input">
            <el-input-number
              v-model="formData.areaSqmMin"
              :max="9999.99"
              :min="0.01"
              :precision="2"
              :step="1"
              placeholder="最小面积"
            />
            <span>至</span>
            <el-input-number
              v-model="formData.areaSqmMax"
              :max="9999.99"
              :min="0.01"
              :precision="2"
              :step="1"
              placeholder="最大面积"
              @change="validateAreaRangeField"
            />
            <span>㎡</span>
          </div>
        </el-form-item>
        <el-form-item label="早餐" prop="breakfastIncluded">
          <el-radio-group v-model="formData.breakfastIncluded">
            <el-radio :label="true">含早餐</el-radio>
            <el-radio :label="false">无早餐</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="房型设施">
          <el-checkbox-group v-model="selectedFacilityCodes" class="facility-checks">
            <el-checkbox
              v-for="option in allRoomFacilityOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-checkbox>
          </el-checkbox-group>
          <div class="custom-facility-row">
            <el-input
              v-model="customFacilityInput"
              clearable
              maxlength="12"
              placeholder="输入自定义设施，如儿童护栏"
              show-word-limit
              @keyup.enter="addCustomFacility"
            />
            <el-button type="primary" plain @click="addCustomFacility">新增设施</el-button>
          </div>
          <div class="upload-hint"
            >用于小程序房型详情展示，可多选；自定义设施保存后会成为当前商户共享选项。</div
          >
        </el-form-item>
        <el-form-item label="初始价格" prop="initialPriceYuan">
          <div class="price-input">
            <el-input-number
              v-model="formData.initialPriceYuan"
              class="!w-1/1"
              :max="999999"
              :min="0.01"
              :precision="2"
              :step="1"
            />
            <span>元 / 晚</span>
          </div>
        </el-form-item>
        <el-form-item label="房型封面" prop="coverUrl">
          <UploadImg
            v-model="formData.coverUrl"
            directory="hotel-pms/public/room"
            :height="'140px'"
            :width="'260px'"
          />
        </el-form-item>
        <el-form-item label="房型细节图">
          <UploadImgs
            v-model="roomImageList"
            directory="hotel-pms/public/room"
            :height="'108px'"
            :limit="6"
            :width="'108px'"
          />
          <div class="upload-hint">用于小程序房型详情预览，最多 6 张。</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="CommonStatusEnum.ENABLE">启用</el-radio>
            <el-radio :label="CommonStatusEnum.DISABLE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="dialogVisible = false">取 消</el-button>
      </template>
    </Dialog>
  </div>
</template>

<script lang="ts" setup>
import {
  SaasBookingApi,
  SaasRoomTypeCreateReqVO,
  SaasRoomTypePageReqVO,
  SaasRoomTypeUpdateReqVO,
  SaasRoomTypeVO
} from '@/api/saas/booking'
import { SaasMerchantApi, SaasMerchantVO } from '@/api/saas/merchant'
import { CommonStatusEnum } from '@/utils/constants'
import { setVisitTenantId } from '@/utils/auth'
import { UploadImg, UploadImgs } from '@/components/UploadFile'

defineOptions({ name: 'SaasMerchantRoomType' })

const router = useRouter()
const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const list = ref<SaasRoomTypeVO[]>([])
const total = ref(0)
const merchantContext = ref<SaasMerchantVO>({})
const queryParams = reactive<SaasRoomTypePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  status: undefined
})
const queryFormRef = ref()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
type RoomTypeFormData = Omit<SaasRoomTypeVO, 'initialPrice'> & {
  initialPriceYuan?: number
}
const formData = ref<RoomTypeFormData>({})
const roomImageList = ref<string[]>([])
const selectedFacilityCodes = ref<string[]>([])
const customFacilityInput = ref('')
const sharedCustomFacilityOptions = ref<{ label: string; value: string }[]>([])
const formRef = ref()
const roomFacilityOptions = [
  { label: '停车', value: 'parking' },
  { label: '充电桩', value: 'charging_pile' },
  { label: '接机接站', value: 'pickup' },
  { label: '嗨唱 KTV', value: 'ktv' },
  { label: '有投影', value: 'projection' },
  { label: '智能门锁', value: 'smart_lock' },
  { label: '高速 Wi-Fi', value: 'wifi' },
  { label: '早餐', value: 'breakfast' },
  { label: '浴缸', value: 'bathtub' },
  { label: '洗衣房', value: 'laundry' },
  { label: '厨房', value: 'kitchen' },
  { label: '冰箱', value: 'refrigerator' }
]
const CUSTOM_FACILITY_PREFIX = 'custom:'
const allRoomFacilityOptions = computed(() => [
  ...roomFacilityOptions,
  ...sharedCustomFacilityOptions.value
])
const validateInitialPrice = (
  _rule: unknown,
  value: number | undefined,
  callback: (error?: Error) => void
) => {
  if (!Number.isFinite(value) || Number(value) <= 0) {
    callback(new Error('初始价格必须大于 0'))
    return
  }
  callback()
}
const validateAreaRange = (
  _rule: unknown,
  _value: number | undefined,
  callback: (error?: Error) => void
) => {
  const min = Number(formData.value.areaSqmMin)
  const max = Number(formData.value.areaSqmMax)
  if (!Number.isFinite(min) || min <= 0 || !Number.isFinite(max) || max <= 0) {
    callback(new Error('请输入完整的房型面积区间'))
    return
  }
  if (max < min) {
    callback(new Error('最大面积不能小于最小面积'))
    return
  }
  callback()
}
const formRules = reactive({
  name: [{ required: true, message: '房型名称不能为空', trigger: 'blur' }],
  maxOccupancy: [{ required: true, message: '最大入住人数不能为空', trigger: 'blur' }],
  areaSqmMin: [
    { required: true, message: '房型面积不能为空', trigger: 'change' },
    { validator: validateAreaRange, trigger: 'change' }
  ],
  breakfastIncluded: [{ required: true, message: '请选择是否含早餐', trigger: 'change' }],
  initialPriceYuan: [
    { required: true, message: '初始价格不能为空', trigger: 'change' },
    { validator: validateInitialPrice, trigger: 'change' }
  ]
})

const loadMerchantContext = async () => {
  merchantContext.value = await SaasMerchantApi.getMerchantContext()
  if (!merchantContext.value.id) {
    throw new Error('当前账号未绑定商户，无法进入商户后台')
  }
  if (merchantContext.value.tenantId) {
    setVisitTenantId(merchantContext.value.tenantId)
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SaasBookingApi.getRoomTypePage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.name = undefined
  queryParams.status = undefined
  handleQuery()
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    name: undefined,
    maxOccupancy: 2,
    areaSqm: undefined,
    areaSqmMin: undefined,
    areaSqmMax: undefined,
    breakfastIncluded: false,
    initialPriceYuan: undefined,
    status: CommonStatusEnum.ENABLE,
    coverUrl: undefined,
    imageUrls: undefined,
    facilityCodes: undefined
  }
  roomImageList.value = []
  selectedFacilityCodes.value = []
  customFacilityInput.value = ''
  formRef.value?.resetFields()
}

const openForm = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增房型' : '编辑房型'
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    await loadSharedCustomFacilities()
    if (id) {
      const roomType: SaasRoomTypeVO = await SaasBookingApi.getRoomType(id)
      const legacyArea = roomType.areaSqm
      formData.value = {
        ...roomType,
        areaSqmMin: roomType.areaSqmMin ?? legacyArea,
        areaSqmMax: roomType.areaSqmMax ?? legacyArea,
        initialPriceYuan: priceToYuan(roomType.initialPrice)
      }
      roomImageList.value = parseImageUrls(roomType.imageUrls)
      selectedFacilityCodes.value = parseFacilityCodes(roomType.facilityCodes)
      mergeSharedCustomFacilityCodes(selectedFacilityCodes.value)
    }
  } finally {
    formLoading.value = false
  }
}

const showRoomTypeDetail = async (row: SaasRoomTypeVO) => {
  await ElMessageBox.alert(
    [
      '房型名称：' + (row.name || '-'),
      '每间最大入住人数：' + (row.maxOccupancy || '-'),
      '房型面积：' + formatArea(row.areaSqmMin, row.areaSqmMax, row.areaSqm),
      '早餐：' + (row.breakfastIncluded ? '含早餐' : '无早餐'),
      '设施：' + formatFacilityCodes(row.facilityCodes),
      '初始价格：' + formatPrice(row.initialPrice),
      '细节图：' + parseImageUrls(row.imageUrls).length + ' 张',
      '状态：' + (row.status === CommonStatusEnum.ENABLE ? '启用' : '停用')
    ].join('\n'),
    '房型详情',
    { confirmButtonText: '关闭' }
  )
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const { initialPriceYuan, ...roomTypeData } = formData.value
    const payload = {
      ...roomTypeData,
      areaSqm: roomTypeData.areaSqmMin,
      initialPrice: yuanToFen(initialPriceYuan),
      imageUrls: stringifyImageUrls(roomImageList.value),
      facilityCodes: stringifyFacilityCodes(selectedFacilityCodes.value)
    }
    if (formType.value === 'create') {
      await SaasBookingApi.createRoomType(payload as SaasRoomTypeCreateReqVO)
      message.success(t('common.createSuccess'))
    } else {
      await SaasBookingApi.updateRoomType(payload as SaasRoomTypeUpdateReqVO)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const parseImageUrls = (value?: string | string[]) => {
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean)
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter(Boolean) : []
  } catch {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
}

const stringifyImageUrls = (urls: string[]) => {
  const normalized = urls.map((url) => String(url || '').trim()).filter(Boolean)
  return normalized.length ? JSON.stringify(normalized) : undefined
}

const parseFacilityCodes = (value?: string | string[]) => {
  if (!value) return []
  if (Array.isArray(value)) return value.map((item) => String(item).trim()).filter(Boolean)
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map((item) => String(item).trim()).filter(Boolean) : []
  } catch {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
}

const stringifyFacilityCodes = (codes: string[]) => {
  const normalized = Array.from(
    new Set(codes.map((code) => String(code || '').trim()).filter(Boolean))
  )
  return normalized.length ? JSON.stringify(normalized) : undefined
}

const normalizeFacilityLabel = (label: string) => label.replace(/\s+/g, ' ').trim()

const isCustomFacilityCode = (code: string) => code.startsWith(CUSTOM_FACILITY_PREFIX)

const customFacilityLabel = (code: string) =>
  normalizeFacilityLabel(
    isCustomFacilityCode(code) ? code.slice(CUSTOM_FACILITY_PREFIX.length) : code
  )

const customFacilityCode = (label: string) => `${CUSTOM_FACILITY_PREFIX}${label}`

const mergeSharedCustomFacilityCodes = (codes: string[]) => {
  const options = new Map(sharedCustomFacilityOptions.value.map((option) => [option.value, option]))
  codes
    .filter(isCustomFacilityCode)
    .forEach((code) => {
      const label = customFacilityLabel(code)
      if (label && !options.has(code)) {
        options.set(code, { label, value: code })
      }
    })
  sharedCustomFacilityOptions.value = Array.from(options.values())
}

const loadSharedCustomFacilities = async () => {
  sharedCustomFacilityOptions.value = []
  mergeSharedCustomFacilityCodes(await SaasBookingApi.getSharedFacilityOptions())
}

const addCustomFacility = () => {
  const label = normalizeFacilityLabel(customFacilityInput.value)
  if (!label) {
    message.warning('请输入自定义设施名称')
    return
  }
  const existedBuiltin = roomFacilityOptions.find((option) => option.label === label)
  if (existedBuiltin) {
    if (!selectedFacilityCodes.value.includes(existedBuiltin.value)) {
      selectedFacilityCodes.value.push(existedBuiltin.value)
    }
    customFacilityInput.value = ''
    message.warning('该设施已在固定选项中，已为你勾选')
    return
  }
  const value = customFacilityCode(label)
  mergeSharedCustomFacilityCodes([value])
  if (!selectedFacilityCodes.value.includes(value)) {
    selectedFacilityCodes.value.push(value)
  }
  customFacilityInput.value = ''
}

const facilityLabel = (code: string) =>
  roomFacilityOptions.find((option) => option.value === code)?.label ||
  customFacilityLabel(code) ||
  code

const formatFacilityCodes = (value?: string | string[]) => {
  const labels = parseFacilityCodes(value).map(facilityLabel)
  return labels.length ? labels.join('、') : '-'
}

const priceToYuan = (price?: number) =>
  price !== undefined && price !== null && price > 0 ? Number((price / 100).toFixed(2)) : undefined

const yuanToFen = (price?: number) =>
  price !== undefined && price !== null ? Math.round(Number(price) * 100) : undefined

const formatPrice = (price?: number) =>
  price !== undefined && price !== null && price > 0 ? `¥${(price / 100).toFixed(2)}` : '-'

const formatArea = (areaMin?: number, areaMax?: number, legacyArea?: number) => {
  const min = Number(areaMin ?? legacyArea)
  const max = Number(areaMax ?? areaMin ?? legacyArea)
  if (!Number.isFinite(min) || min <= 0 || !Number.isFinite(max) || max <= 0) return '-'
  return min === max ? `${min} ㎡` : `${min}–${max} ㎡`
}

const validateAreaRangeField = () => {
  formRef.value?.validateField('areaSqmMin')
}

const toggleStatus = async (row: SaasRoomTypeVO) => {
  if (!row.id) return
  const nextStatus =
    row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  await message.confirm(
    `确认${nextStatus === CommonStatusEnum.ENABLE ? '上架' : '下架'}房型“${row.name || row.id}”吗？`
  )
  await SaasBookingApi.updateRoomTypeStatus(row.id, nextStatus)
  message.success('状态更新成功')
  await getList()
}

const deleteRoomType = async (row: SaasRoomTypeVO) => {
  if (!row.id) return
  await message.confirm(`确认删除房型“${row.name || row.id}”吗？删除后将无法继续被小程序预订。`)
  await SaasBookingApi.deleteRoomType(row.id)
  message.success('删除成功')
  await getList()
}

onMounted(async () => {
  await loadMerchantContext()
  await getList()
})
</script>

<style lang="scss" scoped>
.saas-room-page {
  min-height: calc(100vh - 84px);
  margin: -12px -12px 0;
  padding: 26px;
  color: #111827;
  background:
    radial-gradient(circle at 20% 0, rgba(16, 185, 129, 0.14), transparent 28%),
    radial-gradient(circle at 90% 8%, rgba(37, 99, 235, 0.1), transparent 30%), #eefaf7;
  border-radius: 22px;
}

.saas-room-page__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: stretch;
  margin-bottom: 18px;
  padding: 24px;
  color: #fff;
  background: linear-gradient(135deg, #052e2b 0%, #059669 58%, #0ea5e9 100%);
  border-radius: 20px;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.1);

  h1 {
    margin: 8px 0;
    font-size: 30px;
    font-weight: 800;
  }

  p {
    max-width: 760px;
    margin: 0;
    color: rgba(255, 255, 255, 0.78);
    line-height: 1.7;
  }
}

.saas-room-page__crumb {
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
}

.saas-room-page__context {
  width: 280px;
  padding: 18px;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 16px;

  span,
  small {
    display: block;
    color: #64748b;
  }

  strong {
    display: block;
    margin: 8px 0;
    color: #059669;
    font-size: 20px;
  }
}

.room-thumb {
  width: 72px;
  height: 48px;
  border-radius: 8px;
}

.upload-hint {
  width: 100%;
  margin-top: 8px;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.facility-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  line-height: 1.8;
}

.custom-facility-row {
  display: flex;
  width: 100%;
  max-width: 560px;
  align-items: center;
  gap: 10px;
  margin-top: 12px;

  :deep(.el-input) {
    flex: 1 1 auto;
  }
}

.price-input {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 10px;

  span {
    flex: 0 0 auto;
    color: #667085;
  }
}

.area-range-input {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;

  span {
    color: #667085;
  }
}

.empty-cell {
  color: #98a2b3;
}

@media (max-width: 768px) {
  .saas-room-page {
    margin: 0;
    padding: 16px;
  }

  .saas-room-page__hero {
    flex-direction: column;
  }

  .saas-room-page__context {
    width: 100%;
  }
}
</style>
