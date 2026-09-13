<template>
  <div class="wheel-wrap">
    <!-- 转盘外圈跑马灯 -->
    <div
      v-for="n in 16"
      :key="`bulb-${n}`"
      class="rim-bulb"
      :style="bulbStyle(n - 1)"
    />
    <!-- 指针 -->
    <div class="wheel-pointer">
      <div class="pointer-pin" />
    </div>
    <!-- 转盘主体 -->
    <div
      ref="wheelRef"
      class="wheel"
      :style="{
        transform: `rotate(${rotation}deg)`,
        background: wheelBackground,
        transition: spinning ? 'transform 4s cubic-bezier(0.25, 0.1, 0.25, 1)' : 'none'
      }"
    >
      <div
        v-for="(award, i) in awards"
        :key="award.awardId"
        class="sector-label"
        :style="{ transform: `rotate(${sectorAngle * i}deg)` }"
      >
        <div class="sector-content" :class="{ locked: !award.isAwardUnlock }">
          <span class="award-title">{{ award.awardTitle }}</span>
          <span v-if="!award.isAwardUnlock" class="lock-tip">
            🔒 还差{{ award.waitUnlockCount }}次
          </span>
          <span v-else class="award-sub">{{ award.awardSubTitle }}</span>
        </div>
      </div>
    </div>
    <!-- 中心按钮 -->
    <div class="wheel-center" :class="{ disabled: centerDisabled }" @click="onCenterClick">
      <span class="center-text">{{ spinning ? '抽奖中' : '立即抽奖' }}</span>
      <span v-if="!spinning" class="center-sub">剩余 {{ usableCount }} 次</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  // 奖品列表（已按 sort 排序）
  awards: { type: Array, default: () => [] },
  usableCount: { type: Number, default: 0 }
})

const emit = defineEmits(['draw'])

const rotation = ref(0)
const spinning = ref(false)

const sectorAngle = computed(() => (props.awards.length ? 360 / props.awards.length : 0))

// 交替红金配色的扇区背景
const wheelBackground = computed(() => {
  const n = props.awards.length
  if (!n) return '#fff'
  const stops = []
  for (let i = 0; i < n; i++) {
    const color = i % 2 === 0 ? '#fff2d9' : '#ffe1b3'
    stops.push(`${color} ${(i * 360) / n}deg ${((i + 1) * 360) / n}deg`)
  }
  return `conic-gradient(${stops.join(', ')})`
})

const centerDisabled = computed(() => spinning.value || props.usableCount <= 0)

// 跑马灯位置：沿外圈均匀分布
function bulbStyle(i) {
  const angle = (i * 360) / 16 - 90
  const rad = (angle * Math.PI) / 180
  const r = 162 // 外圈半径（比转盘 320/2 略大）
  return {
    left: `${50 + (Math.cos(rad) * r) / 3.2}%`,
    top: `${50 + (Math.sin(rad) * r) / 3.2}%`,
    animationDelay: `${(i % 2) * 0.5}s`
  }
}

function onCenterClick() {
  if (centerDisabled.value) return
  emit('draw')
}

/**
 * 旋转到指定 awardIndex（与奖品 sort 对应），动画结束后 resolve
 */
function spinTo(awardIndex) {
  return new Promise((resolve) => {
    const i = props.awards.findIndex((a) => a.sort === awardIndex)
    const index = i === -1 ? 0 : i
    // 目标扇区中心角度（从顶部顺时针计算）
    const sectorCenter = (index + 0.5) * sectorAngle.value
    // 当前角度归一化后，再叠加 5 圈以上，保证顺时针旋转惯性
    const current = rotation.value % 360
    const target = rotation.value - current + 360 * 5 + (360 - sectorCenter)
    spinning.value = true
    rotation.value = target
    setTimeout(() => {
      spinning.value = false
      resolve()
    }, 4100)
  })
}

defineExpose({ spinTo, spinning })
</script>

<style scoped>
.wheel-wrap {
  position: relative;
  width: 340px;
  height: 340px;
  margin: 16px auto;
}

/* 外圈跑马灯：金红交替闪烁 */
.rim-bulb {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: var(--brand-gold-light);
  box-shadow: 0 0 6px rgba(240, 165, 0, 0.8);
  animation: bulb-blink 1s ease-in-out infinite;
  z-index: 6;
}

.rim-bulb:nth-of-type(even) {
  background: #fff;
  box-shadow: 0 0 6px rgba(255, 216, 115, 0.9);
}

@keyframes bulb-blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.25;
  }
}

.wheel {
  width: 320px;
  height: 320px;
  margin: 10px;
  border-radius: 50%;
  border: 10px solid var(--brand-red);
  box-shadow: 0 0 0 4px var(--brand-gold), 0 8px 24px rgba(168, 30, 30, 0.35);
  position: relative;
  overflow: hidden;
}

/* 指针：金色底座钉 + 深金箭头 */
.wheel-pointer {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 14px solid transparent;
  border-right: 14px solid transparent;
  border-top: 24px solid var(--brand-gold-deep);
  z-index: 10;
  filter: drop-shadow(0 2px 2px rgba(0, 0, 0, 0.3));
}

.pointer-pin {
  position: absolute;
  top: -26px;
  left: 50%;
  transform: translateX(-50%);
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--brand-gold);
  border: 2px solid #fff;
}

.sector-label {
  position: absolute;
  top: 0;
  left: 50%;
  width: 0;
  height: 100%;
  transform-origin: 0 50%;
}

.sector-content {
  position: absolute;
  top: 18px;
  left: 0;
  transform: translateX(-50%);
  width: 88px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.award-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--brand-red-dark);
  text-align: center;
  word-break: keep-all;
}

.award-sub {
  font-size: 10px;
  color: var(--brand-brown);
  opacity: 0.8;
  text-align: center;
}

.sector-content.locked .award-title {
  color: #b0a08e;
}

.lock-tip {
  font-size: 10px;
  color: #999;
  text-align: center;
}

.wheel-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: var(--brand-red);
  border: 4px solid var(--brand-gold);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 5;
  box-shadow: 0 4px 12px rgba(168, 30, 30, 0.5);
  user-select: none;
  animation: center-pulse 1.8s ease-in-out infinite;
}

@keyframes center-pulse {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(168, 30, 30, 0.5), 0 0 0 0 rgba(240, 165, 0, 0.5);
  }
  50% {
    box-shadow: 0 4px 12px rgba(168, 30, 30, 0.5), 0 0 0 10px rgba(240, 165, 0, 0);
  }
}

.wheel-center:active {
  transform: translate(-50%, -50%) scale(0.96);
}

.wheel-center.disabled {
  background: #b3a99b;
  border-color: #d9d0c2;
  cursor: not-allowed;
  animation: none;
}

.center-text {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}

.center-sub {
  font-size: 11px;
  margin-top: 2px;
  color: var(--brand-gold);
}
</style>
