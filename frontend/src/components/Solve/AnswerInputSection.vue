<template>
  <div class="answer-section">
    <input
      type="text"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="$emit('submit-answer')"
      placeholder="정답을 입력하세요"
      class="answer-input"
      :disabled="isDisabled"
    />
    <button @click="$emit('submit-answer')" class="submit-button" :disabled="isDisabled">정답<br>제출</button>
  </div>
  <transition name="fade">
    <div v-if="hasSubmitted" class="answer-toggle-wrapper">
      <div class="answer-toggle-row">
        <button class="toggle-answer-btn" @click="$emit('toggle-show-answer')">
          <template v-if="showAnswer">정답<br>숨기기</template>
          <template v-else>정답<br>보기</template>
        </button>
        <div v-if="showAnswer" class="correct-answer-card">
          <span class="answer-label">정답</span>
          <span class="answer-text">{{ correctAnswer }}</span>
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
export default {
  props: {
    modelValue: { // v-model을 위한 prop
      type: String,
      default: ''
    },
    isDisabled: { // 입력란 및 버튼 비활성화 여부
      type: Boolean,
      default: false
    },
    correctAnswer: {
      type: String,
      default: ''
    },
    showAnswer: {
      type: Boolean,
      default: false
    },
    hasSubmitted: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'submit-answer', 'toggle-show-answer']
};
</script>

<style scoped>
.answer-section {
  display: flex;
  gap: 10px;
  width: 100%;
  margin-top: 20px; /* 카드와 정답 입력란 사이 간격 증가 */
}

.answer-input {
  flex-grow: 1;
  padding: 10px;
  border: 2px solid #ddd;
  border-radius: 10px; /* 래디어스 값 통일 */
  font-size: 1rem;
}

.answer-input:focus {
  outline: none;
  border-color: #7a4cb8; /* 포커스 시 진한 보라색 테두리 */
}

.answer-input:disabled {
  background-color: #e9ecef;
  cursor: not-allowed;
}

.submit-button {
  background-color: #a471ff;
  color: white;
  border: none;
  border-radius: 10px;
  padding: 0.375rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
}

.submit-button:disabled {
  background-color: #c9b3ff;
  cursor: not-allowed;
  box-shadow: none;
}

.answer-toggle-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-top: 18px;
  width: 100%;
}

/* 버튼과 정답 카드가 한 행에 나란히 */
.answer-toggle-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  width: 100%;
  justify-content: flex-start;
  min-height: 36px;
}

.toggle-answer-btn {
  background: #a471ff;
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 8px 13px;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 0;
  min-width: 80px;
  max-width: 120px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(140, 93, 255, 0.08);
  height: 100%;
  display: flex;
  align-items: center;
}

.correct-answer-card {
  background: #fff;
  border: 1.2px solid #e0d0ff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(164, 113, 255, 0.06);
  padding: 9px 13px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  max-width: 100%;
  flex-grow: 1;
  font-size: 0.98rem;
  color: #5a2e87;
  margin-top: 2px;
  animation: fadeInCard 0.3s;
}

.answer-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #a471ff;
  margin-bottom: 2px;
  letter-spacing: 0.02em;
}

.answer-text {
  font-size: 1.01rem;
  font-weight: 600;
  color: #4a1e77;
  word-break: break-all;
}

@keyframes fadeInCard {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style> 