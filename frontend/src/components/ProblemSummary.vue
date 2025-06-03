<template>
  <div class="problem-summary">
    <h3>{{ localProblem.title }}</h3>

    <p class="author"> 글 작성자: {{ localProblem.author }}</p>

    <div class="category-row">
      <div class="category-tags">
        <span
          class="tag"
          v-for="tag in localProblem.categories"
          :key="tag"
          :style="{ backgroundColor: getColor(tag) }"
        >
          {{ tag }}
        </span>
      </div>
      <button class="solve-btn" @click.stop="handleSolveClick">문제 풀기</button>
    </div>

    <div class="meta">
      <div class="meta-left">
        <span
          v-if="canLikeScrap"
          @click.stop="toggleLike"
          :class="['clickable', { active: localProblem.liked }]"
        >
          ❤️ {{ localProblem.likes }}
        </span>
        <span
          v-else
          class="clickable disabled"
        >
          ❤️ {{ localProblem.likes }}
        </span>

        <span
          v-if="canLikeScrap"
          @click.stop="toggleScrap"
          :class="['clickable', { active: localProblem.scrapped }]"
        >
          📌 {{ localProblem.scraps }}
        </span>
        <span
          v-else
          class="clickable disabled"
        >
          📌 {{ localProblem.scraps }}
        </span>

        <span>🃏 {{ localProblem.cardCount }} 카드</span>
      </div>

      <span
        v-if="isAuthenticated && localProblem.authorId === currentUserId"
        class="edit-button"
        @click.stop="handleEditClick"
      >
        🖋️ 수정
      </span>
    </div>
  </div>
</template>

<script>
export default {
  props: ['problem', 'isAuthenticated', 'currentUserId'],
  emits: ['auth-required', 'update-like', 'update-scrap'],
  data() {
    return {
      localProblem: { ...this.problem } // props 복사본
    }
  },
  computed: {
    canLikeScrap() {
      return this.isAuthenticated && this.localProblem.authorId !== this.currentUserId
    }
  },
  watch: {
    problem: {
      handler(newVal) {
        this.localProblem = { ...newVal }
      },
      deep: true
    }
  },
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
      if (this.isAuthenticated) {
        this.$router.push(`/study/${this.localProblem.id}`)
      } else {
        this.$emit('auth-required')
      }
    },
    toggleLike() {
      if (!this.canLikeScrap) return
      this.localProblem.liked = !this.localProblem.liked
      this.localProblem.likes += this.localProblem.liked ? 1 : -1
      this.$emit('update-like', this.localProblem)
    },
    toggleScrap() {
      if (!this.canLikeScrap) return
      this.localProblem.scrapped = !this.localProblem.scrapped
      this.localProblem.scraps += this.localProblem.scrapped ? 1 : -1
      this.$emit('update-scrap', this.localProblem)
    },
    handleEditClick() {
      this.$router.push(`/edit/${this.localProblem.id}`)
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
.author {
  margin: 4px 0 8px;
  font-size: 13px;
  color: #999;
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
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
}
.meta-left {
  display: flex;
  gap: 12px;
}
.edit-button {
  margin-left: auto;
  font-size: 13px;
  color: #888;
  cursor: pointer;
  transition: color 0.2s;
}
.edit-button:hover {
  color: #5f35b4;
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
.clickable.active {
  color: #e91e63;
  font-weight: bold;
}
.clickable.disabled {
  color: #aaa;
  cursor: not-allowed;
}
</style>
