<template>
  <div class="problem-summary">
    <h3>{{ problem.title }}</h3>
    <p>{{ problem.category }}</p>
    <div class="meta">
      <span>❤️ {{ problem.likes }}</span>
      <span>📌 {{ problem.scraps }}</span>
      <span>🃏 {{ problem.cardCount }} 카드</span>
    </div>
  </div>
</template>

<script>
export default {
  props: ['problem']
}
</script>

<style scoped>
.problem-summary {
  padding: 16px;
  border: 1px solid #ccc;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
}
.problem-summary h3 {
  margin: 0;
  font-size: 18px;
}
.meta {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  font-size: 14px;
  color: #666;
}
</style>