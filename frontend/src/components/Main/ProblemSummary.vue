<template>
  <div class="problem-summary">
    <div class="title-row">
      <div class="title-left">
        <h3 class="problem-title">{{ localProblem.title }}</h3>
        <span class="author">작성자: {{ localProblem.authorId }}</span>
      </div>
      <div class="card-count">
        <img :src="cardIcon" alt="card icon" class="icon card-icon" />
        {{ localProblem.cardCount }} 카드
      </div>
    </div>

    <div class="category-row">
      <div class="category-tags">
        <span
          class="tag"
          v-for="categoryObj in localProblem.categories"
          :key="categoryObj.tag_name"
          :style="{ backgroundColor: categoryObj.color_code }"
        >
          {{ displayTagName(categoryObj.tag_name) }}
        </span>
      </div>
      <div class="btn-wrapper">
        <button
          v-if="isAuthenticated && localProblem.authorId === currentUserId"
          class="edit-btn"
          @click.stop="handleEditClick"
        >
          수정
        </button>
        <button class="solve-btn" @click.stop="handleSolveClick">문제 풀기</button>
      </div>
    </div>

    <div class="meta">
      <div class="meta-left">
        <div v-if="canLikeScrap" class="icon-wrapper" @click.stop="toggleLike">
          <img :src="localProblem.liked ? heartOn : heartOff" alt="like" class="icon" />
          <span>{{ localProblem.likes }}</span>
        </div>
        <div v-else class="icon-wrapper disabled">
          <img :src="heartOff" alt="like" class="icon" />
          <span>{{ localProblem.likes }}</span>
        </div>

        <div v-if="canLikeScrap" class="icon-wrapper" @click.stop="toggleScrap">
          <img :src="localProblem.scrapped ? scrapOn : scrapOff" alt="scrap" class="icon" />
          <span>{{ localProblem.scraps }}</span>
        </div>
        <div v-else class="icon-wrapper disabled">
          <img :src="scrapOff" alt="scrap" class="icon" />
          <span>{{ localProblem.scraps }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import heartOff from '@/assets/icons/like_outline.png'
import heartOn from '@/assets/icons/like_filled.png'
import scrapOff from '@/assets/icons/scrap_outline.png'
import scrapOn from '@/assets/icons/scrap_filled.png'
import cardIcon from '@/assets/icons/card.png'

export default {
  props: ['problem', 'isAuthenticated', 'currentUserId'],
  emits: ['auth-required', 'update-like', 'update-scrap'],
  data() {
    return {
      localProblem: { ...this.problem },
      heartOff,
      heartOn,
      scrapOff,
      scrapOn,
      cardIcon
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
        console.log('ProblemSummary - localProblem.categories:', this.localProblem.categories);
      },
      deep: true
    }
  },
  methods: {
    displayTagName(tagName) {
      // Remove the '#' prefix if it exists
      return tagName.startsWith('#') ? tagName.substring(1) : tagName;
    },
    getColor(tag) {
      return tag.color_code || '#ccc';
    },
    toggleLike() {
      if (!this.canLikeScrap) return

      const newLiked = !this.localProblem.liked
      this.localProblem.liked = newLiked
      this.localProblem.likes += newLiked ? 1 : -1

      axios.post(`/api/like/${this.localProblem.id}`, {
        userId: this.currentUserId
      }).then(() => {
        this.$emit('update-like', this.localProblem)
      }).catch((error) => {
        console.error('좋아요 반영 실패:', error)
        this.localProblem.liked = !newLiked
        this.localProblem.likes += newLiked ? -1 : 1
      })
    },
    toggleScrap() {
      if (!this.canLikeScrap) return

      const newScrapped = !this.localProblem.scrapped
      this.localProblem.scrapped = newScrapped
      this.localProblem.scraps += newScrapped ? 1 : -1

      axios.post(`/api/scrap/${this.localProblem.id}`, {
        userId: this.currentUserId
      }).then(() => {
        this.$emit('update-scrap', this.localProblem)
      }).catch(() => {
        this.localProblem.scrapped = !newScrapped
        this.localProblem.scraps += newScrapped ? -1 : 1
      })
    },
    handleEditClick() {
      this.$router.push(`/edit/${this.localProblem.id}`)
    },
    handleSolveClick() {
      if (this.isAuthenticated) {
        this.$emit('solve', this.localProblem) // 전체 problem 객체 전달
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
  margin: 0;
  font-size: 18px;
}
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.problem-title {
  font-size: 18px;
  font-weight: bold;
}
.author {
  font-size: 13px;
  color: #888;
  white-space: nowrap;
}
.card-count {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}
.category-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0;
}
.category-tags {
  display: flex;
  gap: 8px;
}
.tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 14px;
  font-size: 13px;
  color: white;
}
.meta {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
}
.meta-left {
  display: flex;
  gap: 16px;
}
.icon-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
}
.icon-wrapper.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.icon {
  width: 20px;
  height: 20px;
  cursor: pointer;
  transition: transform 0.2s ease;
}
.icon:hover {
  transform: scale(1.1);
}
.card-icon {
  width: 18px;
  height: 18px;
}
.btn-wrapper {
  display: flex;
  gap: 6px;
}
.edit-btn {
  padding: 6px 10px;
  background-color: #fff4c1;
  color: #4a3f69;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  white-space: nowrap;
  font-size: 14px;
  transition: background-color 0.2s ease;
}
.edit-btn:hover {
  background-color: #ffe066;
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
</style>
