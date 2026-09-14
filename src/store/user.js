import { defineStore } from 'pinia'
import { queryActivityList, queryUserActivityAccount, queryUserCredit } from '../api/raffle'

// 兜底活动ID：活动列表接口拿不到数据时使用（可用 VITE_ACTIVITY_ID 覆盖）
export const DEFAULT_ACTIVITY_ID = Number(import.meta.env.VITE_ACTIVITY_ID || 100301)

const ACTIVITY_STORAGE_KEY = 'activityId'

const emptyAccount = () => ({
  totalCount: 0,
  totalCountSurplus: 0,
  dayCount: 0,
  dayCountSurplus: 0,
  monthCount: 0,
  monthCountSurplus: 0
})

// 请求按用户和活动键去重，响应落库前再次校验上下文，防止切换账号后旧请求覆盖新数据。
let accountRequest = null
let creditRequest = null
let activityPromise = null
let accountVersion = 0
let creditVersion = 0

export const useUserStore = defineStore('user', {
  state: () => ({
    userId: localStorage.getItem('userId') || '',
    accountName: localStorage.getItem('accountName') || '',
    credit: null,
    // 当前活动：优先取本地记录，其次兜底活动
    activityId: Number(localStorage.getItem(ACTIVITY_STORAGE_KEY)) || DEFAULT_ACTIVITY_ID,
    // 后端配置的活动列表（多活动切换用）
    activityList: [],
    account: emptyAccount()
  }),
  getters: {
    isLogin: (state) => Boolean(state.userId),
    // 可用抽奖次数取三者剩余的最小值
    usableCount: (state) =>
      Math.min(
        state.account.totalCountSurplus,
        state.account.dayCountSurplus,
        state.account.monthCountSurplus
      ),
    currentActivity: (state) =>
      state.activityList.find((item) => item.activityId === state.activityId) || null
  },
  actions: {
    login(session) {
      const normalizedUserId = String(session?.userId || '').trim()
      const accountName = String(session?.accountName || '').trim()
      const accessToken = String(session?.accessToken || '')
      if (!normalizedUserId || !accountName || !accessToken) {
        throw new Error('登录响应缺少必要字段')
      }
      accountVersion++
      creditVersion++
      accountRequest = null
      creditRequest = null
      this.userId = normalizedUserId
      this.accountName = accountName
      this.credit = null
      this.account = emptyAccount()
      localStorage.setItem('userId', normalizedUserId)
      localStorage.setItem('accountName', accountName)
      localStorage.setItem('accessToken', accessToken)
    },
    logout() {
      accountVersion++
      creditVersion++
      accountRequest = null
      creditRequest = null
      this.userId = ''
      this.accountName = ''
      this.credit = null
      this.account = emptyAccount()
      this.activityList = []
      localStorage.removeItem('userId')
      localStorage.removeItem('accountName')
      localStorage.removeItem('accessToken')
    },
    /** 加载活动列表；本地记录的活动已不存在时回落到第一个活动 */
    async loadActivities() {
      if (!activityPromise) {
        activityPromise = queryActivityList()
          .then((list) => {
            this.activityList = Array.isArray(list) ? list : []
            if (
              this.activityList.length &&
              !this.activityList.some((item) => item.activityId === this.activityId)
            ) {
              this.switchActivity(this.activityList[0].activityId)
            }
          })
          .catch(() => {
            // 接口失败时保持兜底活动，不阻塞页面
          })
          .finally(() => {
            activityPromise = null
          })
      }
      return activityPromise
    },
    /** 切换活动：额度按新的活动重新拉取 */
    switchActivity(activityId) {
      const id = Number(activityId)
      if (!id || id === this.activityId) return
      accountVersion++
      accountRequest = null
      this.activityId = id
      this.account = emptyAccount()
      localStorage.setItem(ACTIVITY_STORAGE_KEY, String(id))
      this.refreshAccount(true).catch(() => {})
    },
    async refreshAccount(force = false) {
      const userId = this.userId
      const activityId = this.activityId
      if (!userId || !activityId) {
        this.account = emptyAccount()
        return null
      }
      const key = `${userId}:${activityId}`
      if (force) {
        accountVersion++
        accountRequest = null
      }
      if (!accountRequest || accountRequest.key !== key) {
        const version = accountVersion
        const promise = queryUserActivityAccount(userId, activityId)
          .then((account) => {
            if (version === accountVersion && this.userId === userId && this.activityId === activityId) {
              this.account = { ...emptyAccount(), ...(account || {}) }
            }
            return account
          })
          .finally(() => {
            if (accountRequest?.promise === promise) accountRequest = null
          })
        accountRequest = { key, promise }
      }
      return accountRequest.promise
    },
    async refreshCredit(force = false) {
      const userId = this.userId
      if (!userId) {
        this.credit = null
        return null
      }
      if (force) {
        creditVersion++
        creditRequest = null
      }
      if (!creditRequest || creditRequest.key !== userId) {
        const version = creditVersion
        const promise = queryUserCredit(userId)
          .then((credit) => {
            if (version === creditVersion && this.userId === userId) {
              this.credit = credit
            }
            return credit
          })
          .finally(() => {
            if (creditRequest?.promise === promise) creditRequest = null
          })
        creditRequest = { key: userId, promise }
      }
      return creditRequest.promise
    }
  }
})
