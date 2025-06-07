<template>
  <div class="card" :class="getCardStatusClass(problem.cardStatus)">
    <div class="card-header">
      <!-- 이 부분은 ProblemCard와 동일한 디자인을 위한 것이므로, problem_id만 표시하는 대신
           ProblemSolveCard의 목적에 맞게 문제(question)를 강조합니다. -->
    </div>
    <div class="card-content">
      <h3 class="card-question">{{ problem.question }}</h3>
      <!-- 정답 확인 후 정답을 보여줄 수 있는 버튼/로직 추가 -->
      <button v-if="hasSubmitted && !isCorrectAnswer" @click="$emit('toggle-show-answer')" class="show-answer-button">
        {{ showAnswer ? '정답 숨기기' : '정답 보기' }}
      </button>
      <div v-if="showAnswer && !isCorrectAnswer" class="correct-answer-display">
        정답: {{ problem.answer }}
      </div>
    </div>
    <div class="card-footer">
      <!-- ProblemSolveCard에서는 좋아요/스크랩 기능 불필요 -->
    </div>
  </div>
</template>

<script>
export default {
  props: {
    problem: {
      type: Object,
      required: true
    },
    hasSubmitted: {
      type: Boolean,
      default: false
    },
    isCorrectAnswer: {
      type: Boolean,
      default: false
    },
    showAnswer: {
      type: Boolean,
      default: false
    }
  },
  emits: ['toggle-show-answer'],
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
  border: 3px solid #a471ff; /* 메인 보라색 테두리 */
  border-radius: 0.75rem; /* 통일된 래디어스 값 */
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;
  aspect-ratio: 2.5/3.5; /* 세로로 긴 직사각형 비율 */
  position: relative;
  width: 100%; /* 부모 컨테이너의 max-width에 맞춰지도록 */
  box-sizing: border-box; /* padding과 border가 width/height에 포함되도록 */
  height: auto; /* 내용에 따라 높이 조절 */
  min-height: 250px; /* 최소 높이 설정 */
}

/* 카드 상태별 배경색 - 기존 ProblemSolveCard의 스타일을 유지하면서 border만 ProblemCard에 맞춥니다. */
.card-solved {
  background-color: #e6ffe6; /* 연한 초록색 */
  border-color: #7a4cb8; /* 진한 보라색 테두리 */
}
.card-unsolved {
  background-color: #f9f9f9;
  border-color: #7a4cb8; /* 진한 보라색 테두리 */
}
.card-review {
  background-color: #fff3e6; /* 연한 주황색 */
  border-color: #7a4cb8; /* 진한 보라색 테두리 */
}

.card-question {
  font-size: 1.4rem; /* 폰트 크기 약간 감소 */
  font-weight: bold;
  color: #333;
  line-height: 1.4;
  margin-top: 0.5rem;
  text-align: center;
  flex-grow: 1; /* 중앙 정렬을 위해 남은 공간을 차지하도록 */
  display: flex;
  align-items: center;
  justify-content: center;
  word-break: keep-all; /* 단어가 잘리지 않도록 */
  overflow-y: auto; /* 내용이 길 경우 스크롤 허용 */
  padding: 5px; /* 텍스트와 경계 사이 여백 */
}

.card-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.card-title {
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.card-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 0.85rem;
  color: #777;
}

.card-author {
  margin-bottom: 5px;
}

.card-category-tag {
  display: inline-block;
  background-color: #e0e0e0;
  padding: 4px 8px;
  border-radius: 5px;
  font-size: 0.75rem;
  color: #555;
  margin-top: 5px;
}

.card-content {
  width: 100%;
  text-align: center;
  margin-bottom: 15px;
}

.card-description {
  font-size: 1rem;
  line-height: 1.5;
  color: #555;
  white-space: pre-wrap; /* 줄바꿈 유지 */
}

.card-footer {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-top: 10px;
}

.card-footer-item {
  display: flex;
  align-items: center;
  font-size: 0.9rem;
  color: #777;
  cursor: pointer;
}

.card-footer-item img {
  width: 16px;
  height: 16px;
  margin-right: 5px;
}

.icon-liked, .icon-scrapped {
  width: 16px;
  height: 16px;
  margin-right: 5px;
  cursor: pointer;
}

.like-icon {
  filter: grayscale(100%); /* 기본적으로 회색 */
  transition: filter 0.2s;
}

.like-icon.active {
  filter: grayscale(0%) brightness(1.2) sepia(1) hue-rotate(-50deg) saturate(3); /* 색상 강조 */
}

.scrap-icon {
  filter: grayscale(100%); /* 기본적으로 회색 */
  transition: filter 0.2s;
}

.scrap-icon.active {
  filter: grayscale(0%) brightness(0.8) sepia(1) hue-rotate(90deg) saturate(3); /* 색상 강조 */
}

.show-answer-button {
  background-color: #c9b3ff; /* 연한 보라색 */
  color: #2c003e; /* 진한 보라색 글씨 */
  border: none;
  border-radius: 0.75rem; /* 통일된 래디어스 값 */
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.2s;
  margin-top: 15px; /* 정답 표시 버튼과 카드 내용 사이 간격 */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.show-answer-button:hover {
  background-color: #a471ff;
  color: white;
  transform: translateY(-2px);
}

.correct-answer-display {
  margin-top: 15px;
  font-size: 1.15rem; /* 정답 텍스트 크기 약간 증가 */
  color: #28a745; /* 초록색으로 강조 */
  font-weight: bold;
}

/* ProblemCard에서 가져왔지만 ProblemSolveCard에는 필요 없는 스타일은 제거하거나 주석 처리했습니다. */
/* .card-icon, .title (일부 속성만 재정의), .author, .tags, .tag, .divider, .description-container, .description, .icons, .icon, .icon img, .icon span, .mogwi-image, .empty-description, .mogwi-image-small, .encourage-message, .encourage-message::after */
</style> 