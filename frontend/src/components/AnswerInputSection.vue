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
  justify-content: center; /* input과 button을 중앙 정렬 */
  align-items: center; /* 세로 중앙 정렬 (선택 사항) */
  width: 100%; /* 부모(solve-section-container)의 100% 너비를 차지 */
  max-width: 400px; /* 최대 너비를 설정하여 너무 넓어지지 않도록 */
  margin-top: 20px; /* 상단 마진 유지 */
}

.answer-input {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 1rem;
  flex-grow: 1; /* 남은 공간을 채우도록 함 */
  max-width: 250px; /* 입력 필드 자체의 최대 너비 제한 (넘치지 않도록) */
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
  padding: 0.5rem 1rem; /* 세로 패딩을 늘려 버튼 높이 증가 */
  font-size: 0.875rem; /* StartButton의 font-size */
  font-weight: 600; /* StartButton의 font-weight */
  cursor: pointer;
  transition: background-color 0.2s; /* StartButton의 transition */
  line-height: 1; /* 텍스트 줄 간격을 조절하여 버튼 높이 줄이기 */
  text-align: center; /* 두 줄 텍스트 중앙 정렬 */
}

.submit-button:disabled {
  background-color: #c9b3ff; /* StartButton 색상에 맞춰 비활성화 색상 추정 */
  cursor: not-allowed;
}

.submit-button:hover:not(:disabled) {
  background-color: #8b5cf6; /* StartButton의 hover 색상 */
}
</style> 