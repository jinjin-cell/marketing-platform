<template>
  <div class="bottom-nav">
    <div
      v-for="item in navItems"
      :key="item.path"
      class="nav-item"
      :class="{ active: route.path === item.path }"
      @click="router.push(item.path)"
    >
      <span class="nav-icon">{{ item.icon }}</span>
      <span class="nav-label">{{ item.label }}</span>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const navItems = [
  { path: '/', label: '抽奖', icon: '🎯' },
  { path: '/sign', label: '签到', icon: '📅' },
  { path: '/exchange', label: '兑换', icon: '🎁' }
]
</script>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 640px;
  display: flex;
  background: #fff;
  border-top: 1px solid #f0e2c8;
  box-shadow: 0 -2px 10px rgba(91, 58, 41, 0.06);
  z-index: 100;
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 10px;
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
  position: relative;
}

/* 激活态：顶部金色指示条 + 图标放大 */
.nav-item.active {
  color: var(--brand-red);
  font-weight: 600;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 3px;
  border-radius: 0 0 3px 3px;
  background: var(--brand-gold);
}

.nav-item.active .nav-icon {
  transform: scale(1.15);
}

.nav-icon {
  font-size: 20px;
  line-height: 1.2;
  transition: transform 0.2s;
}

.nav-label {
  font-size: 12px;
  margin-top: 2px;
}
</style>
