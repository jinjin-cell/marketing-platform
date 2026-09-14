<template>
  <div class="page">
    <UserBar />

    <!-- 额度明细 -->
    <div class="card quota-card">
      <div class="quota-item">
        <span class="quota-num">{{ userStore.account.totalCountSurplus }}/{{ userStore.account.totalCount }}</span>
        <span class="quota-label">总额度(剩/总)</span>
      </div>
      <div class="quota-item">
        <span class="quota-num">{{ userStore.account.monthCountSurplus }}/{{ userStore.account.monthCount }}</span>
        <span class="quota-label">月额度(剩/总)</span>
      </div>
      <div class="quota-item">
        <span class="quota-num">{{ userStore.account.dayCountSurplus }}/{{ userStore.account.dayCount }}</span>
        <span class="quota-label">日额度(剩/总)</span>
      </div>
    </div>

    <!-- 我的资产入口 -->
    <div class="card entry-card">
      <div class="entry-item" @click="router.push('/credit')">
        <span class="entry-icon">💰</span>
        <span class="entry-text">积分明细</span>
        <span class="entry-arrow">›</span>
      </div>
      <div class="entry-divider" />
      <div class="entry-item" @click="router.push('/orders')">
        <span class="entry-icon">📦</span>
        <span class="entry-text">我的订单</span>
        <span class="entry-arrow">›</span>
      </div>
    </div>

    <!-- 中奖记录 -->
    <div class="card">
      <div class="section-title">我的中奖记录</div>
    </div>

    <div v-if="loading" class="card loading-card">加载中…</div>
    <div v-else-if="error" class="card loading-card error-state">
      <span>{{ error }}</span>
      <el-button size="small" @click="loadRecords">重试</el-button>
    </div>
    <div v-else-if="!records.length" class="card loading-card">
      暂无中奖记录，去<router-link to="/" class="link">抽奖</router-link>试试手气吧
    </div>

    <div v-for="(r, i) in records" :key="i" class="card record-card">
      <div class="record-info">
        <div class="record-title">{{ r.awardTitle }}</div>
        <div class="record-time">{{ r.awardTime }}</div>
      </div>
      <el-tag
        size="small"
        :type="stateTagType(r.awardState)"
        effect="plain"
        round
      >
        {{ stateText(r.awardState) }}
      </el-tag>
    </div>

    <BottomNav />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import UserBar from '../components/UserBar.vue'
import BottomNav from '../components/BottomNav.vue'
import { queryUserAwardRecordList } from '../api/raffle'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const router = useRouter()

const records = ref([])
const loading = ref(true)
const error = ref('')

const STATE_MAP = {
  create: { text: '待发放', type: 'warning' },
  complete: { text: '已发放', type: 'success' },
  // 历史数据中存在 completed 写法，一并兼容
  completed: { text: '已发放', type: 'success' },
  fail: { text: '发放失败', type: 'danger' }
}

function stateText(state) {
  return STATE_MAP[state]?.text || state
}

function stateTagType(state) {
  return STATE_MAP[state]?.type || 'info'
}

async function loadRecords() {
  loading.value = true
  error.value = ''
  try {
    const result = await queryUserAwardRecordList(userStore.userId, userStore.activityId)
    records.value = Array.isArray(result) ? result : []
  } catch (e) {
    error.value = '中奖记录加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await userStore.loadActivities()
  await loadRecords()
  userStore.refreshAccount().catch(() => {})
})
</script>

<style scoped>
/* 资产入口 */
.entry-card {
  padding: 4px 14px;
}

.entry-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  cursor: pointer;
}

.entry-icon {
  font-size: 18px;
}

.entry-text {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: var(--brand-brown);
}

.entry-arrow {
  color: #c8c8c8;
  font-size: 18px;
}

.entry-divider {
  height: 1px;
  background: #f0f0f0;
}

.quota-card {
  display: flex;
}

.quota-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.quota-num {
  font-size: 17px;
  font-weight: 700;
  color: var(--brand-red-dark);
}

.quota-label {
  font-size: 12px;
  color: #999;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--brand-red-dark);
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
</style>
