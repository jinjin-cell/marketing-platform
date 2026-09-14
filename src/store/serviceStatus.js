import { defineStore } from 'pinia'

export const useServiceStatusStore = defineStore('serviceStatus', {
  state: () => ({
    degraded: false,
    message: ''
  }),
  actions: {
    markDegraded(message) {
      this.degraded = true
      this.message = message || '抽奖服务暂不可用，请稍后重试'
    },
    clearDegraded() {
      this.degraded = false
      this.message = ''
    }
  }
})
