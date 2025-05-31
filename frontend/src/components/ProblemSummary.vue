<template>
  <div class="problem-summary">
    <h3>{{ problem.title }}</h3>

    <div class="category-row">
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
      <button class="solve-btn" @click.stop="handleSolveClick"> 문제 풀기 </button>
    </div>

    <div class="meta">
      <span @click.stop="handleLikeClick" class="clickable">❤️ {{ problem.likes }}</span>
      <span @click.stop="handleScrapClick" class="clickable">📌 {{ problem.scraps }}</span>
      <span>🃏 {{ problem.cardCount }} 카드</span>
    </div>
  </div>
</template>

<script>
export default {
  props: ['problem', 'isAuthenticated'],
  emits: ['solve', 'auth-required', 'update-like', 'update-scrap'],
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
        '#자료구조': '#f06292',
        '#전체': '#b0bec5'
      }
      return colors[tag] || '#ccc'
    },
    handleSolveClick() {
      this.$emit('solve', this.problem)
    },
    handleLikeClick() {
      if (this.isAuthenticated) {
        this.$emit('update-like', this.problem)
      } else {
        this.$emit('auth-required')
      }
    },
    handleScrapClick() {
      if (this.isAuthenticated) {
        this.$emit('update-scrap', this.problem)
      } else {
        this.$emit('auth-required')
      }
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
  width: 100%;
  box-sizing: border-box;
}
.problem-summary h3 {
  margin: 0 0 6px;
  font-size: 18px;
}
.category-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.category-tags {
  margin-bottom: 5px;
}
.tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 14px;
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
.solve-btn {
  padding: 6px 10px;
  background-color: #a471ff;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  white-space: nowrap;
}
.solve-btn:hover {
  background-color: #854fe6;
}
.clickable {
  cursor: pointer;
}
</style>
