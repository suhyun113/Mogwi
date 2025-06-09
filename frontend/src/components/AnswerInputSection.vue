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
    }
  },
  emits: ['update:modelValue', 'submit-answer'] // v-model 및 정답 제출 이벤트를 부모로 전달
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
  border-radius: 0.75rem; /* 래디어스 값 통일 */
  font-size: 1rem;
}

.answer-input:focus {
  outline: none;
  border-color: #7a4cb8; /* 포커스 시 진한 보라색 테두리 */
  /* 그림자 없이 진한 보라색 테두리만 */
}

.answer-input:disabled {
  background-color: #e9ecef;
  cursor: not-allowed;
}

.submit-button {
  background-color: #a471ff; /* StartButton의 배경색 */
  color: white;
  border: none;
  border-radius: 0.5rem; /* StartButton의 border-radius */
  padding: 0.375rem 1rem; /* StartButton의 padding */
  font-size: 0.875rem; /* StartButton의 font-size */
  font-weight: 600; /* StartButton의 font-weight */
  cursor: pointer;
  transition: background-color 0.2s; /* StartButton의 transition */
  flex-shrink: 0;
}

.submit-button:disabled {
  background-color: #c9b3ff; /* StartButton 색상에 맞춰 비활성화 색상 추정 */
  cursor: not-allowed;
  box-shadow: none;
}

.submit-button:hover:not(:disabled) {
  background-color: #8b5cf6; /* StartButton의 hover 색상 */
  transform: translateY(-2px); /* 호버 시 살짝 올라가는 효과 */
}
</style> 