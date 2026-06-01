import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/recommend',
    name: 'Recommend',
    component: () => import('@/views/Recommend.vue'),
    meta: { title: '智能推荐' }
  },
  {
    path: '/route-plan',
    name: 'RoutePlan',
    component: () => import('@/views/RoutePlan.vue'),
    meta: { title: '路线规划' }
  },
  {
    path: '/itinerary',
    name: 'Itinerary',
    component: () => import('@/views/Itinerary.vue'),
    meta: { title: '旅行计划' }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/AdminDashboard.vue'),
    meta: { title: '管理后台', requiresAuth: true }
  },
  {
    path: '/admin/spots',
    name: 'AdminSpotManage',
    component: () => import('@/views/AdminSpotManage.vue'),
    meta: { title: '景点管理', requiresAuth: true }
  },
  {
    path: '/admin/route-graph',
    name: 'RouteGraphEditor',
    component: () => import('@/views/RouteGraphEditor.vue'),
    meta: { title: '路网标定', requiresAuth: true }
  },
  {
    path: '/admin/reports',
    name: 'AdminReportManage',
    component: () => import('@/views/AdminReportManage.vue'),
    meta: { title: '内容审核', requiresAuth: true }
  },
  {
    path: '/admin/logs',
    name: 'AdminLogManage',
    component: () => import('@/views/AdminLogManage.vue'),
    meta: { title: '日志治理', requiresAuth: true }
  },
  {
    path: '/admin/circles',
    name: 'AdminCircleManage',
    component: () => import('@/views/AdminCircleManage.vue'),
    meta: { title: '圈子管理', requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/Search.vue'),
    meta: { title: '设施查询' }
  },
  {
    path: '/diary',
    name: 'Diary',
    component: () => import('@/views/Diary.vue'),
    meta: { title: '旅行日记' }
  },
  {
    path: '/log/:id',
    name: 'LogDetail',
    component: () => import('@/views/LogDetail.vue'),
    meta: { title: '日志详情' }
  },
  {
    path: '/food',
    name: 'Food',
    component: () => import('@/views/Food.vue'),
    meta: { title: '美食推荐' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/spot/:id',
    name: 'SpotDetail',
    component: () => import('@/views/SpotDetail.vue'),
    meta: { title: '景点详情' }
  },
  {
    path: '/food/:id',
    name: 'FoodDetail',
    component: () => import('@/views/FoodDetail.vue'),
    meta: { title: '美食详情' }
  },
  {
    path: '/user/:userId',
    name: 'UserHome',
    component: () => import('@/views/UserHome.vue'),
    meta: { title: '个人主页', requiresAuth: true }
  },
  {
    path: '/circle',
    name: 'Circle',
    component: () => import('@/views/Circle.vue'),
    meta: { title: '圈子', requiresAuth: true }
  },
  {
    path: '/circle/:id',
    name: 'CircleDetail',
    component: () => import('@/views/CircleDetail.vue'),
    meta: { title: '圈子详情', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - 个性化旅游系统`
  }
  
  // 检查是否需要登录
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    ElMessage.warning('请先登录')
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
