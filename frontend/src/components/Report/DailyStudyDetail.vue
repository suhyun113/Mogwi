<template>
  <div class="daily-study-detail">
    <div v-if="selectedDate" class="detail-content">
      <p class="selected-date-display">{{ selectedDate }} 학습 기록</p>
      <div v-if="dailyStudyData && (dailyStudyData.perfect > 0 || dailyStudyData.vague > 0 || dailyStudyData.forgotten > 0)" class="study-summary-tags">
        <div class="tag-item">
          <span class="tag perfect-tag">완벽한 기억</span>
          <span class="count perfect-count-color">{{ dailyStudyData.perfect }}</span>
        </div>
        <div class="tag-item">
          <span class="tag vague-tag">희미한 기억</span>
          <span class="count vague-count-color">{{ dailyStudyData.vague }}</span>
        </div>
        <div class="tag-item">
          <span class="tag forgotten-tag">사라진 기억</span>
          <span class="count forgotten-count-color">{{ dailyStudyData.forgotten }}</span>
        </div>
      </div>
      <div v-else class="no-data-message">
        해당 날짜에 학습 기록이 없습니다.
      </div>
    </div>
    <div v-else class="no-date-selected-message">
      날짜를 선택하여 학습 기록을 확인하세요.
    </div>
  </div>
</template>

<script>
export default {
  name: 'DailyStudyDetail',
  props: {
    selectedDate: {
      type: String,
      default: null,
    },
    dailyStudyData: {
      type: Object,
      default: () => ({ perfect: 0, vague: 0, forgotten: 0 }),
    },
  },
};
</script>

<style scoped>
.daily-study-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  height: 100%;
  padding: 10px; /* 패딩을 20px에서 10px로 줄임 */
  box-sizing: border-box;
  background-color: transparent;
}

.detail-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  width: 100%;
  height: 100%;
  flex-grow: 1;
  gap: 4px;
  padding-top: 15px;
}

.selected-date-display {
  font-size: 1.3rem;
  font-weight: 600;
  color: white;
  margin-bottom: 0;
  margin-top: 10px;
}

.study-summary-tags {
  display: flex;
  flex-direction: column;
  gap: 10px; /* 태그 간격을 15px에서 10px로 줄임 */
  width: 80%;
  max-width: 250px;
}

.tag-item {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  gap: 10px; /* 태그와 숫자 사이 간격을 15px에서 10px로 줄임 */
  margin-bottom: 0px;
}

.tag {
  font-size: 0.9rem;
  font-weight: 550;
  padding: 8px 16px;
  border-radius: 10px;
  margin-top: 15px;
}

.perfect-tag {
  background-color: #e8f5e9; /* 이미지의 숫자 배경색과 동일한 연한 초록색 */
  color: #4CAF50; /* 이미지의 숫자 색상과 동일한 초록색 */
}

.vague-tag {
  background-color: #fffde7; /* 이미지의 숫자 배경색과 동일한 연한 노란색 */
  color: #FFC107; /* 이미지의 숫자 색상과 동일한 노란색 */
}

.forgotten-tag {
  background-color: #ffebee; /* 이미지의 숫자 배경색과 동일한 연한 빨간색 */
  color: #F44336; /* 이미지의 숫자 색상과 동일한 빨간색 */
}

.count {
  font-size: 1.15rem;
  font-weight: 700;
  color: white;
  line-height: 1.2;
  display: flex;
  align-items: center;
  position: relative;
  top: 8px;
}

.count.perfect-count-color {
  /* Removed color: #4CAF50; */
}
.count.vague-count-color {
  /* Removed color: #FFC107; */
}
.count.forgotten-count-color {
  /* Removed color: #F44336; */
}

.no-data-message,
.no-date-selected-message {
  font-size: 1rem; /* 폰트 크기를 1.1rem에서 1rem으로 줄임 */
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
  margin-top: 5px; /* 상단 마진 추가 */
}
</style>