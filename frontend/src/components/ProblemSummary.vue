// ProblemSummary.vue
<template>
  <div class="problem-summary">
    <h3>{{ problem.title }}</h3>
    <div class="category-tags">
      <span
        class="tag"
        v-for="tag in problem.categories"
        :key="tag"
        :style="{ backgroundColor: getColor(tag) }"
      >
        {{ tag }}
      </span>
    </div>
    <div class="meta">
      <span>❤️ {{ problem.likes }}</span>
      <span>📌 {{ problem.scraps }}</span>
      <span>🃏 {{ problem.cardCount }} 카드</span>
    </div>
  </div>
</template>

<script>
export default {
  props: ['problem'],
  methods: {
    getColor(tag) {
      const colors = {
        '#수학': '#ffd54f',
        '#AI': '#81c784',
        '#컴퓨터': '#64b5f6',
        '#과학': '#4dd0e1',
        '#역사': '#a1887f',
        '#기타': '#e0e0e0',
        '#프론트엔드': '#ba68c8',
        '#자료구조': '#f06292'
      }
      return colors[tag] || '#ccc'
    }
  }
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
  margin: 0 0 6px;
  font-size: 18px;
}
.category-tags {
  margin-bottom: 6px;
}
.tag {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 13px;
  margin-right: 6px;
  color: white;
}
.meta {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  font-size: 14px;
  color: #666;
}
</style>
