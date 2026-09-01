import { Layout } from '@/utils/routerHelper'

const menuMeta = {
  hidden: false,
  canTo: true,
  noTagsView: false,
  noCache: true
}

const hotelPmsRouter: AppRouteRecordRaw[] = [
  {
    path: '/hotel',
    component: Layout,
    redirect: '/hotel/front-desk',
    name: 'HotelPms',
    meta: {
      ...menuMeta,
      title: '酒店管理',
      icon: 'ep:house',
      alwaysShow: true
    },
    children: [
      {
        path: 'front-desk',
        component: () => import('@/views/saas/merchant/front-desk/index.vue'),
        name: 'HotelFrontDesk',
        meta: { ...menuMeta, title: '酒店前台', icon: 'ep:service' }
      },
      {
        path: 'orders',
        component: () => import('@/views/saas/merchant/order/index.vue'),
        name: 'HotelOrders',
        meta: { ...menuMeta, title: '酒店订单', icon: 'ep:list' }
      },
      {
        path: 'room-types',
        component: () => import('@/views/saas/merchant/room/index.vue'),
        name: 'HotelRoomTypes',
        meta: { ...menuMeta, title: '房型管理', icon: 'ep:house' }
      },
      {
        path: 'inventory',
        component: () => import('@/views/saas/merchant/inventory/index.vue'),
        name: 'HotelInventory',
        meta: { ...menuMeta, title: '房态与库存', icon: 'ep:calendar' }
      },
      {
        path: 'profile',
        component: () => import('@/views/saas/merchant/profile/index.vue'),
        name: 'HotelProfile',
        meta: { ...menuMeta, title: '酒店资料', icon: 'ep:location' }
      },
      {
        path: 'staff',
        component: () => import('@/views/saas/merchant/staff/index.vue'),
        name: 'HotelStaff',
        meta: { ...menuMeta, title: '员工管理', icon: 'ep:user-filled' }
      }
    ]
  }
]

export default hotelPmsRouter
