<template>
  <div class="page">
    <UserBar />

    <div class="card summary-card">
      <span class="summary-label">当前积分余额</span>
      <span class="summary-value">{{ creditText }}</span>
    </div>

    <div class="card">
      <div class="section-title">积分明细</div>
      <div class="section-sub">签到返利、兑换抽奖等所有积分变动</div>
    </div>

    <div v-if="loading" class="card loading-card">加载中…</div>
    <div v-else-if="error" class="card loading-card error-state">
      <span>{{ error }}</span>
      <el-button size="small" @click="loadRecords">重试</el-button>
    </div>
    <div v-else-if="!records.length" class="card loading-card">
      暂无积分流水，去<router-link to="/sign" class="link">签到</router-link>赚积分吧
    </div>

    <div v-for="(r, i) in records" :key="r.orderId || i" class="card record-card">
      <div class="record-info">
        <div class="record-title">{{ r.tradeName || '积分变动' }}</div>
        <div class="record-time">{{ r.createTime }} · {{ r.tradeTypeDesc || r.tradeType }}</div>
      </div>
      <div class="record-amount" :class="amountClass(r)">{{ amountText(r) }}</div>
    </div>

    <BottomNav />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import UserBar from '../components/UserBar.vue'
import BottomNav from '../components/BottomNav.vue'
import { queryUserCreditOrderList } from '../api/raffle'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

const records = ref([])
const loading = ref(true)
const error = ref('')

const creditText = computed(() =>
  userStore.credit === null ? '--' : formatAmount(userStore.credit)
)

function formatAmount(amount) {
  const value = Number(amount || 0)
  return Number.isInteger(value) ? String(value) : value.toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
}

function amountText(record) {
  const amount = Number(record.tradeAmount || 0)
  const text = Math.abs(amount).toFixed(amount % 1 === 0 ? 0 : 2)
  return amount < 0 ? `-${text}` : `+${text}`
}

function amountClass(record) {
  return Number(record.tradeAmount || 0) < 0 ? 'minus' : 'plus'
}

async function loadRecords() {
  loading.value = true
  error.value = ''
  userStore.refreshCredit().catch(() => {})
  try {
    const result = await queryUserCreditOrderList(userStore.userId, 50)
    records.value = Array.isArray(result) ? result : []
  } catch (e) {
    error.value = '积分明细加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadRecords)
</script>

<style scoped>
.summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-label {
  font-size: 14px;
  color: #999;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--brand-red);
}

.section-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

.loading-card {
  text-align: center;
  color: #999;
  font-size: 13px;
}

.error-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #b42318;
}

.link {
  color: var(--brand-red);
  text-decoration: none;
  font-weight: 600;
}

.record-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.record-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--brand-brown);
}

.record-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.record-amount {
  font-size: 17px;
  font-weight: 700;
}

.record-amount.plus {
  color: var(--brand-red);
}

.record-amount.minus {
  color: #4a9d5f;
}
</style>
