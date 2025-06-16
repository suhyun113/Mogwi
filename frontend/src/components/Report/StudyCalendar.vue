<template>
  <div class="study-calendar">
    <div class="calendar-header">
      <button @click="prevMonth" class="nav-button">&lt;</button>
      <h3 class="current-month">{{ currentMonthYear }}</h3>
      <button @click="nextMonth" class="nav-button">&gt;</button>
    </div>
    <div class="calendar-grid">
      <div class="day-label" v-for="day in daysOfWeek" :key="day">{{ day }}</div>
      <div
        v-for="blank in startDayOffset"
        :key="`blank-${blank}`"
        class="calendar-day blank-day"
      ></div>
      <div
        v-for="day in daysInMonth"
        :key="day"
        :class="['calendar-day', { 'today': isToday(day), 'has-study': hasStudy(day), 'selected': isSelected(day) }]"
        @click="selectDate(day)"
      >
        {{ day }}
        <div v-if="hasStudy(day)" class="study-counts-bubble">
          <span class="perfect-count" :title="`${getStudyCount(day, 'perfect')} 완벽`">{{ getStudyCount(day, 'perfect') }}</span>
          <span class="vague-count" :title="`${getStudyCount(day, 'vague')} 희미`">{{ getStudyCount(day, 'vague') }}</span>
          <span class="forgotten-count" :title="`${getStudyCount(day, 'forgotten')} 사라짐`">{{ getStudyCount(day, 'forgotten') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue';

export default {
  name: 'StudyCalendar',
  props: {
    // studyDates: { 'YYYY-MM-DD': { perfect: N, vague: M, forgotten: O }, ... }
    studyDates: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['date-selected'],
  setup(props, { emit }) {
    const today = new Date();
    const currentMonth = ref(today.getMonth());
    const currentYear = ref(today.getFullYear());
    const selectedDay = ref(null); // Stores the day number (e.g., 15) of the selected date in the current month

    const daysOfWeek = ['일', '월', '화', '수', '목', '금', '토'];

    const currentMonthYear = computed(() => {
      const date = new Date(currentYear.value, currentMonth.value);
      return date.toLocaleString('ko-KR', { year: 'numeric', month: 'long' });
    });

    const firstDayOfMonth = computed(() => {
      return new Date(currentYear.value, currentMonth.value, 1).getDay(); // 0 for Sunday, 1 for Monday, etc.
    });

    const daysInMonth = computed(() => {
      return new Date(currentYear.value, currentMonth.value + 1, 0).getDate(); // Last day of current month
    });

    const startDayOffset = computed(() => firstDayOfMonth.value);

    const isToday = (day) => {
      const date = new Date();
      return (
        day === date.getDate() &&
        currentMonth.value === date.getMonth() &&
        currentYear.value === date.getFullYear()
      );
    };

    const hasStudy = (day) => {
      const dateString = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      return !!props.studyDates[dateString] && (props.studyDates[dateString].perfect > 0 || props.studyDates[dateString].vague > 0 || props.studyDates[dateString].forgotten > 0);
    };

    const getStudyCount = (day, type) => {
      const dateString = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      return props.studyDates[dateString]?.[type] || 0;
    };

    const isSelected = (day) => {
        return day === selectedDay.value;
    };

    const selectDate = (day) => {
        selectedDay.value = day;
        const dateString = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        emit('date-selected', dateString);
    };

    const prevMonth = () => {
      if (currentMonth.value === 0) {
        currentMonth.value = 11;
        currentYear.value--;
      } else {
        currentMonth.value--;
      }
      selectedDay.value = null; // Clear selection on month change
    };

    const nextMonth = () => {
      if (currentMonth.value === 11) {
        currentMonth.value = 0;
        currentYear.value++;
      } else {
        currentMonth.value++;
      }
      selectedDay.value = null; // Clear selection on month change
    };

    // Watch for changes in studyDates to potentially update initial selection
    watch(() => props.studyDates, (newVal) => {
        const todayString = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
        if (newVal[todayString] && selectedDay.value === null) {
            // If today has study data and no date is selected yet, select today
            selectDate(today.getDate());
        }
    }, { immediate: true }); // Run immediately on component mount

    return {
      currentMonth,
      currentYear,
      daysOfWeek,
      currentMonthYear,
      firstDayOfMonth,
      daysInMonth,
      startDayOffset,
      isToday,
      hasStudy,
      getStudyCount,
      isSelected,
      selectDate,
      prevMonth,
      nextMonth,
      selectedDay // Expose for debugging if needed
    };
  }
};
</script>

<style scoped>
.study-calendar {
  width: 100%;
  max-width: 600px;
  background-color: #ffffff;
  border-radius: 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  padding: 25px;
  border: 1px solid #dcd0f0;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.nav-button {
  background-color: #e6e0f4;
  color: #5a2e87;
  border: none;
  padding: 8px 15px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1.1rem;
  font-weight: bold;
  transition: background-color 0.2s, transform 0.2s;
}

.nav-button:hover {
  background-color: #d1c4e9;
  transform: scale(1.05);
}

.current-month {
  font-size: 1.8rem;
  color: #4a1e77;
  font-weight: 700;
  margin: 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.day-label {
  font-weight: bold;
  text-align: center;
  color: #8c5dff;
  font-size: 0.9rem;
  padding: 8px 0;
}

.calendar-day {
  padding: 10px 0;
  text-align: center;
  border-radius: 8px;
  font-size: 1rem;
  color: #333;
  min-height: 70px; /* Adjust height to accommodate bubble */
  display: flex;
  flex-direction: column;
  justify-content: flex-start; /* Align number to top */
  align-items: center;
  position: relative; /* For positioning the bubble */
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
  border: 1px solid transparent; /* Default transparent border */
}

.calendar-day.blank-day {
  background-color: #f8f8f8;
  cursor: default;
}

.calendar-day.today {
  border: 2px solid #a471ff;
  background-color: #f0e6ff;
  font-weight: bold;
  color: #4a1e77;
}

.calendar-day.has-study {
  background-color: #e6f7e6; /* Light green for study activity */
  font-weight: 500;
  color: #333;
}

.calendar-day.selected {
  border: 2px solid #8c5dff;
  background-color: #d8c8ff;
  font-weight: bold;
  color: #4a1e77;
}

.calendar-day:hover:not(.blank-day) {
  background-color: #f0e6ff;
  border-color: #a471ff;
}

.study-counts-bubble {
  display: flex;
  gap: 5px;
  font-size: 0.75rem;
  margin-top: 5px;
  width: 100%;
  justify-content: center;
  flex-wrap: wrap;
}

.study-counts-bubble span {
  padding: 2px 4px;
  border-radius: 4px;
  font-weight: bold;
  white-space: nowrap;
}

.study-counts-bubble .perfect-count {
  background-color: #e8f5e9;
  color: #4CAF50;
}
.study-counts-bubble .vague-count {
  background-color: #fffde7;
  color: #FFC107;
}
.study-counts-bubble .forgotten-count {
  background-color: #ffebee;
  color: #F44336;
}

@media (max-width: 768px) {
  .study-calendar {
    padding: 15px;
  }
  .calendar-header {
    margin-bottom: 15px;
  }
  .current-month {
    font-size: 1.5rem;
  }
  .nav-button {
    padding: 6px 12px;
    font-size: 1rem;
  }
  .calendar-grid {
    gap: 5px;
  }
  .calendar-day {
    min-height: 60px;
    font-size: 0.9rem;
    padding: 8px 0;
  }
  .day-label {
    font-size: 0.8rem;
    padding: 6px 0;
  }
  .study-counts-bubble {
    font-size: 0.7rem;
    gap: 3px;
  }
  .study-counts-bubble span {
    padding: 1px 3px;
  }
}
</style>