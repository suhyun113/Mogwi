<template>
  <div class="problem-summary">
    <div class="title-row">
      <div class="title-left">
        <h3 class="problem-title">{{ localProblem.title }}</h3>
        <span class="author">작성자: {{ localProblem.authorNickname }}</span>
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

      <div class="meta-right-group">
        <div class="study-card-summary">
          <span class="perfect-count" :title="`${localProblem.perfectCount}개 - 완벽한 기억`">{{ localProblem.perfectCount }}</span> /
          <span class="vague-count" :title="`${localProblem.vagueCount}개 - 희미한 기억`">{{ localProblem.vagueCount }}</span> /
          <span class="forgotten-count" :title="`${localProblem.forgottenCount}개 - 사라진 기억`">{{ localProblem.forgottenCount }}</span>
        </div>
        <div class="card-count meta-card-count">
          <img :src="cardIcon" alt="card icon" class="icon card-icon" />
          {{ localProblem.cardCount }} 카드
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
      return colors[trimmedTag] || '#ccc'
    },
    getStatusText(status) {
      switch (status) {
        case 'new':
          return '진행 전';
        case 'ongoing':
          return '진행 중';
        case 'completed':
          return '완료';
        default:
          return '';
      }
    },
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
/* 기존 .card-count 스타일은 .meta-card-count로 이동 */
/* .card-count {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  flex-shrink: 0;
} */

.study-status {
  font-size: 13px;
  font-weight: bold;
  padding: 4px 8px;
  border-radius: 5px;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-new {
  background-color: #e0e0e0;
  color: #616161;
}

.status-ongoing {
  background-color: #ffe082;
  color: #c66900;
}

.status-completed {
  background-color: #a5d6a7;
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
  justify-content: space-between; /* meta-left와 meta-right-group을 양 끝으로 정렬 */
  align-items: center;
  font-size: 14px;
  color: #666;
  flex-wrap: wrap; /* 반응형을 위해 추가 */
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

/* 추가된 카드별 학습 상태 스타일 */
.study-card-summary {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: bold;
  white-space: nowrap;
}

.perfect-count {
  color: #4CAF50;
}

.vague-count {
  color: #FFC107;
}

.forgotten-count {
  color: #F44336;
}

/* 새로 추가된 카드 수 스타일 */
.meta-card-count {
    font-size: 13px;
    color: #666;
    display: flex;
    align-items: center;
    gap: 4px;
    white-space: nowrap;
    flex-shrink: 0;
}

/* meta 내의 오른쪽 정렬을 위한 그룹 */
.meta-right-group {
    display: flex;
    align-items: center;
    gap: 12px; /* 카드 상태 개수와 총 카드 수 사이의 간격 */
    margin-left: auto; /* meta-left 옆에서 오른쪽으로 정렬 */
    flex-wrap: wrap; /* 작은 화면에서 줄바꿈되도록 */
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
    .study-status {
        align-self: flex-end;
        margin-top: 4px;
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
    /* 작은 화면에서 meta 정렬 */
    .meta {
        flex-direction: column;
        align-items: flex-start; /* 좋아요/스크랩은 왼쪽에 */
        gap: 8px;
    }
    .meta-right-group {
        margin-left: 0; /* 왼쪽 정렬 해제 */
        width: 100%; /* 전체 너비 차지 */
        justify-content: flex-end; /* 오른쪽 끝으로 정렬 */
    }
    /* study-card-summary의 margin-left: auto;를 초기화 */
    .study-card-summary {
        margin-left: 0;
    }
}
</style>