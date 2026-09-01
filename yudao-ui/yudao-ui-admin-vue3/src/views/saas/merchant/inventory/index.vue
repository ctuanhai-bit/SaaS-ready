<template>
  <div class="saas-inventory-page">
    <section class="saas-inventory-page__hero">
      <div>
        <div class="saas-inventory-page__crumb">Hotel PMS Community / 酒店管理 / {{ title }}</div>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <div class="saas-inventory-page__context">
        <span>当前商户</span>
        <strong>{{ merchantContext.name || '未加载' }}</strong>
        <small
        >商户编号 {{ merchantContext.id || '-' }} / 租户编号 {{ merchantContext.tenantId || '-' }}</small
        >
      </div>
    </section>

    <el-alert
      v-if="pageError"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="pageError"
    >
      <template #default>
        <el-button class="mt-8px" type="primary" @click="loadPageData">重试</el-button>
      </template>
    </el-alert>

    <ContentWrap class="inventory-calendar-wrap">
      <div class="inventory-calendar__header">
        <div>
          <strong
          >{{ selectedRoomTypeName }} · 当前筛选 {{ filteredCalendarDays.length }} 天库存</strong
          >
          <span>按日期查看总量、可售、锁定、价格与销售状态</span>
        </div>
        <div class="inventory-calendar__actions">
          <el-select
            v-model="selectedRoomTypeId"
            class="!w-220px"
            :loading="loadingRoomTypes"
            placeholder="选择房型"
            @change="handleRoomTypeChange"
          >
            <el-option
              v-for="room in selectableRoomTypes"
              :key="room.id"
              :label="room.name"
              :value="room.id"
            />
          </el-select>
          <el-button-group>
            <el-button :disabled="loading" @click="shiftDateRange(-14)">前 14 天</el-button>
            <el-button :disabled="loading" @click="handleTodayClick">今天</el-button>
            <el-button :disabled="loading" @click="shiftDateRange(14)">后 14 天</el-button>
          </el-button-group>
          <el-button type="primary" :disabled="loading || !selectedRoomTypeId" @click="openEdit()"
          >批量调整</el-button
          >
        </div>
      </div>

      <el-form :inline="true" class="inventory-calendar__filters" label-width="78px">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="loadPageData"
          />
        </el-form-item>
        <el-form-item label="可用状态">
          <el-select v-model="availabilityFilter" class="!w-160px" placeholder="全部">
            <el-option label="全部" value="all" />
            <el-option label="可售" value="available" />
            <el-option label="紧张" value="tight" />
            <el-option label="满房" value="soldOut" />
            <el-option label="未设置" value="unconfigured" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :loading="loading" @click="loadPageData">
            <Icon class="mr-5px" icon="ep:search" />
            查询
          </el-button>
        </el-form-item>
      </el-form>

      <el-empty
        v-if="!loading && !filteredCalendarDays.length && !pageError"
        description="暂无房态库存数据"
      />
      <div v-else v-loading="loading" class="inventory-calendar">
        <button
          v-for="day in filteredCalendarDays"
          :key="day.dateKey"
          class="inventory-day"
          :class="{
            'is-today': day.isToday,
            'is-unconfigured': !day.configured,
            'is-sold-out': day.configured && day.available <= 0,
            'is-tight': day.configured && day.available > 0 && day.available <= 2
          }"
          type="button"
          @click="openEdit(day.raw, day.configured)"
        >
          <div class="inventory-day__top">
            <span>{{ day.shortDate }}</span>
            <small>{{ day.weekday }}</small>
          </div>
          <el-tag :type="day.tagType" effect="light" size="small">{{ day.statusText }}</el-tag>
          <strong>{{ day.configured ? day.available : '--' }}</strong>
          <div v-if="day.configured" class="inventory-day__meta">
            <span>总量 {{ day.total }}</span>
            <span>锁定 {{ day.locked }}</span>
          </div>
          <div v-else class="inventory-day__meta inventory-day__meta--unset">
            <span>库存尚未配置</span>
          </div>
          <small class="inventory-day__price">{{ formatPrice(day.price) }}</small>
        </button>
      </div>
    </ContentWrap>

    <ContentWrap title="房态操作">
      <div class="inventory-action-grid">
        <div class="inventory-action-card">
          <span>锁房</span>
          <strong>指定日期减少可售库存</strong>
          <el-button plain type="warning" @click="openInventoryAction('lock')">锁房</el-button>
        </div>
        <div class="inventory-action-card">
          <span>解锁</span>
          <strong>恢复人工锁定库存</strong>
          <el-button plain type="success" @click="openInventoryAction('unlock')">解锁</el-button>
        </div>
        <div class="inventory-action-card">
          <span>调整库存</span>
          <strong>按房型和日期修改可售库存</strong>
          <el-button plain type="primary" @click="openEdit()">调整库存</el-button>
        </div>
        <div class="inventory-action-card">
          <span>查看订单</span>
          <strong>按日期查看订单占用来源</strong>
          <el-button plain type="info" @click="router.push('/hotel/orders')">查看订单</el-button>
        </div>
      </div>
    </ContentWrap>

    <div class="inventory-bottom">
      <ContentWrap title="库存变更记录">
        <el-alert
          v-if="!loadingLogs && !logs.length"
          :closable="false"
          show-icon
          title="暂无变更记录"
          type="info"
        />
        <el-table
          v-else
          v-loading="loadingLogs"
          :data="logs"
          :stripe="true"
          :show-overflow-tooltip="true"
        >
          <el-table-column label="时间" prop="createTime" min-width="150" />
          <el-table-column label="操作人" prop="operator" width="120" />
          <el-table-column label="类型" prop="type" width="120" />
          <el-table-column label="说明" prop="remark" min-width="220" />
        </el-table>
      </ContentWrap>
    </div>

    <Dialog v-model="dialogVisible" title="批量调整库存" width="560px">
      <el-form v-loading="submitLoading" :model="formData" label-width="110px">
        <el-form-item label="适用房型" prop="roomTypeIds">
          <div class="inventory-batch-room-types">
            <el-select
              v-model="formData.roomTypeIds"
              collapse-tags
              collapse-tags-tooltip
              class="inventory-batch-room-types__select"
              multiple
              placeholder="选择一个或多个房型"
            >
              <el-option
                v-for="room in selectableRoomTypes"
                :key="room.id"
                :label="room.name"
                :value="room.id"
              />
            </el-select>
            <el-button @click="toggleAllRoomTypes">
              {{ allRoomTypesSelected ? '清空' : '全选' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="日期范围" prop="dateRange">
          <el-date-picker
            v-model="formData.dateRange"
            end-placeholder="结束日期"
            start-placeholder="开始日期"
            type="daterange"
            value-format="YYYY-MM-DD"
            class="!w-1/1"
          />
        </el-form-item>
        <el-form-item label="每日可售库存" prop="available">
          <el-input-number v-model="formData.available" :min="0" class="!w-1/1" />
        </el-form-item>
        <el-form-item label="每日价格（元）" prop="price">
          <el-input-number
            v-model="formData.price"
            :min="0.01"
            :precision="2"
            :step="0.01"
            class="!w-1/1"
          />
        </el-form-item>
        <el-alert
          v-if="batchTargetCount"
          :closable="false"
          show-icon
          type="warning"
          :title="`将统一调整 ${formData.roomTypeIds.length} 个房型、${batchDates.length} 天，共 ${batchTargetCount} 条房态库存`"
        />
      </el-form>
      <template #footer>
        <el-button :disabled="submitLoading" @click="dialogVisible = false">取消</el-button>
        <el-button :loading="submitLoading" type="primary" @click="submitForm">确认调整</el-button>
      </template>
    </Dialog>

    <Dialog v-model="actionDialogVisible" :title="actionDialogTitle" width="520px">
      <el-form v-loading="actionSubmitLoading" :model="actionForm" label-width="110px">
        <el-form-item label="房型" prop="roomTypeId">
          <el-select v-model="actionForm.roomTypeId" class="!w-1/1" placeholder="选择房型">
            <el-option
              v-for="room in selectableRoomTypes"
              :key="room.id"
              :label="room.name"
              :value="room.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker
            v-model="actionForm.date"
            value-format="YYYY-MM-DD"
            type="date"
            class="!w-1/1"
          />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="actionForm.quantity" :min="1" class="!w-1/1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="actionSubmitLoading" @click="actionDialogVisible = false">取消</el-button>
        <el-button
          :loading="actionSubmitLoading"
          :type="inventoryActionType === 'lock' ? 'warning' : 'success'"
          @click="submitInventoryAction"
        >
          {{ actionDialogTitle }}
        </el-button>
      </template>
    </Dialog>
  </div>
</template>

<script lang="ts" setup>
import { SaasBookingApi, SaasRoomStatusVO, SaasRoomTypeVO } from '@/api/saas/booking'
import { SaasMerchantApi, SaasMerchantVO } from '@/api/saas/merchant'
import { fenToYuan, yuanToFen } from '@/utils'
import { setVisitTenantId } from '@/utils/auth'
import { dateUtil } from '@/utils/dateUtil'

defineOptions({ name: 'SaasMerchantInventory' })

type CalendarDay = {
  dateKey: string
  shortDate: string
  weekday: string
  total: number
  available: number
  locked: number
  price?: number
  isToday: boolean
  configured: boolean
  statusText: string
  tagType: 'success' | 'warning' | 'danger' | 'info'
  raw: SaasRoomStatusVO
}

const message = useMessage()
const router = useRouter()
const title = '房态管理'
const subtitle = '按房型和日期维护可售库存，支持查看可用状态、批量调整和订单占用来源。'

const loading = ref(false)
const loadingLogs = ref(false)
const loadingRoomTypes = ref(false)
const submitLoading = ref(false)
const pageError = ref('')
const merchantContext = ref<SaasMerchantVO>({})
const roomTypes = ref<SaasRoomTypeVO[]>([])
const selectedRoomTypeId = ref<number>()
const list = ref<SaasRoomStatusVO[]>([])
const logs = ref<any[]>([])
const dateRange = ref<string[]>([])
const availabilityFilter = ref<'all' | 'available' | 'tight' | 'soldOut' | 'unconfigured'>(
  'all'
)
const dialogVisible = ref(false)
type InventoryBatchForm = {
  roomTypeIds: number[]
  dateRange: string[]
  available?: number
  price?: number
}
const formData = ref<InventoryBatchForm>({ roomTypeIds: [], dateRange: [] })
const actionDialogVisible = ref(false)
const actionSubmitLoading = ref(false)
const inventoryActionType = ref<'lock' | 'unlock'>('lock')
const actionForm = ref<any>({})

const selectedRoomTypeName = computed(() => {
  return roomTypes.value.find((room) => room.id === selectedRoomTypeId.value)?.name || '全部房型'
})

const selectableRoomTypes = computed(() =>
  roomTypes.value.filter((room): room is SaasRoomTypeVO & { id: number } => room.id !== undefined)
)

const buildDateList = (range: string[]) => {
  if (!range?.[0] || !range?.[1]) return []
  const start = dateUtil(range[0]).startOf('day')
  const end = dateUtil(range[1]).startOf('day')
  if (!start.isValid() || !end.isValid() || end.isBefore(start)) return []
  const dates: string[] = []
  let cursor = start
  while (!cursor.isAfter(end) && dates.length <= 1000) {
    dates.push(cursor.format('YYYY-MM-DD'))
    cursor = cursor.add(1, 'day')
  }
  return dates
}

const batchDates = computed(() => buildDateList(formData.value.dateRange))
const batchTargetCount = computed(
  () => formData.value.roomTypeIds.length * batchDates.value.length
)
const allRoomTypesSelected = computed(
  () =>
    selectableRoomTypes.value.length > 0 &&
    formData.value.roomTypeIds.length === selectableRoomTypes.value.length
)

const actionDialogTitle = computed(() => (inventoryActionType.value === 'lock' ? '锁房' : '解锁'))

const parseDateValue = (value?: string | number[] | unknown) => {
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  if (typeof value === 'string') {
    if (/^\d{4},\d{1,2},\d{1,2}$/.test(value)) {
      const [year, month, day] = value.split(',')
      return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`
    }
    return value
  }
  return ''
}

const calendarDays = computed<CalendarDay[]>(() => {
  const roomType = roomTypes.value.find((room) => room.id === selectedRoomTypeId.value)
  if (!roomType?.id) return []
  const inventoryByDate = new Map(
    list.value.map((row) => [parseDateValue(row.date), row] as const)
  )
  return buildDateList(dateRange.value)
    .map((dateKey) => {
      const inventory = inventoryByDate.get(dateKey)
      const configured = Boolean(inventory?.id)
      const available = configured ? Number(inventory?.available || 0) : 0
      const total = configured ? Number(inventory?.total || 0) : 0
      const locked = configured ? Number(inventory?.locked || 0) : 0
      const day = dateUtil(dateKey)
      const statusText = !configured
        ? '未设置'
        : available <= 0
          ? '满房'
          : available <= 2
            ? '紧张'
            : '可售'
      const raw: SaasRoomStatusVO = inventory
        ? { ...inventory, date: dateKey }
        : {
            id: 0,
            roomTypeId: roomType.id,
            roomType: roomType.name || '-',
            total: 0,
            available: 0,
            locked: 0,
            price: roomType.initialPrice ?? -1,
            status: roomType.status,
            date: dateKey
          }
      return {
        dateKey,
        shortDate: day.isValid() ? day.format('M/D') : dateKey || '-',
        weekday: day.isValid() ? `周${'日一二三四五六'[day.day()]}` : '',
        total,
        available,
        locked,
        price: configured ? inventory?.price : roomType.initialPrice,
        isToday: day.isSame(dateUtil(), 'day'),
        configured,
        statusText,
        tagType: !configured
          ? 'info'
          : available <= 0
            ? 'danger'
            : available <= 2
              ? 'warning'
              : 'success',
        raw
      } as CalendarDay
    })
    .sort((a, b) => a.dateKey.localeCompare(b.dateKey))
})

const filteredCalendarDays = computed(() => {
  if (availabilityFilter.value === 'all') return calendarDays.value
  return calendarDays.value.filter((day) => {
    if (availabilityFilter.value === 'unconfigured') return !day.configured
    if (!day.configured) return false
    if (availabilityFilter.value === 'available') return day.available > 2
    if (availabilityFilter.value === 'tight') return day.available > 0 && day.available <= 2
    return day.available <= 0
  })
})

const formatPrice = (value?: number | string) => {
  if (value === undefined || value === null) return '-'
  const num = Number(value)
  if (!Number.isFinite(num)) return String(value)
  if (num < 0) return '未定价'
  return `¥${fenToYuan(num)}`
}

const priceToYuan = (value?: number) => {
  if (value === undefined || value === null || value < 0) return undefined
  return Number(fenToYuan(value))
}

const resetDateRange = () => {
  const start = dateUtil()
  dateRange.value = [start.format('YYYY-MM-DD'), start.add(14, 'day').format('YYYY-MM-DD')]
}

const shiftDateRange = (days: number) => {
  const start = dateUtil(dateRange.value?.[0] || undefined).add(days, 'day')
  dateRange.value = [start.format('YYYY-MM-DD'), start.add(14, 'day').format('YYYY-MM-DD')]
  loadPageData()
}

const handleTodayClick = () => {
  resetDateRange()
  loadPageData()
}

const ensureMerchantContext = async () => {
  const ctx = await SaasMerchantApi.getMerchantContext()
  if (!ctx?.id) throw new Error('当前账号未绑定商户，无法进入商户后台')
  merchantContext.value = ctx
  if (ctx.tenantId) setVisitTenantId(ctx.tenantId)
}

const loadRoomTypes = async () => {
  loadingRoomTypes.value = true
  try {
    const data = await SaasBookingApi.getRoomTypePage({ pageNo: 1, pageSize: 100 })
    roomTypes.value = data.list || []
    if (!selectedRoomTypeId.value && roomTypes.value[0]?.id) {
      selectedRoomTypeId.value = roomTypes.value[0].id
    }
  } finally {
    loadingRoomTypes.value = false
  }
}

const loadLogs = async () => {
  loadingLogs.value = true
  try {
    const data = await SaasBookingApi.getInventoryChangeLogs({
      roomTypeId: selectedRoomTypeId.value
    })
    logs.value = data.list || []
  } finally {
    loadingLogs.value = false
  }
}

const loadPageData = async () => {
  loading.value = true
  pageError.value = ''
  try {
    await ensureMerchantContext()
    if (!dateRange.value?.length) resetDateRange()
    if (!roomTypes.value.length) await loadRoomTypes()
    const params = {
      roomTypeId: selectedRoomTypeId.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    }
    list.value = await SaasBookingApi.getInventoryCalendar(params)
    await loadLogs()
  } catch (e: any) {
    pageError.value = e?.message || '房态日历加载失败，请检查后端接口'
    list.value = []
    logs.value = []
    message.error(pageError.value)
  } finally {
    loading.value = false
  }
}

const handleRoomTypeChange = () => {
  loadPageData()
}

const openEdit = (row?: SaasRoomStatusVO, configured = true) => {
  const selectedDate = parseDateValue(row?.date)
  const roomTypeId = row?.roomTypeId || selectedRoomTypeId.value
  const roomType = roomTypes.value.find((room) => room.id === roomTypeId)
  const visibleDateRange = calendarDays.value.length
    ? [calendarDays.value[0].dateKey, calendarDays.value[calendarDays.value.length - 1].dateKey]
    : [...dateRange.value]
  formData.value = {
    roomTypeIds: roomTypeId ? [roomTypeId] : [],
    dateRange: selectedDate ? [selectedDate, selectedDate] : visibleDateRange,
    available: configured ? row?.available : undefined,
    price: priceToYuan(row?.price ?? roomType?.initialPrice)
  }
  dialogVisible.value = true
}

const toggleAllRoomTypes = () => {
  formData.value.roomTypeIds = allRoomTypesSelected.value
    ? []
    : selectableRoomTypes.value.map((room) => room.id)
}

const openInventoryAction = (type: 'lock' | 'unlock') => {
  inventoryActionType.value = type
  actionForm.value = {
    roomTypeId: selectedRoomTypeId.value,
    date: dateRange.value?.[0] || dateUtil().format('YYYY-MM-DD'),
    quantity: 1
  }
  actionDialogVisible.value = true
}

const submitForm = async () => {
  if (
    !formData.value.roomTypeIds.length ||
    !batchDates.value.length ||
    formData.value.available === undefined ||
    formData.value.available === null
  ) {
    message.warning('请选择房型、日期范围并填写每日可售库存')
    return
  }
  if (batchTargetCount.value > 1000) {
    message.warning('单次批量调整最多支持 1000 条房态库存，请缩小房型或日期范围')
    return
  }
  const price = Number(formData.value.price)
  if (!Number.isFinite(price) || price <= 0) {
    message.warning('请输入大于 0 元的价格')
    return
  }
  await message.confirm(
    `确认统一调整 ${formData.value.roomTypeIds.length} 个房型、${batchDates.value.length} 天，共 ${batchTargetCount.value} 条房态库存吗？`
  )
  submitLoading.value = true
  try {
    await SaasBookingApi.updateInventoryCalendar({
      roomTypeIds: formData.value.roomTypeIds,
      dates: batchDates.value,
      available: formData.value.available,
      price: yuanToFen(price)
    })
    message.success(
      `已更新 ${formData.value.roomTypeIds.length} 个房型、${batchDates.value.length} 天，共 ${batchTargetCount.value} 条库存`
    )
    dialogVisible.value = false
    await loadPageData()
  } catch (e: any) {
    message.error(e?.message || '库存调整失败')
  } finally {
    submitLoading.value = false
  }
}

const submitInventoryAction = async () => {
  if (!actionForm.value.roomTypeId || !actionForm.value.date || !actionForm.value.quantity) {
    message.warning('请选择房型、日期和数量')
    return
  }
  await message.confirm(`确认${actionDialogTitle.value} ${actionForm.value.quantity} 间吗？`)
  actionSubmitLoading.value = true
  try {
    if (inventoryActionType.value === 'lock') {
      await SaasBookingApi.lockInventory(actionForm.value)
    } else {
      await SaasBookingApi.unlockInventory(actionForm.value)
    }
    message.success(`${actionDialogTitle.value}成功`)
    actionDialogVisible.value = false
    await loadPageData()
  } catch (e: any) {
    message.error(e?.message || `${actionDialogTitle.value}失败`)
  } finally {
    actionSubmitLoading.value = false
  }
}

onMounted(async () => {
  resetDateRange()
  await loadPageData()
})
</script>

<style lang="scss" scoped>
.saas-inventory-page {
  min-height: calc(100vh - 84px);
  margin: -12px -12px 0;
  padding: 26px;
  background: #f5f9fb;
  border-radius: 22px;
}

.saas-inventory-page__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 24px;
  color: #fff;
  background: linear-gradient(135deg, #0f172a, #2563eb);
  border-radius: 20px;

  h1 {
    margin: 8px 0;
    font-size: 30px;
    font-weight: 800;
  }

  p {
    margin: 0;
    color: rgb(255 255 255 / 78%);
  }
}

.saas-inventory-page__crumb {
  color: rgb(255 255 255 / 68%);
  font-size: 13px;
}

.saas-inventory-page__context {
  width: 280px;
  padding: 18px;
  color: #0f172a;
  background: rgb(255 255 255 / 88%);
  border-radius: 16px;

  span,
  small {
    display: block;
    color: #64748b;
  }

  strong {
    display: block;
    margin: 8px 0;
    color: #2563eb;
    font-size: 20px;
  }
}

.inventory-calendar-wrap {
  margin-bottom: 16px;
}

.inventory-calendar__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 16px;

  strong,
  span {
    display: block;
  }

  span {
    margin-top: 6px;
    color: #64748b;
    font-size: 13px;
  }
}

.inventory-calendar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.inventory-calendar__filters {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5edf5;
}

.inventory-calendar {
  display: grid;
  grid-template-columns: repeat(7, minmax(150px, 1fr));
  gap: 10px;
}

.inventory-day {
  min-height: 138px;
  padding: 14px;
  text-align: left;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 8px 18px rgb(15 23 42 / 4%);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;

  &:hover {
    border-color: #38bdf8;
    box-shadow: 0 12px 26px rgb(37 99 235 / 12%);
    transform: translateY(-1px);
  }

  &.is-today {
    border-color: #2563eb;
  }

  &.is-tight {
    background: #fffaf0;
  }

  &.is-unconfigured {
    background: #f8fafc;
    border-style: dashed;

    strong {
      color: #94a3b8;
    }
  }

  &.is-sold-out {
    background: #fff5f5;
  }

  strong {
    display: block;
    margin-top: 10px;
    color: #2563eb;
    font-size: 24px;
    line-height: 1;
  }
}

.inventory-day__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  color: #0f172a;
  font-weight: 700;

  small {
    color: #94a3b8;
    font-weight: 500;
  }
}

.inventory-day__meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
  color: #64748b;
  font-size: 12px;
}

.inventory-day__meta--unset {
  justify-content: flex-start;
  color: #94a3b8;
}

.inventory-day__price {
  display: block;
  margin-top: 8px;
  color: #64748b;
}

.inventory-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.inventory-action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.inventory-action-card {
  min-height: 128px;
  padding: 18px;
  background: #fff;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 12px;

  span {
    color: #64748b;
    font-weight: 700;
  }

  strong {
    display: block;
    margin: 10px 0 14px;
    color: #0f172a;
    font-size: 18px;
    font-weight: 800;
  }
}

.inventory-batch-room-types {
  display: flex;
  width: 100%;
  gap: 10px;
}

.inventory-batch-room-types__select {
  flex: 1;
  min-width: 0;
}

@media (max-width: 1200px) {
  .inventory-calendar {
    grid-template-columns: repeat(4, minmax(150px, 1fr));
  }
}

@media (max-width: 768px) {
  .saas-inventory-page {
    margin: 0;
    padding: 16px;
  }

  .saas-inventory-page__hero,
  .inventory-calendar__header,
  .inventory-calendar__actions,
  .inventory-bottom {
    flex-direction: column;
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .saas-inventory-page__context {
    width: 100%;
  }

  .inventory-calendar {
    grid-template-columns: 1fr;
  }

  .inventory-action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
