<template>
  <div class="page">
    <UserBar />

    <div class="card">
      <div class="section-title">我的订单</div>
      <div class="section-sub">积分兑换获得的抽奖次数，按兑换时间倒序</div>
    </div>

    <div v-if="loading" class="card loading-card">加载中…</div>
    <div v-else-if="error" class="card loading-card error-state">
      <span>{{ error }}</span>
      <el-button size="small" @click="loadRecords">重试</el-button>
    </div>
    <div v-else-if="!records.length" class="card loading-card">
      暂无兑换记录，去<router-link to="/exchange" class="link">积分兑换</router-link>换点抽奖次数吧
    </div>

    <div v-for="(o, i) in records" :key="o.orderId || i" class="card record-card">
      <div class="record-info">
        <div class="record-title">{{ o.activityName || `活动 ${o.activityId}` }}</div>
        <div class="record-sub">
          SKU {{ o.sku }} · 次数 +{{ o.totalCount }}（日 {{ o.dayCount }} / 月 {{ o.monthCount }}）
        </div>
        <div class="record-time">{{ o.orderTime }}</div>
        <div class="record-order">订单号 {{ o.orderId }}</div>
      </div>
      <div class="record-right">
        <div class="record-amount">-{{ formatAmount(o.payAmount) }} 积分</div>
        <el-tag size="small" :type="stateTagType(o.state)" effect="plain" round>
          {{ o.stateDesc || o.state }}
        </el-tag>
      </div>
    </div>

    <BottomNav />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import UserBar from '../components/UserBar.vue'
import BottomNav from '../components/BottomNav.vue'
import { queryUserActivityOrderList } from '../api/raffle'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

const records = ref([])
const loading = ref(true)
const error = ref('')

const STATE_TAG = {
  completed: 'success',
  wait_pay: 'warning',
  expired: 'info'
}

function stateTagType(state) {
  return STATE_TAG[state] || 'info'
}

function formatAmount(amount) {
  const value = Number(amount || 0)
  return value.toFixed(value % 1 === 0 ? 0 : 2)
}

async function loadRecords() {
  loading.value = true
  error.value = ''
  try {
    const result = await queryUserActivityOrderList(userStore.userId)
    records.value = Array.isArray(result) ? result : []
  } catch (e) {
    error.value = '订单记录加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadRecords)
</script>

<style scoped>
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
  gap: 12px;
}

.record-info {
  flex: 1;
  min-width: 0;
}

.record-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--brand-brown);
}

.record-sub {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.record-time,
.record-order {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  word-break: break-all;
}

.record-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.record-amount {
  font-size: 14px;
  font-weight: 700;
  color: #4a9d5f;
}
</style>
