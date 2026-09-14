<template>
  <div class="page">
    <UserBar />

    <!-- 多活动切换：只有一个活动时不展示 -->
    <div v-if="userStore.activityList.length > 1" class="card activity-switch">
      <span class="switch-label">当前活动</span>
      <el-select
        :model-value="userStore.activityId"
        size="small"
        class="switch-select"
        @change="onActivityChange"
      >
        <el-option
          v-for="item in userStore.activityList"
          :key="item.activityId"
          :label="item.activityName"
          :value="item.activityId"
        />
      </el-select>
    </div>

    <!-- 必中规则提示 -->
    <div v-if="weightTip" class="card weight-tip">
      <el-icon class="tip-icon"><Bell /></el-icon>
      <span>{{ weightTip }}</span>
    </div>

    <div v-if="serviceStatus.degraded" class="service-alert" role="alert">
      <span>{{ serviceStatus.message }}</span>
      <el-button size="small" @click="retryService">重新尝试</el-button>
    </div>

    <!-- 大转盘 -->
    <div class="card wheel-card">
      <template v-if="awards.length">
        <LuckyWheel
          ref="wheelRef"
          :awards="awards"
          :usable-count="userStore.usableCount"
          :disabled="serviceStatus.degraded"
          @draw="onDraw"
        />
        <div v-if="userStore.usableCount <= 0" class="empty-quota">
          抽奖次数用完啦，去
          <router-link to="/sign">签到</router-link>
          或
          <router-link to="/exchange">积分兑换</router-link>
          获取次数
        </div>
      </template>
      <div v-else class="wheel-empty">
        <p>奖品数据未加载成功，请确认后端服务已启动</p>
        <el-button type="primary" round size="small" :loading="reloading" @click="initPage">
          重新加载
        </el-button>
      </div>
    </div>

    <!-- 额度明细 -->
    <div class="card quota-card">
      <div class="quota-item">
        <span class="quota-icon">🎫</span>
        <span class="quota-num">{{ userStore.account.totalCountSurplus }}/{{ userStore.account.totalCount }}</span>
        <span class="quota-label">总额度(剩/总)</span>
      </div>
      <div class="quota-item">
        <span class="quota-icon">🗓️</span>
        <span class="quota-num">{{ userStore.account.monthCountSurplus }}/{{ userStore.account.monthCount }}</span>
        <span class="quota-label">月额度(剩/总)</span>
      </div>
      <div class="quota-item">
        <span class="quota-icon">☀️</span>
        <span class="quota-num">{{ userStore.account.dayCountSurplus }}/{{ userStore.account.dayCount }}</span>
        <span class="quota-label">日额度(剩/总)</span>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="card quick-entry">
      <div class="quick-item" @click="router.push('/credit')">
        <span class="quick-icon">💰</span>
        <span class="quick-text">积分明细</span>
      </div>
      <div class="quick-item" @click="router.push('/orders')">
        <span class="quick-icon">📦</span>
        <span class="quick-text">我的订单</span>
      </div>
      <div class="quick-item" @click="router.push('/my')">
        <span class="quick-icon">👤</span>
        <span class="quick-text">我的</span>
      </div>
    </div>

    <!-- 中奖弹窗 -->
    <el-dialog v-model="awardDialogVisible" width="80%" :show-close="false" align-center class="award-dialog">
      <div class="award-result">
        <div class="award-emoji">🎉</div>
        <div class="award-result-title">恭喜中奖</div>
        <div class="award-result-name">{{ awardResult?.awardTitle }}</div>
        <div v-if="awardConfigText" class="award-result-config">{{ awardConfigText }}</div>
      </div>
      <template #footer>
        <el-button type="primary" style="width: 100%" @click="awardDialogVisible = false">开心收下</el-button>
      </template>
    </el-dialog>

    <BottomNav />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import UserBar from '../components/UserBar.vue'
import BottomNav from '../components/BottomNav.vue'
import LuckyWheel from '../components/LuckyWheel.vue'
import {
  draw,
  queryRaffleAwardList,
  queryRaffleStrategyRuleWeight
} from '../api/raffle'
import { useUserStore } from '../store/user'
import { useServiceStatusStore } from '../store/serviceStatus'

const userStore = useUserStore()
const serviceStatus = useServiceStatusStore()
const router = useRouter()
const wheelRef = ref(null)

const awards = ref([])
const ruleWeights = ref([])
const awardDialogVisible = ref(false)
const awardResult = ref(null)
const drawing = ref(false)
const reloading = ref(false)

// 权重提示：优先展示已达标最大档位，否则展示最近将达成档位
const weightTip = computed(() => {
  if (!ruleWeights.value.length) return ''
  const used = ruleWeights.value[0].userActivityAccountTotalUseCount
  const sorted = [...ruleWeights.value].sort((a, b) => a.ruleWeightCount - b.ruleWeightCount)
  const reached = sorted.filter((r) => used >= r.ruleWeightCount).pop()
  const target = reached || sorted[0]
  const names = target.strategyAwards.map((a) => a.awardTitle).join('、')
  return reached
    ? `已解锁累计 ${target.ruleWeightCount} 次必中范围：${names}`
    : `再抽 ${target.ruleWeightCount - used} 次必中：${names}`
})

// awardConfig 形如 "1,100"，解析为积分范围提示
const awardConfigText = computed(() => {
  const cfg = awardResult.value?.awardConfig
  if (!cfg) return ''
  const parts = String(cfg).split(',')
  if (parts.length === 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
    return `随机积分 ${parts[0]} ~ ${parts[1]}`
  }
  return ''
})

async function loadPageData() {
  // 奖品列表、权重规则、账户额度并行请求；refreshAccount 由 store 去重，与 UserBar 的调用共享同一次请求
  const [awardList, weights] = await Promise.all([
    queryRaffleAwardList(userStore.userId, userStore.activityId),
    queryRaffleStrategyRuleWeight(userStore.userId, userStore.activityId).catch(() => []),
    userStore.refreshAccount().catch(() => {})
  ])
  awards.value = [...awardList].sort((a, b) => a.sort - b.sort)
  ruleWeights.value = weights
}

async function onDraw() {
  if (drawing.value || serviceStatus.degraded) return
  drawing.value = true
  try {
    const result = await draw(userStore.userId, userStore.activityId)
    await wheelRef.value.spinTo(result.awardIndex)
    awardResult.value = result
    awardDialogVisible.value = true
    // 抽奖后刷新：奖品解锁进度、账户额度、权重规则
    loadPageData().catch(() => {})
  } catch (e) {
    if (e.code === 'AWARD_INDEX_MISMATCH') {
      ElMessage.error('抽奖结果与转盘奖品不一致，已停止展示，请联系管理员')
    }
  } finally {
    drawing.value = false
  }
}

function retryService() {
  serviceStatus.clearDegraded()
}

async function initPage() {
  if (reloading.value) return
  reloading.value = true
  try {
    // 概率表和库存由后端懒初始化，页面只读取业务数据。
    await loadPageData().catch(() => {})
    if (!awards.value.length) {
      ElMessage.warning('活动奖品尚未配置，请联系管理员')
    }
  } finally {
    reloading.value = false
  }
}

// 切换活动：刷新奖品、权重与额度。
async function onActivityChange(activityId) {
  if (activityId === userStore.activityId) return
  userStore.switchActivity(activityId)
  awards.value = []
  ruleWeights.value = []
  await initPage()
}

onMounted(async () => {
  // 先取活动列表，保证首屏用的是本地记录的活动（或列表第一个活动）
  await userStore.loadActivities()
  await initPage()
})
</script>

<style scoped>
/* 快捷入口 */
.quick-entry {
  display: flex;
  align-items: center;
  padding: 6px 0;
}

.quick-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 0;
  cursor: pointer;
}

.quick-icon {
  font-size: 20px;
}

.quick-text {
  font-size: 12px;
  color: #666;
}

/* 活动切换 */
.activity-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
}

.switch-label {
  font-size: 13px;
  color: #999;
}

.switch-select {
  width: 190px;
}

/* 必中提示：金底横幅风 */
.weight-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand-red-deep);
  background: #fbe6b8;
  border: 1px solid var(--brand-gold);
  border-radius: 10px;
}

.tip-icon {
  color: var(--brand-gold-deep);
  flex-shrink: 0;
}

.wheel-card {
  padding: 8px 16px 16px;
}

.service-alert {
  margin: 10px 12px;
  padding: 10px 12px;
  border: 1px solid #e7a23b;
  border-radius: 8px;
  background: #fff7e8;
  color: #8a5a12;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.wheel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 0;
  color: #999;
  font-size: 13px;
}

.empty-quota {
  text-align: center;
  font-size: 13px;
  color: #999;
  padding: 4px 0 8px;
}

.empty-quota a {
  color: var(--brand-red);
  text-decoration: none;
  font-weight: 600;
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

.quota-icon {
  font-size: 18px;
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

.award-result {
  text-align: center;
  padding: 8px 0;
}

.award-emoji {
  font-size: 52px;
  animation: emoji-bounce 0.9s ease infinite;
}

@keyframes emoji-bounce {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-8px) scale(1.08);
  }
}

.award-result-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--brand-red-dark);
  margin: 8px 0 4px;
}

.award-result-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--brand-gold-deep);
}

.award-result-config {
  font-size: 13px;
  color: #999;
  margin-top: 6px;
}

/* 中奖弹窗金边 */
:deep(.award-dialog) {
  border: 2px solid var(--brand-gold);
}

:deep(.award-dialog .el-dialog__header) {
  display: none;
}
</style>
