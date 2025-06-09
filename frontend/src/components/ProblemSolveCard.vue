<template>
  <div class="card" :class="getCardStatusClass(selectedStatus)">
    <div class="card-content with-image">
      <!-- 문제 -->
      <h3 class="card-question">{{ problem.question }}</h3>

      <!-- 이미지 + 말풍선 -->
      <div class="image-wrapper">
        <template v-if="!problem.imageUrl">
          <div class="speech-bubble">정답을 맞춰봐!</div>
          <img :src="require('@/assets/mogwi-character.png')" class="problem-image" alt="모귀 캐릭터" />
        </template>
        <template v-else>
          <img :src="problem.imageUrl" class="problem-image" alt="문제 이미지" />
        </template>
      </div>

      <!-- 기억 상태 선택 드롭다운 (오른쪽 상단) -->
      <select v-model="selectedStatus" class="status-dropdown" :disabled="!hasSubmitted">
        <option value="new" disabled>새로운 기억</option>
        <option value="perfect">완벽한 기억</option>
        <option value="vague">희미한 기억</option>
        <option value="forgotten">사라진 기억</option>
      </select>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue';

export default {
  props: {
    problem: { type: Object, required: true },
    showAnswer: { type: Boolean, default: false },
    hasSubmitted: { type: Boolean, default: false }
  },
  emits: ['toggle-show-answer', 'next'],
  setup(props) {
    const selectedStatus = ref(props.problem.cardStatus || 'new');

    const getCardStatusClass = (status) => {
      if (status === 'perfect') return 'card-perfect';
      if (status === 'vague') return 'card-vague';
      if (status === 'forgotten') return 'card-forgotten';
      if (status === 'new') return 'card-new';
      return '';
    };

    return {
      selectedStatus,
      getCardStatusClass
    };
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
  position: relative;
  width: 100%;
  box-sizing: border-box;
  min-height: 400px;
  transition: border-color 0.3s ease;
}

.card-perfect {
  border-color: #28a745; /* 초록 */
}

.card-vague {
  border-color: #ffc107; /* 노랑 */
}

.card-forgotten {
  border-color: #dc3545; /* 빨강 */
}

.card-new {
  border-color: #a471ff; /* 회색: 새로운 카드 */
}

.card-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: space-between;
  margin-top: 20px;
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

.image-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: auto;
  margin-top: auto;
}

.problem-image {
  width: 90px;
  height: auto;
  border-radius: 8px;
  object-fit: contain;
  margin: 8px 0;
}

.speech-bubble {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
  background: #f3f4f6;
  padding: 0.75rem 1rem;
  border-radius: 1rem;
  position: relative;
  max-width: 200px;
  text-align: center;
  margin-bottom: 6px;
}

.speech-bubble::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid #f3f4f6;
}

.status-dropdown {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 0.9rem;
  font-weight: bold;
  border-radius: 0.75rem;
  padding: 5px 12px;
  background-color: #f0e6ff;
  color: #5a2e87;
  border: none;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  cursor: pointer;
}
</style>
