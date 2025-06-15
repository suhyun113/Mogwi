<template>
  <div class="problem-summary">
    <div class="title-row">
      <div class="title-left">
        <h3 class="problem-title">{{ localProblem.title }}</h3>
        <span class="author">작성자: {{ localProblem.authorNickname }}</span>
      </div>
      <div class="card-count">
        <img :src="cardIcon" alt="card icon" class="icon card-icon" />
        {{ localProblem.cardCount }} 카드
      </div>
      <div class="study-status">
        <span :class="getStatusClass(localProblem.studyStatus)">
          {{ getStatusText(localProblem.studyStatus) }}
        </span>
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
import ProblemTag from './ProblemTag.vue'

export default {
  name: 'ProblemListItem',
  components: {
    ProblemTag
  },
  props: {
    problem: {
      type: Object,
      required: true
    },
    isAuthenticated: {
      type: Boolean,
      default: false
    },
    currentUserId: {
      type: String,
      default: ''
    }
  },
  emits: ['auth-required', 'update-problem-data', 'go-to-study'],

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
  watch: {
    problem: {
      handler(newVal) {
        this.localProblem = { ...newVal }
      },
      deep: true,
      immediate: true
    }
  },
  methods: {
    getColor(tag) {
      const trimmedTag = tag ? tag.trim() : '';
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
      return colors[trimmedTag] || '#ccc'
    },
    // 학습 상태 텍스트 반환 메서드 추가
    getStatusText(status) {
      switch (status) {
        case 'new':
          return '진행 전';
        case 'ongoing':
          return '진행 중';
        case 'completed':
          return '완료';
        default:
          return ''; // 알 수 없는 상태일 경우
      }
    },
    // 학습 상태에 따른 CSS 클래스 반환 메서드 추가
    getStatusClass(status) {
      switch (status) {
        case 'new':
          return 'status-new';
        case 'ongoing':
          return 'status-ongoing';
        case 'completed':
          return 'status-completed';
        default:
          return '';
      }
    },
    async toggleLike() {
      if (!this.isAuthenticated) {
        this.$emit('auth-required');
        return;
      }
      if (this.localProblem.authorId === this.currentUserId) {
        alert("본인이 작성한 문제는 좋아요/스크랩할 수 없습니다.");
        return;
      }

      const newLiked = !this.localProblem.isLiked;
      this.localProblem.isLiked = newLiked;
      this.localProblem.totalLikes += newLiked ? 1 : -1;

      try {
        await axios.post(`/api/like/${this.localProblem.id}`, {
          userId: this.currentUserId,
          liked: newLiked
        });
        this.$emit('update-problem-data', this.localProblem);
      } catch (error) {
        console.error('좋아요 반영 실패:', error);
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
      this.localProblem.isScrapped = newScrapped;
      this.localProblem.totalScraps += newScrapped ? 1 : -1;

      try {
        await axios.post(`/api/scrap/${this.localProblem.id}`, {
          userId: this.currentUserId,
          scrapped: newScrapped
        });
        this.$emit('update-problem-data', this.localProblem);
      } catch (error) {
        console.error('스크랩 반영 실패:', error);
        this.localProblem.isScrapped = !newScrapped;
        this.localProblem.totalScraps += newScrapped ? -1 : 1;
        alert("스크랩 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
      }
    },
    handleEditClick() {
      this.$router.push(`/edit/${this.localProblem.id}`);
    },
    handleSolveClick() {
      if (this.isAuthenticated) {
        this.$emit('go-to-study', this.localProblem.id);
      } else {
        this.$emit('auth-required');
      }
    }
  }
}
</script>

<style scoped>
.problem-summary {
  padding: 16px;
  border: 1px solid #e0d0ff;
  border-radius: 8px;
  background: #ffffff;
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 15px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.problem-summary:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.problem-summary h3 {
  margin: 0;
  font-size: 18px;
  color: #5a2e87;
}
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  /* 학습 상태 추가로 인한 flex-wrap 및 gap 추가 */
  flex-wrap: wrap;
  gap: 8px;
}
.title-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.problem-title {
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}
.author {
  font-size: 13px;
  color: #888;
  white-space: nowrap;
  flex-shrink: 0;
}
.card-count {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  flex-shrink: 0; /* 카운트가 줄어들지 않도록 설정 */
}
/* 학습 상태 스타일 추가 */
.study-status {
  font-size: 13px;
  font-weight: bold;
  padding: 4px 8px;
  border-radius: 5px;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-new {
  background-color: #e0e0e0; /* 회색 (진행 전) */
  color: #616161;
}

.status-ongoing {
  background-color: #ffe082; /* 노란색 (진행 중) */
  color: #c66900;
}

.status-completed {
  background-color: #a5d6a7; /* 초록색 (완료) */
  color: #2e7d32;
}


.category-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 8px;
}
.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex-grow: 1;
}

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
  pointer-events: none;
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
  flex-shrink: 0;
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
  background-color: #ffc107;
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
    .study-status { /* 작은 화면에서 오른쪽 정렬 */
        align-self: flex-end;
        margin-top: 4px;
    }
    .card-count { /* 작은 화면에서 study-status와 줄바꿈되도록 여유 공간 부여 */
        align-self: flex-start;
        margin-bottom: 4px;
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
        flex-grow: 1;
    }
}
</style>