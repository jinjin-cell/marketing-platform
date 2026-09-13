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

    <!-- 中奖记录 -->
    <div class="card">
      <div class="section-title">我的中奖记录</div>
    </div>

    <div v-if="loading" class="card loading-card">加载中…</div>
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
import UserBar from '../components/UserBar.vue'
import BottomNav from '../components/BottomNav.vue'
import { queryUserAwardRecordList } from '../api/raffle'
import { useUserStore, ACTIVITY_ID } from '../store/user'

const userStore = useUserStore()

const records = ref([])
const loading = ref(true)

const STATE_MAP = {
  create: { text: '发放中', type: 'warning' },
  complete: { text: '已到账', type: 'success' },
  fail: { text: '发放失败', type: 'danger' }
}

function stateText(state) {
  return STATE_MAP[state]?.text || state
}

function stateTagType(state) {
  return STATE_MAP[state]?.type || 'info'
}

onMounted(async () => {
  try {
    records.value = await queryUserAwardRecordList(userStore.userId, ACTIVITY_ID)
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
  }
  userStore.refreshAccount().catch(() => {})
})
</script>

<style scoped>
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
