import { defineStore } from 'pinia'
import { queryUserActivityAccount, queryUserCredit } from '../api/raffle'

export const ACTIVITY_ID = Number(import.meta.env.VITE_ACTIVITY_ID || 100301)

// 进行中的刷新请求；同页多个组件并发刷新时共享一次接口调用
let accountPromise = null
let creditPromise = null

export const useUserStore = defineStore('user', {
  state: () => ({
    userId: localStorage.getItem('userId') || '',
    credit: null,
    account: {
      totalCount: 0,
      totalCountSurplus: 0,
      dayCount: 0,
      dayCountSurplus: 0,
      monthCount: 0,
      monthCountSurplus: 0
    }
  }),
  getters: {
    isLogin: (state) => Boolean(state.userId),
    // 可用抽奖次数取三者剩余的最小值
    usableCount: (state) =>
      Math.min(
        state.account.totalCountSurplus,
        state.account.dayCountSurplus,
        state.account.monthCountSurplus
      )
  },
  actions: {
    login(userId) {
      this.userId = userId
      localStorage.setItem('userId', userId)
    },
    logout() {
      this.userId = ''
      this.credit = null
      localStorage.removeItem('userId')
    },
    async refreshAccount() {
      if (!accountPromise) {
        accountPromise = queryUserActivityAccount(this.userId, ACTIVITY_ID)
          .then((account) => {
            this.account = account
          })
          .finally(() => {
            accountPromise = null
          })
      }
      return accountPromise
    },
    async refreshCredit() {
      if (!creditPromise) {
        creditPromise = queryUserCredit(this.userId)
          .then((credit) => {
            this.credit = credit
          })
          .finally(() => {
            creditPromise = null
          })
      }
      return creditPromise
    }
  }
})
