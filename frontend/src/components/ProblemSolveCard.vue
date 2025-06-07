<template>
  <div class="card" :class="getCardStatusClass(problem.cardStatus)">
    <h3 class="card-question">{{ problem.question }}</h3>
    <!-- 현재 카드 인덱스와 총 카드 수를 표시합니다. -->
    <div class="card-count">{{ currentCardIndex + 1 }} / {{ totalCards }}</div>
    <!-- 정답 확인 후 정답을 보여줄 수 있는 버튼/로직 추가 -->
    <button v-if="showAnswer && !isCorrectAnswer" @click="$emit('toggle-show-answer')" class="show-answer-button">정답 숨기기</button>
    <button v-else-if="!showAnswer && !isCorrectAnswer && hasSubmitted" @click="$emit('toggle-show-answer')" class="show-answer-button">정답 보기</button>
    <div v-if="showAnswer && !isCorrectAnswer" class="correct-answer-display">
      정답: {{ problem.answer }}
    </div>
  </div>
</template>

<script>
export default {
  props: {
    problem: { // 현재 카드의 문제 객체 (question, answer, cardStatus 등 포함)
      type: Object,
      required: true
    },
    currentCardIndex: { // 현재 카드 인덱스
      type: Number,
      required: true
    },
    totalCards: { // 총 카드 수
      type: Number,
      required: true
    },
    hasSubmitted: { // 정답 제출 여부
      type: Boolean,
      required: true
    },
    isCorrectAnswer: { // 현재 카드의 정답 여부
      type: Boolean,
      required: true
    },
    showAnswer: { // 정답 표시 여부
      type: Boolean,
      required: true
    }
  },
  emits: ['toggle-show-answer'], // 정답 보기/숨기기 이벤트를 부모로 전달

  methods: {
    getCardStatusClass(status) {
      if (status === 'solved') {
        return 'card-solved';
      } else if (status === 'review') {
        return 'card-review';
      } else if (status === 'unsolved') {
        return 'card-unsolved';
      }
      return '';
    }
  }
};
</script>

<style scoped>
/* ProblemCard.vue의 스타일을 그대로 가져옵니다. */
.card {
  background: white;
  border: 3px solid #a471ff;
  border-radius: 1rem;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;
  aspect-ratio: 2.5/3.5; /* 세로로 긴 직사각형 비율 */
  position: relative;
  width: 100%; /* 부모 컨테이너의 max-width에 맞춰지도록 */
  box-sizing: border-box; /* padding과 border가 width/height에 포함되도록 */
}

/* 카드 상태별 배경색 - 기존 ProblemSolveCard의 스타일을 유지하면서 border만 ProblemCard에 맞춥니다. */
.card-solved {
  background-color: #e6ffe6; /* 연한 초록색 */
  border: 3px solid #8A2BE2; /* 보라색 테두리 */
}
.card-unsolved {
  background-color: #f9f9f9;
  border: 3px solid #8A2BE2; /* 보라색 테두리 */
}
.card-review {
  background-color: #fff3e6; /* 연한 주황색 */
  border: 3px solid #8A2BE2; /* 보라색 테두리 */
}

.card-question {
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
  line-height: 1.4; /* ProblemCard의 title과 유사하게 */
  margin-top: 0.5rem; /* ProblemCard의 title과 유사하게 */
  text-align: center;
  flex-grow: 1; /* 중앙 정렬을 위해 남은 공간을 차지하도록 */
  display: flex;
  align-items: center;
  justify-content: center;
  /* overflow: hidden; */ /* 내용이 길 경우 스크롤이 필요할 수 있습니다. */
  /* word-break: keep-all; */ /* 단어가 잘리지 않도록 */
}

.card-count {
  position: absolute;
  top: 1rem;
  right: 1rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: #6b7280;
  font-size: 0.875rem;
}

.show-answer-button {
  background-color: #6c757d; /* 회색 */
  margin-top: 10px;
  padding: 10px 15px;
  border: none;
  border-radius: 5px;
  color: white;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.show-answer-button:hover {
  background-color: #5a6268;
}

.correct-answer-display {
  margin-top: 10px;
  font-size: 1.1rem;
  color: #28a745;
  font-weight: bold;
}

/* ProblemCard에서 가져왔지만 ProblemSolveCard에는 필요 없는 스타일은 제거하거나 주석 처리했습니다. */
/* .card-icon, .title (일부 속성만 재정의), .author, .tags, .tag, .divider, .description-container, .description, .icons, .icon, .icon img, .icon span, .mogwi-image, .empty-description, .mogwi-image-small, .encourage-message, .encourage-message::after */
</style> 