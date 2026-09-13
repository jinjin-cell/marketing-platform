<template>
  <div class="page">
    <UserBar />

    <div class="card">
      <div class="section-heading">积分兑换抽奖次数</div>
      <div class="section-sub">当前积分余额：<span class="text-gold">{{ creditText }}</span></div>
    </div>

    <div v-if="loading" class="card loading-card">加载中…</div>
    <div v-else-if="!products.length" class="card loading-card">暂无可兑换商品</div>

    <div v-for="p in products" :key="p.sku" class="card product-card">
      <div class="product-info">
        <div class="product-name">
          {{ p.activityCount.totalCount }} 次抽奖卡
        </div>
        <div class="product-detail">
          日限 {{ p.activityCount.dayCount }} 次 · 月限 {{ p.activityCount.monthCount }} 次
        </div>
        <div class="product-stock" :class="{ 'stock-low': p.stockCountSurplus <= 10 }">
          <span class="stock-dot" />剩余库存 {{ p.stockCountSurplus }} / {{ p.stockCount }}
        </div>
      </div>
      <div class="product-action">
        <div class="product-price">
          <span class="price-num">{{ formatAmount(p.productAmount) }}</span>
          <span class="price-unit">积分</span>
        </div>
        <el-button
          type="primary"
          round
          size="small"
          :disabled="!canExchange(p)"
          :loading="exchangingSku === p.sku"
          @click="onExchange(p)"
        >
          {{ exchangeBtnText(p) }}
        </el-button>
      </div>
    </div>

    <BottomNav />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserBar from '../components/UserBar.vue'
import BottomNav from '../components/BottomNav.vue'
import { creditPayExchangeSku, querySkuProductList } from '../api/raffle'
import { useUserStore, ACTIVITY_ID } from '../store/user'

const userStore = useUserStore()

const products = ref([])
const loading = ref(true)
const exchangingSku = ref(null)

const creditText = computed(() =>
  userStore.credit === null ? '--' : Number(userStore.credit).toFixed(0)
)

function formatAmount(amount) {
  return Number(amount).toFixed(0)
}

function canExchange(p) {
  return (
    p.stockCountSurplus > 0 &&
    userStore.credit !== null &&
    Number(userStore.credit) >= Number(p.productAmount) &&
    exchangingSku.value === null
  )
}

function exchangeBtnText(p) {
  if (p.stockCountSurplus <= 0) return '已兑完'
  if (userStore.credit !== null && Number(userStore.credit) < Number(p.productAmount)) return '积分不足'
  return '兑换'
}

async function loadProducts() {
  loading.value = true
  try {
    products.value = await querySkuProductList(ACTIVITY_ID)
  } catch (e) {
    products.value = []
  } finally {
    loading.value = false
  }
}

async function onExchange(p) {
  try {
    await ElMessageBox.confirm(
      `消耗 ${formatAmount(p.productAmount)} 积分，兑换 ${p.activityCount.totalCount} 次抽奖机会？`,
      '确认兑换',
      { confirmButtonText: '确认兑换', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }
  exchangingSku.value = p.sku
  try {
    await creditPayExchangeSku(userStore.userId, p.sku)
    ElMessage.success('兑换成功，抽奖额度已到账')
    // 刷新：积分余额、账户额度、商品库存
    await Promise.all([
      userStore.refreshCredit().catch(() => {}),
      userStore.refreshAccount().catch(() => {}),
      loadProducts()
    ])
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    exchangingSku.value = null
  }
}

onMounted(() => {
  loadProducts()
  userStore.refreshCredit().catch(() => {})
})
</script>

<style scoped>
.section-sub {
  font-size: 13px;
  color: #999;
  margin-top: 6px;
}

.loading-card {
  text-align: center;
  color: #999;
  font-size: 13px;
}

/* 商品卡：左侧金色票券边 */
.product-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-left: 4px solid var(--brand-gold);
}

.product-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--brand-brown);
}

.product-detail {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.product-stock {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #3d8b37;
  margin-top: 6px;
}

.stock-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.product-stock.stock-low {
  color: var(--brand-gold-deep);
}

.product-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

/* 价格：红包数字风 */
.product-price {
  display: flex;
  align-items: baseline;
  gap: 2px;
  background: var(--brand-cream);
  border: 1px dashed var(--brand-gold);
  border-radius: 8px;
  padding: 2px 10px;
}

.price-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--brand-red);
}

.price-unit {
  font-size: 12px;
  color: #999;
}
</style>
