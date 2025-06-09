<template>
  <div class="card" :class="getCardStatusClass(problem.cardStatus)">
    <div class="card-content with-image">
      <h3 class="card-question">{{ problem.question }}</h3>

      <!-- 항상 image-wrapper는 유지 -->
      <div class="image-wrapper">
        <template v-if="!problem.imageUrl">
          <!-- 말풍선을 이미지 위에 위치시키되, 일반 flow 안에서 먼저 배치 -->
          <div class="speech-bubble">정답을 맞춰봐!</div>
          <img
            :src="require('@/assets/mogwi-character.png')"
            class="problem-image"
            alt="모귀 캐릭터"
          />
        </template>
        <template v-else>
          <img
            :src="problem.imageUrl"
            class="problem-image"
            alt="문제 이미지"
          />
        </template>
      </div>

      <button
        v-if="hasSubmitted && !isCorrectAnswer"
        @click="$emit('toggle-show-answer')"
        class="show-answer-button"
      >
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
  border: 3px solid #a471ff;
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

.card-content {
  width: 100%;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-content.no-image {
  justify-content: center; /* 이미지 없으면 텍스트 중앙 정렬 */
  flex-grow: 1;
}

.card-content.with-image {
  justify-content: flex-start; /* 이미지 있으면 위에서 아래로 정렬 */
}

.card-question {
  font-size: 1.4rem;
  font-weight: bold;
  color: #333;
  line-height: 1.4;
  word-break: keep-all;
  margin-bottom: 10px;
  padding: 5px;
}

.problem-image {
  width: 100px; /* 기존보다 작게 */
  height: auto;
  border-radius: 8px;
  object-fit: contain;
  margin-top: 10px;
  margin-bottom: 10px;
}

/* 이미지 + 말풍선을 감싸는 래퍼 */
.image-wrapper {
  position: relative;
  display: flex;
  flex-direction: column; /* 세로 정렬 */
  justify-content: center;
  align-items: center;
  margin-bottom: 10px;
}

/* 말풍선 스타일 */
.speech-bubble {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
  margin: 0;
  background: #f3f4f6;
  padding: 0.75rem 1rem;
  border-radius: 1rem;
  position: relative; 
  max-width: 200px;
  text-align: center;
  margin-bottom: 6px; /* 모귀와 간격 약간 */
}

.speech-bubble::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid #f3f4f6;
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
