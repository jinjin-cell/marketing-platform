import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: '幸运大转盘' } },
  { path: '/sign', name: 'Sign', component: () => import('../views/Sign.vue'), meta: { title: '每日签到' } },
  { path: '/exchange', name: 'Exchange', component: () => import('../views/Exchange.vue'), meta: { title: '积分兑换' } },
  { path: '/my', name: 'My', component: () => import('../views/My.vue'), meta: { title: '我的' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 大营销抽奖平台` : '大营销抽奖平台'
  const userId = localStorage.getItem('userId')
  if (!userId && to.path !== '/login') {
    return '/login'
  }
  if (userId && to.path === '/login') {
    return '/'
  }
  return true
})

export default router
