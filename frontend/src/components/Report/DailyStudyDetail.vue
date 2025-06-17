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
  height: 100%; /* 부모 컨테이너에 맞춰 높이 차지 */
  padding: 20px; /* 모든 방향으로 패딩 추가 */
  box-sizing: border-box;
  background-color: transparent; /* 배경색을 투명으로 설정 */
}

.detail-content {
  display: flex;
  flex-direction: column;
  align-items: center; /* 가로 중앙 정렬 */
  justify-content: center; /* 세로 공간에 내용 중앙 정렬 */
  width: 100%;
  height: 100%; /* 부모 높이 채우기 */
  flex-grow: 1; /* 남은 공간을 차지하도록 설정 */
}

.selected-date-display {
  font-size: 1.5rem;
  font-weight: 600;
  color: white; /* 텍스트 색상을 흰색으로 */
}

.study-summary-tags {
  display: flex;
  flex-direction: column; /* 세로 정렬 */
  gap: 15px; /* 태그 항목 간 간격 */
  width: 80%; /* 태그 컨테이너 너비 조정 */
  max-width: 250px; /* 최대 너비 설정 */
}

.tag-item {
  display: flex;
  justify-content: center; /* 태그와 개수를 중앙에 정렬 */
  align-items: center;
  width: 100%;
  gap: 15px; /* 태그와 숫자 사이에 간격 더 추가 */
}

.tag {
  font-size: 1rem;
  font-weight: 500;
  padding: 8px 15px;
  border-radius: 20px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2); /* 개별 태그에 그림자 추가 */
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
  font-size: 1.2rem;
  font-weight: 700;
  color: white; /* 개수 텍스트 색상을 흰색으로 통일 */
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
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.8); /* 흰색 투명도 적용 */
  text-align: center;
}
</style>