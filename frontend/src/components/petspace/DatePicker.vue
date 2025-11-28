<!-- 
  文件位置: src/components/petspace/DatePicker.vue
  日期选择器 - 侧边栏内显示
-->

<template>
  <div class="date-picker">
    <div class="picker-header">
      <button @click="prevMonth" class="nav-btn">◀</button>
      <div class="current-month">{{ currentMonth }}</div>
      <button @click="nextMonth" class="nav-btn">▶</button>
    </div>

    <div class="calendar-grid">
      <div v-for="day in weekDays" :key="day" class="week-day">
        {{ day }}
      </div>

      <div 
        v-for="(date, index) in calendarDates" 
        :key="index"
        :class="['calendar-date', {
          'other-month': date.otherMonth,
          'today': date.isToday,
          'selected': date.isSelected
        }]"
        @click="selectDate(date)"
      >
        {{ date.day }}
      </div>
    </div>

    <div class="quick-actions">
      <button @click="selectToday" class="quick-btn">
        回到今天
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Date,
    default: () => new Date()
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'close'])

const selectedDate = ref(new Date(props.modelValue))
const viewMonth = ref(new Date(props.modelValue))

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

// 当前月份显示
const currentMonth = computed(() => {
  const year = viewMonth.value.getFullYear()
  const month = viewMonth.value.getMonth() + 1
  return `${year}年${month}月`
})

// 生成日历日期数组
const calendarDates = computed(() => {
  const dates = []
  const year = viewMonth.value.getFullYear()
  const month = viewMonth.value.getMonth()
  
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const firstDayOfWeek = firstDay.getDay()
  
  // 填充上个月的日期
  const prevMonthLastDay = new Date(year, month, 0).getDate()
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    dates.push({
      day: prevMonthLastDay - i,
      date: new Date(year, month - 1, prevMonthLastDay - i),
      otherMonth: true,
      isToday: false,
      isSelected: false
    })
  }
  
  // 填充本月日期
  for (let i = 1; i <= lastDay.getDate(); i++) {
    const date = new Date(year, month, i)
    dates.push({
      day: i,
      date: date,
      otherMonth: false,
      isToday: isSameDay(date, new Date()),
      isSelected: isSameDay(date, selectedDate.value)
    })
  }
  
  // 填充下个月的日期
  const remainingDays = 42 - dates.length
  for (let i = 1; i <= remainingDays; i++) {
    dates.push({
      day: i,
      date: new Date(year, month + 1, i),
      otherMonth: true,
      isToday: false,
      isSelected: false
    })
  }
  
  return dates
})

const isSameDay = (date1, date2) => {
  return date1.getFullYear() === date2.getFullYear() &&
         date1.getMonth() === date2.getMonth() &&
         date1.getDate() === date2.getDate()
}

const selectDate = (dateObj) => {
  selectedDate.value = new Date(dateObj.date)
  emit('update:modelValue', selectedDate.value)
  emit('change', selectedDate.value)
  emit('close') // 选择后自动关闭
}

const selectToday = () => {
  const today = new Date()
  selectedDate.value = today
  viewMonth.value = today
  emit('update:modelValue', selectedDate.value)
  emit('change', selectedDate.value)
  emit('close')
}

const prevMonth = () => {
  viewMonth.value = new Date(
    viewMonth.value.getFullYear(),
    viewMonth.value.getMonth() - 1,
    1
  )
}

const nextMonth = () => {
  viewMonth.value = new Date(
    viewMonth.value.getFullYear(),
    viewMonth.value.getMonth() + 1,
    1
  )
}

watch(() => props.modelValue, (newVal) => {
  selectedDate.value = new Date(newVal)
  viewMonth.value = new Date(newVal)
})
</script>

<style scoped>
.date-picker {
  user-select: none;
  padding: var(--spacing-4);
  background: rgba(255, 255, 255, 0.95);
  border-radius: var(--radius-xl);
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-6);
  padding: var(--spacing-3);
}

.current-month {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
}

.nav-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  background: var(--color-gray-100);
  color: var(--color-gray-700);
  transition: all var(--duration-base);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: var(--text-lg);
}

.nav-btn:hover {
  background: var(--color-primary);
  color: white;
  transform: scale(1.1);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-6);
}

.week-day {
  text-align: center;
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-gray-600);
  padding: var(--spacing-3);
}

.calendar-date {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-base);
  color: var(--color-gray-900);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-base);
  font-weight: var(--font-medium);
}

.calendar-date:hover {
  background: var(--color-gray-100);
  transform: scale(1.05);
}

.calendar-date.other-month {
  color: var(--color-gray-400);
}

.calendar-date.today {
  background: rgba(99, 102, 241, 0.15);
  color: var(--color-primary);
  font-weight: var(--font-bold);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.3);
}

.calendar-date.selected {
  background: var(--gradient-primary);
  color: white;
  font-weight: var(--font-bold);
  box-shadow: var(--shadow-md);
}

.quick-actions {
  padding-top: var(--spacing-4);
  border-top: 2px solid var(--color-gray-200);
}

.quick-btn {
  width: 100%;
  padding: var(--spacing-3) var(--spacing-4);
  background: var(--gradient-primary);
  color: white;
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  transition: all var(--duration-base);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
}

.quick-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}
</style>