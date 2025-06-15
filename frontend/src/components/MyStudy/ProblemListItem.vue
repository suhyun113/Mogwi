<template>
  <div class="problem-summary">
    <div class="title-row">
      <div class="title-left">
        <h3 class="problem-title">{{ localProblem.title }}</h3>
        <span class="author">작성자: {{ localProblem.authorNickname }}</span> </div>
      <div class="card-count">
        <img :src="cardIcon" alt="card icon" class="icon card-icon" />
        {{ localProblem.cardCount }} 카드
      </div>
    </div>

    <div class="category-row">
      <div class="category-tags">
        <ProblemTag
          v-for="tag in localProblem.categories"
          :key="tag"
          :tagName="tag"
          :backgroundColor="getColor(tag)"
        />
      </div>
      <div class="btn-wrapper">
        <button
          v-if="isAuthenticated && localProblem.authorId === currentUserId"
          class="edit-btn"
          @click.stop="handleEditClick"
        >
          수정
        </button>
        <button class="solve-btn" @click.stop="handleSolveClick">
            {{ localProblem.studyStatus === 'completed' ? '다시 풀기' : '문제 풀기' }}
        </button>
      </div>
    </div>

    <div class="meta">
      <div class="meta-left">
        <div v-if="isAuthenticated && localProblem.authorId !== currentUserId" class="icon-wrapper" @click.stop="toggleLike">
          <img :src="localProblem.isLiked ? heartOn : heartOff" alt="like" class="icon" />
          <span>{{ localProblem.totalLikes }}</span>
        </div>
        <div v-else class="icon-wrapper disabled">
          <img :src="localProblem.isLiked ? heartOn : heartOff" alt="like" class="icon" />
          <span>{{ localProblem.totalLikes }}</span>
        </div>

        <div v-if="isAuthenticated && localProblem.authorId !== currentUserId" class="icon-wrapper" @click.stop="toggleScrap">
          <img :src="localProblem.isScrapped ? scrapOn : scrapOff" alt="scrap" class="icon" />
          <span>{{ localProblem.totalScraps }}</span>
        </div>
        <div v-else class="icon-wrapper disabled">
          <img :src="localProblem.isScrapped ? scrapOn : scrapOff" alt="scrap" class="icon" />
          <span>{{ localProblem.totalScraps }}</span>
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
import ProblemTag from './ProblemTag.vue' // Import the ProblemTag component

export default {
  name: 'ProblemListItem', // Changed name to ProblemListItem
  components: {
    ProblemTag // Register ProblemTag for use in this template
  },
  props: {
    problem: { // The problem object comes from ProblemListSection
      type: Object,
      required: true
    },
    isAuthenticated: { // From parent
      type: Boolean,
      default: false
    },
    currentUserId: { // From parent
      type: String,
      default: ''
    }
  },
  // Define events this component can emit
  emits: ['auth-required', 'update-problem-data', 'go-to-study'], // 'update-problem-data' for any change, 'go-to-study' for solve button

  data() {
    return {
      localProblem: { ...this.problem }, // Create a local copy to manage reactivity for likes/scraps
      heartOff,
      heartOn,
      scrapOff,
      scrapOn,
      cardIcon
    }
  },
  // Watch for changes in the 'problem' prop from the parent
  // This ensures localProblem stays in sync if the parent updates the original problem object
  watch: {
    problem: {
      handler(newVal) {
        this.localProblem = { ...newVal }
      },
      deep: true, // Watch for nested changes in the problem object
      immediate: true // Run handler immediately on component mount
    }
  },
  methods: {
    // Method to get background color for tags
    getColor(tag) {
      // **태그 이름의 양쪽 공백을 제거하여 정확한 매칭을 유도합니다.**
      const trimmedTag = tag ? tag.trim() : ''; // null 또는 undefined 방지
      const colors = {
        '수학': '#ffd54f',
        'AI': '#81c784',
        '컴퓨터': '#64b5f6',
        '과학': '#4dd0e1',
        '역사': '#a1887f',
        '기타': '#e0e0e0',
        '프론트엔드': '#ba68c8',
        '자료구조': '#f06292',
        '전체': '#b0bec5'
      }
      // 일치하는 색상이 없으면 기본 회색을 반환합니다.
      return colors[trimmedTag] || '#ccc' 
    },
    async toggleLike() {
      // Logic for checking authentication and if it's the author's own problem
      if (!this.isAuthenticated) {
        this.$emit('auth-required'); // Emit event to parent to handle auth prompt
        return;
      }
      if (this.localProblem.authorId === this.currentUserId) {
        alert("본인이 작성한 문제는 좋아요/스크랩할 수 없습니다.");
        return;
      }

      const newLiked = !this.localProblem.isLiked;
      // Optimistic UI update
      this.localProblem.isLiked = newLiked;
      this.localProblem.totalLikes += newLiked ? 1 : -1;

      try {
        await axios.post(`/api/like/${this.localProblem.id}`, {
          userId: this.currentUserId,
          liked: newLiked
        });
        // On success, notify parent if needed (e.g., to update a global store or parent list)
        this.$emit('update-problem-data', this.localProblem);
      } catch (error) {
        console.error('좋아요 반영 실패:', error);
        // Rollback on error
        this.localProblem.isLiked = !newLiked;
        this.localProblem.totalLikes += newLiked ? -1 : 1;
        alert("좋아요 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
      }
    },
    async toggleScrap() {
      if (!this.isAuthenticated) {
        this.$emit('auth-required');
        return;
      }
      if (this.localProblem.authorId === this.currentUserId) {
        alert("본인이 작성한 문제는 좋아요/스크랩할 수 없습니다.");
        return;
      }

      const newScrapped = !this.localProblem.isScrapped;
      // Optimistic UI update
      this.localProblem.isScrapped = newScrapped;
      this.localProblem.totalScraps += newScrapped ? 1 : -1;

      try {
        await axios.post(`/api/scrap/${this.localProblem.id}`, {
          userId: this.currentUserId,
          scrapped: newScrapped
        });
        // On success, notify parent
        this.$emit('update-problem-data', this.localProblem);
      } catch (error) {
        console.error('스크랩 반영 실패:', error);
        // Rollback on error
        this.localProblem.isScrapped = !newScrapped;
        this.localProblem.totalScraps += newScrapped ? -1 : 1;
        alert("스크랩 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
      }
    },
    handleEditClick() {
      // Assuming you have Vue Router setup
      this.$router.push(`/edit/${this.localProblem.id}`);
    },
    handleSolveClick() {
      if (this.isAuthenticated) {
        this.$emit('go-to-study', this.localProblem.id); // Emit just the problem ID for navigation
      } else {
        this.$emit('auth-required'); // Ask parent to handle authentication
      }
    }
  }
}
</script>

<style scoped>
.problem-summary {
  padding: 16px;
  border: 1px solid #e0d0ff; /* Adjusted from #ccc for theme consistency */
  border-radius: 8px;
  background: #ffffff; /* Changed from #fafafa for brightness */
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 15px; /* Add margin between items */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); /* Softer shadow */
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.problem-summary:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.problem-summary h3 {
  margin: 0;
  font-size: 18px;
  color: #5a2e87; /* Dark purple for titles */
}
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.title-left {
  display: flex;
  align-items: baseline; /* Align problem title and author nicely */
  gap: 12px;
}
.problem-title {
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%; /* Prevent title from pushing author too much */
}
.author {
  font-size: 13px;
  color: #888;
  white-space: nowrap;
  flex-shrink: 0; /* Prevent author from shrinking */
}
.card-count {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.category-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap; /* Allow tags to wrap */
  gap: 8px; /* Gap for category tags and button wrapper */
}
.category-tags {
  display: flex;
  flex-wrap: wrap; /* Allow tags to wrap within their container */
  gap: 8px;
  flex-grow: 1; /* Allow tags to take up available space */
}
/* .tag style is now handled by ProblemTag.vue */

.meta {
  margin-top: 10px;
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
  cursor: pointer;
  transition: transform 0.2s ease;
}
.icon-wrapper:hover {
  transform: translateY(-2px);
}
.icon-wrapper.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none; /* Disable click events entirely */
}
.icon {
  width: 20px;
  height: 20px;
}
.card-icon {
  width: 18px;
  height: 18px;
}
.btn-wrapper {
  display: flex;
  gap: 6px;
  flex-shrink: 0; /* Prevent buttons from shrinking */
}
.edit-btn, .solve-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  white-space: nowrap;
  font-size: 14px;
  transition: background-color 0.2s ease, transform 0.1s ease;
}
.edit-btn {
  background-color: #ffc107; /* Brighter yellow */
  color: #343a40;
}
.edit-btn:hover {
  background-color: #e0a800;
  transform: translateY(-1px);
}
.solve-btn {
  background-color: #a471ff;
  color: white;
}
.solve-btn:hover {
  background-color: #854fe6;
  transform: translateY(-1px);
}

/* Responsive adjustments */
@media (max-width: 576px) {
    .title-row {
        flex-direction: column;
        align-items: flex-start;
        gap: 4px;
    }
    .problem-title {
        max-width: 100%;
    }
    .category-row {
        flex-direction: column;
        align-items: flex-start;
        gap: 10px;
    }
    .btn-wrapper {
        width: 100%;
        justify-content: flex-end;
    }
    .edit-btn, .solve-btn {
        flex-grow: 1; /* Make buttons take full width of wrapper on small screens */
    }
}
</style>