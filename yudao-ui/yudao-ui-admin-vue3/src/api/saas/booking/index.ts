import request from '@/config/axios'
import type { SaasOrderStatus, SaasPageResult } from '@/api/saas/common'

export interface SaasBookingOrderVO {
  id: number
  orderNo?: string
  tenantId?: number
  merchantId: string | number
  merchantName?: string
  guestName: string
  guestMobile?: string
  roomType?: string
  roomTypeId?: number
  status: SaasOrderStatus
  amount?: number
  checkInDate: string
  checkOutDate?: string
  roomQuantity?: number
  roomNo?: string
  rooms?: string
  verificationCode?: string
}

export interface SaasRoomStatusVO {
  id: number
  tenantId?: number
  merchantId?: number
  roomTypeId?: number
  roomType: string
  total: number
  available: number
  locked: number
  price: number
  maxOccupancy?: number
  status?: number
  date?: string
}

export interface SaasRoomTypeVO {
  id?: number
  tenantId?: number
  merchantId?: number
  name?: string
  maxOccupancy?: number
  areaSqm?: number
  areaSqmMin?: number
  areaSqmMax?: number
  breakfastIncluded?: boolean
  initialPrice?: number
  status?: number
  coverUrl?: string
  imageUrls?: string
  facilityCodes?: string
}

export interface SaasRoomTypePageReqVO extends PageParam {
  merchantId?: number
  name?: string
  status?: number
}

export interface SaasRoomTypeCreateReqVO {
  name?: string
  maxOccupancy?: number
  areaSqm?: number
  areaSqmMin?: number
  areaSqmMax?: number
  breakfastIncluded?: boolean
  initialPrice?: number
  status?: number
  coverUrl?: string
  imageUrls?: string
  facilityCodes?: string
}

export interface SaasRoomTypeUpdateReqVO extends SaasRoomTypeCreateReqVO {
  id?: number
}

export interface SaasInventoryCalendarReqVO {
  roomTypeId?: number
  startDate?: string
  endDate?: string
}

export interface SaasInventoryUpdateReqVO {
  roomTypeId?: number
  roomTypeIds?: number[]
  dates?: string[]
  date?: string
  price?: number
  available?: number
  status?: number
}

export const SaasBookingApi = {
  getOrderPage: async (
    params?: Record<string, unknown>
  ): Promise<SaasPageResult<SaasBookingOrderVO>> => {
    const res = await request.get({
      url: '/booking/order/page',
      params: { pageNo: 1, pageSize: 10, ...params }
    })
    return { list: res.list ?? [], total: res.total ?? 0 }
  },
  getMerchantOrderPage: async (
    params?: Record<string, unknown>
  ): Promise<SaasPageResult<SaasBookingOrderVO>> => {
    const res = await request.get({
      url: '/booking/order/page',
      params: { pageNo: 1, pageSize: 10, ...params }
    })
    return { list: res.list ?? [], total: res.total ?? 0 }
  },
  getOrder: async (id: number): Promise<SaasBookingOrderVO> => {
    return await request.get({ url: '/booking/order/get?id=' + id })
  },
  getRoomStatusList: async (params?: SaasRoomTypePageReqVO): Promise<SaasRoomStatusVO[]> => {
    const res = await request.get({
      url: '/booking/room-type/page',
      params: { pageNo: 1, pageSize: 20, ...params }
    })
    return (res.list ?? []).map((room: SaasRoomTypeVO) => ({
      id: room.id!,
      tenantId: room.tenantId,
      merchantId: room.merchantId,
      roomType: room.name || '-',
      total: room.maxOccupancy || 0,
      available: -1, // -1 表示需通过 inventory/calendar 获取库存
      locked: -1,
      price: -1, // -1 表示列表接口未返回价格
      maxOccupancy: room.maxOccupancy,
      status: room.status
    }))
  },
  getRoomTypePage: async (params: SaasRoomTypePageReqVO) => {
    return await request.get({ url: '/booking/room-type/page', params })
  },
  getSharedFacilityOptions: async (): Promise<string[]> => {
    return await request.get({ url: '/booking/room-type/facility-options' })
  },
  getRoomType: async (id: number) => {
    return await request.get({ url: '/booking/room-type/get?id=' + id })
  },
  createRoomType: async (data: SaasRoomTypeCreateReqVO) => {
    return await request.post({ url: '/booking/room-type/create', data })
  },
  updateRoomType: async (data: SaasRoomTypeUpdateReqVO) => {
    return await request.put({ url: '/booking/room-type/update', data })
  },
  deleteRoomType: async (id: number) => {
    return await request.delete({ url: '/booking/room-type/delete?id=' + id })
  },
  updateRoomTypeStatus: async (id: number, status: number) => {
    return await request.put({ url: '/booking/room-type/status', data: { id, status } })
  },
  getInventoryCalendar: async (
    params?: SaasInventoryCalendarReqVO
  ): Promise<SaasRoomStatusVO[]> => {
    const res = await request.get({ url: '/booking/inventory/calendar', params })
    return Array.isArray(res) ? res : (res?.list ?? [])
  },
  updateInventoryCalendar: async (data: SaasInventoryUpdateReqVO) => {
    return await request.put({ url: '/booking/inventory/calendar', data })
  },
  lockInventory: async (data: { roomTypeId?: number; date?: string; quantity?: number }) => {
    return await request.post({ url: '/booking/inventory/lock', data })
  },
  unlockInventory: async (data: { roomTypeId?: number; date?: string; quantity?: number }) => {
    return await request.post({ url: '/booking/inventory/unlock', data })
  },
  getInventoryChangeLogs: async (params?: Record<string, unknown>) => {
    const res = await request.get({
      url: '/booking/inventory/change-logs',
      params: { pageNo: 1, pageSize: 10, ...params }
    })
    return { list: res.list ?? [], total: res.total ?? 0 }
  },
  confirmOrder: async (id: number) => {
    return await request.post({ url: '/booking/order/confirm', data: { id } })
  },
  markOrderPaidManually: async (id: number) => {
    return await request.post({ url: '/booking/order/manual-paid', data: { id } })
  },
  markOrderRefundedManually: async (id: number) => {
    return await request.post({ url: '/booking/order/manual-refunded', data: { id } })
  },
  cancelOrder: async (id: number) => {
    return await request.put({ url: '/booking/order/cancel?id=' + id })
  },
  checkInOrder: async (id: number) => {
    return await request.post({ url: '/booking/order/check-in', data: { id } })
  },
  checkOutOrder: async (id: number) => {
    return await request.post({ url: '/booking/front-desk/orders/' + id + '/check-out' })
  },
  completeOrder: async (id: number) => {
    return await request.put({ url: '/booking/order/complete?id=' + id })
  },
  getFrontDeskArrivals: async (
    params?: Record<string, unknown>
  ): Promise<SaasPageResult<SaasBookingOrderVO>> => {
    const res = await request.get({
      url: '/booking/front-desk/arrivals',
      params: { pageNo: 1, pageSize: 10, ...params }
    })
    return { list: res.list ?? [], total: res.total ?? 0 }
  },
  getFrontDeskInHouse: async (
    params?: Record<string, unknown>
  ): Promise<SaasPageResult<SaasBookingOrderVO>> => {
    const res = await request.get({
      url: '/booking/front-desk/in-house',
      params: { pageNo: 1, pageSize: 10, ...params }
    })
    return { list: res.list ?? [], total: res.total ?? 0 }
  },
  getFrontDeskDepartures: async (
    params?: Record<string, unknown>
  ): Promise<SaasPageResult<SaasBookingOrderVO>> => {
    const res = await request.get({
      url: '/booking/front-desk/departures',
      params: { pageNo: 1, pageSize: 10, ...params }
    })
    return { list: res.list ?? [], total: res.total ?? 0 }
  },
  searchFrontDeskOrders: async (
    params?: Record<string, unknown>
  ): Promise<SaasPageResult<SaasBookingOrderVO>> => {
    const res = await request.get({
      url: '/booking/front-desk/orders',
      params: { pageNo: 1, pageSize: 10, ...params }
    })
    return { list: res.list ?? [], total: res.total ?? 0 }
  },
  assignFrontDeskRoom: async (id: number, data: Record<string, unknown>) => {
    return await request.post({ url: '/booking/front-desk/orders/' + id + '/assign-room', data })
  },
  checkInFrontDeskOrder: async (id: number) => {
    return await request.post({ url: '/booking/front-desk/orders/' + id + '/check-in' })
  },
  noShowFrontDeskOrder: async (id: number, reason: string) => {
    return await request.post({
      url: '/booking/front-desk/orders/' + id + '/no-show',
      data: { reason }
    })
  }
}
