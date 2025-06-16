<template>
  <div class="daily-study-detail">
    <h3 class="detail-title">
      {{ selectedDate ? `${formatDate(selectedDate)} 학습 기록` : '날짜를 선택해주세요' }}
    </h3>
    <div v-if="dailyStudyData && selectedDate && dailyTotal > 0" class="detail-stats">
      <div class="stat-item perfect">
        <span class="stat-label">완벽한 기억</span>
        <span class="stat-count">{{ dailyStudyData.perfect }}</span>
      </div>
      <div class="stat-item vague">
        <span class="stat-label">희미한 기억</span>
        <span class="stat-count">{{ dailyStudyData.vague }}</span>
      </div>
      <div class="stat-item forgotten">
        <span class="stat-label">사라진 기억</span>
        <span class="stat-count">{{ dailyStudyData.forgotten }}</span>
      </div>
    </div>
    <div v-else-if="!selectedDate" class="no-selection">
      <img src="@/assets/mogwi-look.png" alt="모귀 보기" class="mogwi-look-icon" />
      <p>달력에서 날짜를 클릭하여 상세 기록을 확인하세요.</p>
    </div>
    <div v-else class="no-data">
        <img src="@/assets/mogwi-sleep.png" alt="모귀 잠자기" class="mogwi-sleep-icon" />
        <p>선택하신 날짜에 학습 기록이 없습니다.</p>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue';
import mogwiSleep from '@/assets/mogwi-sleep.png';
import mogwiLook from '@/assets/mogwi-look.png';

export default {
  name: 'DailyStudyDetail',
  props: {
    selectedDate: {
      type: String, // YYYY-MM-DD
      default: null
    },
    dailyStudyData: {
      type: Object, // { perfect: N, vague: M, forgotten: O }
      default: null
    }
  },
  setup(props) {
    const formatDate = (dateString) => {
      if (!dateString) return '';
      const [year, month, day] = dateString.split('-');
      return `${year}년 ${parseInt(month)}월 ${parseInt(day)}일`;
    };

    const dailyTotal = computed(() => {
      if (!props.dailyStudyData) return 0;
      return props.dailyStudyData.perfect + props.dailyStudyData.vague + props.dailyStudyData.forgotten;
    });

    return {
      formatDate,
      dailyTotal,
      mogwiSleep,
      mogwiLook
    };
  }
};
</script>

<style scoped>
.daily-study-detail {
  width: 100%;
  padding: 25px;
  text-align: center;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background-color: #fcf8ff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  border: 1px solid #e9dffc;
}

.detail-title {
  font-size: 1.6rem;
  color: #5a2e87;
  margin-bottom: 25px;
  font-weight: 600;
  border-bottom: 1px dashed #f0e6ff;
  padding-bottom: 12px;
}

.detail-stats {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}

.detail-stats .stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px 20px;
  border-radius: 8px;
  background-color: #ffffff;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05);
  min-width: 140px;
  flex: 1;
  border: 1px solid #f2eaff;
}

.detail-stats .stat-label {
  font-size: 1rem;
  color: #7a4bb7;
  margin-bottom: 8px;
}

.detail-stats .stat-count {
  font-size: 2.2rem;
  font-weight: 700;
}

.detail-stats .perfect .stat-count {
  color: #4CAF50;
}
.detail-stats .vague .stat-count {
  color: #FFC107;
}
.detail-stats .forgotten .stat-count {
  color: #F44336;
}

.no-data, .no-selection {
  margin-top: 30px;
  color: #888;
  font-size: 1.2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.mogwi-sleep-icon, .mogwi-look-icon {
    width: 90px;
    height: auto;
    margin-bottom: 8px;
}

@media (max-width: 768px) {
  .daily-study-detail {
    padding: 15px;
  }
  .detail-title {
    font-size: 1.3rem;
    margin-bottom: 15px;
  }
  .detail-stats {
    gap: 10px;
  }
  .detail-stats .stat-item {
    padding: 10px 15px;
    min-width: unset;
    width: 100%;
  }
  .detail-stats .stat-count {
    font-size: 1.5rem;
  }
  .no-data, .no-selection {
    font-size: 0.95rem;
  }
  .mogwi-sleep-icon, .mogwi-look-icon {
    width: 60px;
  }
}
</style>