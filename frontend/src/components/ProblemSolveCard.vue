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
        정답: {{ problem.correct }}
      </div>
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
.card {
  background: white;
  border: 3px solid #a471ff; /* 메인 보라색 테두리 */
  border-radius: 0.75rem;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  aspect-ratio: 2.5/3.5;
  position: relative;
  width: 100%;
  box-sizing: border-box;
  height: auto;
  min-height: 250px;
}

/* 카드 상태별 배경색 */
.card-solved {
  background-color: #e6ffe6;
  border-color: #7a4cb8;
}
.card-unsolved {
  background-color: #f9f9f9;
  border-color: #7a4cb8;
}
.card-review {
  background-color: #fff3e6;
  border-color: #7a4cb8;
}

.card-question {
  font-size: 1.4rem;
  font-weight: bold;
  color: #333;
  line-height: 1.4;
  margin-top: 0.5rem;
  text-align: center;
  flex-grow: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  word-break: keep-all;
  overflow-y: auto;
  padding: 5px;
}

.card-content {
  width: 100%;
  text-align: center;
  margin-bottom: 15px;
}

.show-answer-button {
  background-color: #c9b3ff;
  color: #2c003e;
  border: none;
  border-radius: 0.75rem;
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.2s;
  margin-top: 15px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.show-answer-button:hover {
  background-color: #a471ff;
  color: white;
  transform: translateY(-2px);
}

.correct-answer-display {
  margin-top: 15px;
  font-size: 1.15rem;
  color: #28a745;
  font-weight: bold;
}
</style>
