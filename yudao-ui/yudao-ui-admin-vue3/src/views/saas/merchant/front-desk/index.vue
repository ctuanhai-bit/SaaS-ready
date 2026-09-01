<template>
  <section class="front-desk-header">
    <div>
      <span>Hotel PMS Community / 酒店前台</span>
      <h1>前台接待</h1>
      <p>处理到店分房、入住、离店与未到店订单。</p>
    </div>
    <div class="front-desk-header__hotel">
      <small>当前酒店</small>
      <strong>{{ hotel.name || '加载中' }}</strong>
      <span>酒店编号 {{ hotel.id || '-' }}</span>
    </div>
  </section>

  <el-alert
    v-if="pageError"
    :closable="false"
    class="mb-16px"
    show-icon
    type="error"
    :title="pageError"
  />

  <ContentWrap>
    <el-form :inline="true" :model="query" class="-mb-15px">
      <el-form-item label="营业日期">
        <el-date-picker
          v-model="query.serviceDate"
          class="!w-180px"
          type="date"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input
          v-model.trim="query.keyword"
          class="!w-260px"
          clearable
          placeholder="订单号、入住人或手机号"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">
          <Icon icon="ep:search" /> 查询
        </el-button>
        <el-button @click="reset">
          <Icon icon="ep:refresh" /> 重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="activeTab" @tab-change="changeTab">
      <el-tab-pane label="今日到店" name="arrivals" />
      <el-tab-pane label="当前在住" name="inHouse" />
      <el-tab-pane label="今日离店" name="departures" />
      <el-tab-pane label="全部订单" name="search" />
    </el-tabs>

    <el-table v-loading="loading" :data="orders" stripe>
      <el-table-column label="订单号" prop="orderNo" min-width="190" />
      <el-table-column label="入住人" prop="guestName" width="120" />
      <el-table-column label="联系电话" prop="guestMobile" width="140" />
      <el-table-column label="房型" prop="roomType" min-width="180" show-overflow-tooltip />
      <el-table-column label="房号" prop="roomNo" width="100">
        <template #default="scope">{{ scope.row.roomNo || scope.row.rooms || '-' }}</template>
      </el-table-column>
      <el-table-column label="入住日期" min-width="210">
        <template #default="scope">
          {{ scope.row.checkInDate || '-' }} 至 {{ scope.row.checkOutDate || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="scope">
          <el-tag :type="statusMeta(scope.row.status).type">
            {{ statusMeta(scope.row.status).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="270" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="showDetails(scope.row)">详情</el-button>
          <el-button
            v-if="Number(scope.row.status) === 20"
            link
            type="primary"
            @click="assignRoom(scope.row)"
          >
            分房
          </el-button>
          <el-button
            v-if="Number(scope.row.status) === 20"
            link
            type="success"
            @click="checkIn(scope.row)"
          >
            入住
          </el-button>
          <el-button
            v-if="Number(scope.row.status) === 20"
            link
            type="warning"
            @click="markNoShow(scope.row)"
          >
            未到店
          </el-button>
          <el-button
            v-if="Number(scope.row.status) === 25"
            link
            type="success"
            @click="checkOut(scope.row)"
          >
            离店
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:limit="query.pageSize"
      v-model:page="query.pageNo"
      :total="total"
      @pagination="loadOrders"
    />
  </ContentWrap>

  <Dialog v-model="detailVisible" title="入住订单详情" width="640px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="订单号" :span="2">
        {{ selected?.orderNo || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="入住人">{{ selected?.guestName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">
        {{ selected?.guestMobile || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="房型">{{ selected?.roomType || '-' }}</el-descriptions-item>
      <el-descriptions-item label="房号">
        {{ selected?.roomNo || selected?.rooms || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="入住日期">
        {{ selected?.checkInDate || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="离店日期">
        {{ selected?.checkOutDate || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="房间数量">
        {{ selected?.roomQuantity || 0 }}
      </el-descriptions-item>
      <el-descriptions-item label="订单金额">
        {{ formatAmount(selected?.amount) }}
      </el-descriptions-item>
      <el-descriptions-item label="核销码" :span="2">
        {{ selected?.verificationCode || '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script lang="ts" setup>
import { SaasBookingApi, type SaasBookingOrderVO } from '@/api/saas/booking'
import { SaasMerchantApi, type SaasMerchantVO } from '@/api/saas/merchant'

defineOptions({ name: 'HotelFrontDesk' })

type FrontDeskTab = 'arrivals' | 'inHouse' | 'departures' | 'search'
type TagType = 'success' | 'warning' | 'danger' | 'info'

const message = useMessage()
const loading = ref(false)
const pageError = ref('')
const activeTab = ref<FrontDeskTab>('arrivals')
const orders = ref<SaasBookingOrderVO[]>([])
const total = ref(0)
const hotel = ref<SaasMerchantVO>({})
const detailVisible = ref(false)
const detailLoading = ref(false)
const selected = ref<SaasBookingOrderVO>()

const defaultQuery = () => ({
  pageNo: 1,
  pageSize: 10,
  serviceDate: new Date().toISOString().slice(0, 10),
  keyword: ''
})
const query = reactive(defaultQuery())

const statusMap: Record<number, { label: string; type: TagType }> = {
  5: { label: '待支付', type: 'warning' },
  10: { label: '待确认', type: 'warning' },
  12: { label: '待酒店确认', type: 'warning' },
  20: { label: '已确认', type: 'success' },
  25: { label: '已入住', type: 'success' },
  30: { label: '已取消', type: 'info' },
  31: { label: '支付超时', type: 'danger' },
  32: { label: '客户取消', type: 'info' },
  35: { label: '未到店', type: 'warning' },
  40: { label: '已完成', type: 'success' }
}

const statusMeta = (status: SaasBookingOrderVO['status']) =>
  statusMap[Number(status)] || { label: `状态 ${status}`, type: 'info' as TagType }

const requestByTab = (params: Record<string, unknown>) => {
  if (activeTab.value === 'arrivals') return SaasBookingApi.getFrontDeskArrivals(params)
  if (activeTab.value === 'inHouse') return SaasBookingApi.getFrontDeskInHouse(params)
  if (activeTab.value === 'departures') return SaasBookingApi.getFrontDeskDepartures(params)
  return SaasBookingApi.searchFrontDeskOrders(params)
}

const loadHotel = async () => {
  if (hotel.value.id) return
  hotel.value = await SaasMerchantApi.getMerchantContext()
}

const loadOrders = async () => {
  loading.value = true
  pageError.value = ''
  try {
    await loadHotel()
    const result = await requestByTab({ ...query })
    orders.value = result.list
    total.value = result.total
  } catch (error: any) {
    pageError.value = error?.message || '前台订单加载失败，请检查服务状态'
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.pageNo = 1
  loadOrders()
}

const reset = () => {
  Object.assign(query, defaultQuery())
  loadOrders()
}

const changeTab = () => {
  query.pageNo = 1
  loadOrders()
}

const showDetails = async (row: SaasBookingOrderVO) => {
  detailVisible.value = true
  detailLoading.value = true
  selected.value = row
  try {
    selected.value = await SaasBookingApi.getOrder(row.id)
  } catch (error: any) {
    message.error(error?.message || '订单详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const runAction = async (prompt: string, action: () => Promise<unknown>, success: string) => {
  try {
    await message.confirm(prompt)
    await action()
    message.success(success)
    await loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') message.error(error?.message || '操作失败')
  }
}

const assignRoom = async (row: SaasBookingOrderVO) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入房号', '分房', {
      inputPattern: /\S+/,
      inputErrorMessage: '房号不能为空'
    })
    await SaasBookingApi.assignFrontDeskRoom(row.id, { roomNo: value })
    message.success('分房成功')
    await loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') message.error(error?.message || '分房失败')
  }
}

const checkIn = (row: SaasBookingOrderVO) =>
  runAction('确认办理入住吗？', () => SaasBookingApi.checkInFrontDeskOrder(row.id), '入住成功')

const checkOut = (row: SaasBookingOrderVO) =>
  runAction('确认办理离店吗？', () => SaasBookingApi.checkOutOrder(row.id), '离店成功')

const markNoShow = async (row: SaasBookingOrderVO) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入未到店原因', '标记未到店', {
      inputPattern: /\S+/,
      inputErrorMessage: '原因不能为空'
    })
    await SaasBookingApi.noShowFrontDeskOrder(row.id, value)
    message.success('已标记未到店')
    await loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') message.error(error?.message || '操作失败')
  }
}

const formatAmount = (amount?: number) => {
  const value = Number(amount)
  return Number.isFinite(value) ? `¥${(value / 100).toFixed(2)}` : '-'
}

onMounted(loadOrders)
</script>

<style lang="scss" scoped>
.front-desk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
  padding: 24px 28px;
  color: #f8fafc;
  background: #164e3f;
  border-bottom: 3px solid #c7a34b;
  border-radius: 6px;

  span,
  p,
  small {
    color: #d7e5df;
  }

  h1 {
    margin: 8px 0;
    font-size: 30px;
  }

  p {
    margin: 0;
  }
}

.front-desk-header__hotel {
  width: min(300px, 100%);
  padding: 16px;
  color: #17382f;
  background: #f8faf9;
  border-radius: 6px;

  small,
  span,
  strong {
    display: block;
  }

  small,
  span {
    color: #64748b;
  }

  strong {
    margin: 6px 0;
    color: #0f766e;
    font-size: 18px;
  }
}

@media (max-width: 768px) {
  .front-desk-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
