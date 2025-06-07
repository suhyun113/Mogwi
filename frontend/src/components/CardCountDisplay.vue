<template>
  <div class="card-count-display">
    {{ currentCardIndex + 1 }} / {{ totalCards }}
  </div>
</template>

<script>
export default {
  props: {
    currentCardIndex: {
      type: Number,
      required: true
    },
    totalCards: {
      type: Number,
      required: true
    }
  }
};
</script>

<style scoped>
.card-count-display {
  font-size: 1.1rem; /* 폰트 크기 조정 */
  font-weight: bold;
  color: #5a2e87; /* 진한 보라색 텍스트 */
  margin-bottom: 25px; /* 카드와 간격 두기 */
  background-color: #fff4c1;
  padding: 8px 15px; /* 패딩 조정 */
  border-radius: 8px; /* 통일된 래디어스 값 */
  min-width: 80px; /* 숫자가 바뀌어도 최소 너비 유지 */
  text-align: center;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1); /* 그림자 추가 */
  border: 1px solid #ffe066;
}
</style> 