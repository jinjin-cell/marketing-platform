<template>
  <div class="page">
    <UserBar />

    <!-- 必中规则提示 -->
    <div v-if="weightTip" class="card weight-tip">
      <el-icon class="tip-icon"><Bell /></el-icon>
      <span>{{ weightTip }}</span>
    </div>

    <!-- 大转盘 -->
    <div class="card wheel-card">
      <template v-if="awards.length">
        <LuckyWheel
          ref="wheelRef"
          :awards="awards"
          :usable-count="userStore.usableCount"
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
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import UserBar from '../components/UserBar.vue'
import BottomNav from '../components/BottomNav.vue'
import LuckyWheel from '../components/LuckyWheel.vue'
import {
  armory,
  draw,
  queryRaffleAwardList,
  queryRaffleStrategyRuleWeight
} from '../api/raffle'
import { useUserStore, ACTIVITY_ID } from '../store/user'

const userStore = useUserStore()
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
    queryRaffleAwardList(userStore.userId, ACTIVITY_ID),
    queryRaffleStrategyRuleWeight(userStore.userId, ACTIVITY_ID).catch(() => []),
    userStore.refreshAccount().catch(() => {})
  ])
  awards.value = [...awardList].sort((a, b) => a.sort - b.sort)
  ruleWeights.value = weights
}

async function onDraw() {
  if (drawing.value) return
  drawing.value = true
  try {
    const result = await draw(userStore.userId, ACTIVITY_ID)
    await wheelRef.value.spinTo(result.awardIndex)
    awardResult.value = result
    awardDialogVisible.value = true
    // 抽奖后刷新：奖品解锁进度、账户额度、权重规则
    loadPageData().catch(() => {})
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    drawing.value = false
  }
}

async function initPage() {
  if (reloading.value) return
  reloading.value = true
  try {
    // 直接拉取页面数据：概率表由后端装配/抽奖链路兜底，不再每次进页都触发 armory 阻塞首屏
    await loadPageData().catch(() => {})
    // 奖品数据为空时降级：手动触发一次活动装配后重试加载
    if (!awards.value.length) {
      const ok = await armory(ACTIVITY_ID).catch(() => false)
      if (ok) {
        await loadPageData().catch(() => {})
      } else {
        ElMessage.warning('活动准备中，请稍后重试')
      }
    }
  } finally {
    reloading.value = false
  }
}

onMounted(initPage)
</script>

<style scoped>
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
