<template>
  <section class="hotel-orders__header">
    <div>
      <span>Hotel PMS Community / 酒店订单</span>
      <h1>酒店订单</h1>
      <p>查询住宿订单并处理确认、入住、离店和取消。</p>
    </div>
  </section>

  <ContentWrap>
    <el-alert
      class="!mb-16px"
      :closable="false"
      show-icon
      title="线下收款模式：请在实际收款或退款完成后再登记订单状态"
      type="warning"
    />
    <el-form :inline="true" :model="query" class="-mb-15px">
      <el-form-item label="关键词">
        <el-input
          v-model.trim="query.keyword"
          class="!w-260px"
          clearable
          placeholder="订单号、入住人或手机号"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" class="!w-160px" clearable placeholder="全部状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search"><Icon icon="ep:search" /> 查询</el-button>
        <el-button @click="reset"><Icon icon="ep:refresh" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="orders" stripe>
      <el-table-column label="订单号" prop="orderNo" min-width="190" />
      <el-table-column label="入住人" prop="guestName" min-width="110" />
      <el-table-column label="房型" prop="roomType" min-width="180" show-overflow-tooltip />
      <el-table-column label="入住日期" min-width="210">
        <template #default="scope">{{ scope.row.checkInDate }} 至 {{ scope.row.checkOutDate || '-' }}</template>
      </el-table-column>
      <el-table-column label="间数" prop="roomQuantity" width="80" align="center" />
      <el-table-column label="金额" width="120" align="right">
        <template #default="scope">{{ formatAmount(scope.row.amount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120" align="center">
        <template #default="scope">
          <el-tag :type="statusMeta(scope.row.status).type">{{ statusMeta(scope.row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="showDetails(scope.row)">详情</el-button>
          <el-button v-if="Number(scope.row.status) === 5" link type="success" @click="markPaid(scope.row)">登记收款</el-button>
          <el-button v-if="[10, 12].includes(Number(scope.row.status))" link type="success" @click="confirm(scope.row)">确认</el-button>
          <el-button v-if="Number(scope.row.status) === 20" link type="success" @click="checkIn(scope.row)">入住</el-button>
          <el-button v-if="Number(scope.row.status) === 25" link type="success" @click="complete(scope.row)">离店</el-button>
          <el-button v-if="Number(scope.row.status) === 50" link type="warning" @click="markRefunded(scope.row)">登记退款</el-button>
          <el-button v-if="[5, 10, 12, 20].includes(Number(scope.row.status))" link type="danger" @click="cancel(scope.row)">取消</el-button>
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

  <Dialog v-model="detailVisible" title="订单详情" width="640px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="订单号" :span="2">{{ selected?.orderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="入住人">{{ selected?.guestName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ selected?.guestMobile || '-' }}</el-descriptions-item>
      <el-descriptions-item label="房型">{{ selected?.roomType || '-' }}</el-descriptions-item>
      <el-descriptions-item label="房间数量">{{ selected?.roomQuantity || 0 }}</el-descriptions-item>
      <el-descriptions-item label="入住日期">{{ selected?.checkInDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="离店日期">{{ selected?.checkOutDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="订单金额">{{ formatAmount(selected?.amount) }}</el-descriptions-item>
      <el-descriptions-item label="核销码">{{ selected?.verificationCode || '-' }}</el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script lang="ts" setup>
import { type SaasBookingOrderVO, SaasBookingApi } from '@/api/saas/booking'

defineOptions({ name: 'HotelOrders' })

const message = useMessage()
const loading = ref(false)
const orders = ref<SaasBookingOrderVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const selected = ref<SaasBookingOrderVO>()
const query = reactive({ pageNo: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })

const statusOptions = [
  { value: 5, label: '待支付' },
  { value: 10, label: '待确认' },
  { value: 12, label: '待酒店确认' },
  { value: 20, label: '已确认' },
  { value: 25, label: '已入住' },
  { value: 30, label: '已取消' },
  { value: 35, label: '未到店' },
  { value: 40, label: '已完成' },
  { value: 50, label: '退款处理中' },
  { value: 60, label: '已退款' }
]

const statusMeta = (status: number | string) => {
  const value = Number(status)
  const label = statusOptions.find((item) => item.value === value)?.label || `状态 ${value}`
  const type = value === 40 ? 'success' : [30, 31, 32, 35, 60, 70].includes(value) ? 'info' : 'warning'
  return { label, type: type as 'success' | 'warning' | 'info' }
}

const formatAmount = (fen?: number) => (fen == null ? '-' : `¥${(fen / 100).toFixed(2)}`)

const loadOrders = async () => {
  loading.value = true
  try {
    const data = await SaasBookingApi.getMerchantOrderPage(query)
    orders.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.pageNo = 1
  loadOrders()
}

const reset = () => {
  query.keyword = ''
  query.status = undefined
  search()
}

const showDetails = async (row: SaasBookingOrderVO) => {
  selected.value = await SaasBookingApi.getOrder(row.id)
  detailVisible.value = true
}

const runAction = async (question: string, action: () => Promise<unknown>) => {
  await message.confirm(question)
  await action()
  message.success('订单状态已更新')
  await loadOrders()
}

const confirm = (row: SaasBookingOrderVO) => runAction('确认接受该订单吗？', () => SaasBookingApi.confirmOrder(row.id))
const markPaid = (row: SaasBookingOrderVO) =>
  runAction('确认线下款项已经实际到账吗？此操作不会调用支付渠道。', () =>
    SaasBookingApi.markOrderPaidManually(row.id)
  )
const checkIn = (row: SaasBookingOrderVO) => runAction('确认为该订单办理入住吗？', () => SaasBookingApi.checkInOrder(row.id))
const complete = (row: SaasBookingOrderVO) => runAction('确认为该订单办理离店吗？', () => SaasBookingApi.completeOrder(row.id))
const markRefunded = (row: SaasBookingOrderVO) =>
  runAction('确认线下退款已经实际完成吗？此操作不会调用支付渠道。', () =>
    SaasBookingApi.markOrderRefundedManually(row.id)
  )
const cancel = (row: SaasBookingOrderVO) => runAction('确认取消该订单并释放库存吗？', () => SaasBookingApi.cancelOrder(row.id))

onMounted(loadOrders)
</script>

<style scoped>
.hotel-orders__header {
  padding: 24px;
  margin-bottom: 16px;
  color: #fff;
  background: #153f35;
  border-bottom: 3px solid #c9a14a;
  border-radius: 6px;
}

.hotel-orders__header span,
.hotel-orders__header p {
  margin: 0;
  color: #cbded8;
}

.hotel-orders__header h1 {
  margin: 8px 0;
  font-size: 28px;
  letter-spacing: 0;
}
</style>
