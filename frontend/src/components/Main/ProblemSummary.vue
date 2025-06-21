<template>
  <div class="problem-summary">
    <div class="title-row">
      <div class="title-left">
        <h3 class="problem-title">{{ localProblem.title }}</h3>
        <span class="author">작성자: {{ localProblem.authorName }}</span>
      </div>
      <div class="study-status">
          <span :class="getStatusClass(localProblem.studyStatus)">
              {{ getStatusText(localProblem.studyStatus) }}
          </span>
      </div>
    </div>

    <div class="category-row">
      <div class="category-tags">
        <span
          class="tag"
          v-for="categoryObj in localProblem.categories"
          :key="categoryObj.id" :style="{ backgroundColor: categoryObj.color_code }"
        >
          #{{ displayTagName(categoryObj.tag_name) }}
        </span>
      </div>
      <div class="btn-wrapper">
        <button
          v-if="isAuthenticated && localProblem.authorId == currentUserId"
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
        <div v-if="canLikeScrap" class="icon-wrapper" @click.stop="toggleLike">
          <img :src="localProblem.isLiked ? heartOn : heartOff" alt="like" class="icon" />
          <span>{{ localProblem.totalLikes }}</span>
        </div>
        <div v-else class="icon-wrapper disabled">
          <img :src="heartOff" alt="like" class="icon" />
          <span>{{ localProblem.totalLikes }}</span>
        </div>

        <div v-if="canLikeScrap" class="icon-wrapper" @click.stop="toggleScrap">
          <img :src="localProblem.isScrapped ? scrapOn : scrapOff" alt="scrap" class="icon" />
          <span>{{ localProblem.totalScraps }}</span>
        </div>
        <div v-else class="icon-wrapper disabled">
          <img :src="scrapOff" alt="scrap" class="icon" />
          <span>{{ localProblem.totalScraps }}</span>
        </div>
      </div>

      <div class="meta-right-group">
        <div class="study-card-summary" v-if="localProblem.studyStatus !== 'new'">
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

export default {
  // ProblemListItem과 동일하게 prop 타입 정의를 좀 더 명확하게 했습니다.
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
      type: [String, Number], // 백엔드 author_id가 String 또는 Number일 수 있으므로 유연하게 설정
      default: null
    }
  },
  emits: ['auth-required', 'update-like', 'update-scrap', 'solve'], // 'solve' 이벤트 추가

  data() {
    return {
      // problem prop의 기본값을 명시적으로 설정하여 localProblem 초기화 시 누락 방지
      localProblem: {
        id: this.problem.id || null,
        title: this.problem.title || '제목 없음',
        authorNickname: this.problem.authorNickname || '알 수 없음', // 백엔드 응답 authorNickname에 맞춤
        authorId: this.problem.authorId || null,
        studyStatus: this.problem.studyStatus || 'new', // 학습 상태 추가
        categories: this.problem.categories || [],
        isLiked: this.problem.isLiked || false, // 'liked' -> 'isLiked' 백엔드 응답과 일치
        isScrapped: this.problem.isScrapped || false, // 'scrapped' -> 'isScrapped' 백엔드 응답과 일치
        totalLikes: this.problem.totalLikes || 0, // 'likes' -> 'totalLikes' 백엔드 응답과 일치
        totalScraps: this.problem.totalScraps || 0, // 'scraps' -> 'totalScraps' 백엔드 응답과 일치
        perfectCount: this.problem.perfectCount || 0, // 학습 통계 추가
        vagueCount: this.problem.vagueCount || 0,
        forgottenCount: this.problem.forgottenCount || 0,
        cardCount: this.problem.cardCount || 0,
      },
      heartOff,
      heartOn,
      scrapOff,
      scrapOn,
      cardIcon
    }
  },
  computed: {
    canLikeScrap() {
      // 좋아요/스크랩 가능 조건: 로그인 상태이고, 본인 문제가 아닐 때
      return this.isAuthenticated && (this.localProblem.authorId != this.currentUserId) && this.currentUserId !== null
    }
  },
  watch: {
    problem: {
      handler(newVal) {
        // prop으로 전달된 problem 객체가 변경될 때 localProblem 업데이트
        this.localProblem = {
          id: newVal.id || null,
          title: newVal.title || '제목 없음',
          authorNickname: newVal.authorNickname || '알 수 없음',
          authorId: newVal.authorId || null,
          studyStatus: newVal.studyStatus || 'new',
          categories: newVal.categories || [],
          isLiked: !!newVal.isLiked, // boolean으로 강제 형변환
          isScrapped: !!newVal.isScrapped, // boolean으로 강제 형변환
          totalLikes: newVal.totalLikes || 0,
          totalScraps: newVal.totalScraps || 0,
          perfectCount: newVal.perfectCount || 0,
          vagueCount: newVal.vagueCount || 0,
          forgottenCount: newVal.forgottenCount || 0,
          cardCount: newVal.cardCount || 0,
        };
        // console.log('ProblemSummary - localProblem updated:', this.localProblem);
      },
      deep: true,
      immediate: true // 컴포넌트 초기화 시에도 한 번 실행
    },
    // currentUserId 또는 isAuthenticated 변경 시 canLikeScrap 재평가되므로 별도 watch 필요 없음.
    // 단, 좋아요/스크랩 비활성화 상태 이미지 처리 (회색 하트/스크랩 아이콘)는 canLikeScrap 값에 따라 잘 작동하도록 로직을 강화했습니다.
    isAuthenticated(newVal) {
      if (!newVal) {
        // 로그아웃 시 localProblem의 사용자 관련 데이터 초기화 (옵션)
        this.localProblem.isLiked = false;
        this.localProblem.isScrapped = false;
        // totalLikes, totalScraps는 전체 통계이므로 유지
      }
    }
  },
  methods: {
    displayTagName(tagName) {
      // Remove the '#' prefix if it exists
      return tagName.startsWith('#') ? tagName.substring(1) : tagName;
    },
    // 학습 상태 텍스트 반환 메서드 (ProblemListItem에서 가져옴)
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
    // 학습 상태 클래스 반환 메서드 (ProblemListItem에서 가져옴)
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
    // getColor 메서드는 태그를 `<span>`으로 직접 렌더링하므로 필요 없음 (ProblemTag 컴포넌트를 사용하지 않는 경우)
    // getColor(tag) {
    //   return tag.color_code || '#ccc';
    // },
    async toggleLike() {
      if (!this.isAuthenticated) {
        this.$emit('auth-required');
        return;
      }
      if (this.localProblem.authorId == this.currentUserId) {
        alert("본인이 작성한 문제는 좋아요할 수 없습니다.");
        return;
      }
      if (!this.canLikeScrap) return; // 최종적으로 다시 한번 권한 확인

      const newLikedStatus = !this.localProblem.isLiked; // 'liked' -> 'isLiked'
      // Optimistic UI update
      this.localProblem.isLiked = newLikedStatus;
      this.localProblem.totalLikes += newLikedStatus ? 1 : -1; // 'likes' -> 'totalLikes'

      try {
        await axios.post(`/api/like/${this.localProblem.id}`, {
          userId: this.currentUserId // userId를 본문의 currentUserId로 보냄
        });
        this.$emit('update-like', { id: this.localProblem.id, isLiked: newLikedStatus, totalLikes: this.localProblem.totalLikes });
      } catch (error) {
        console.error('좋아요 반영 실패:', error);
        // Revert UI if API call fails
        this.localProblem.isLiked = !newLikedStatus;
        this.localProblem.totalLikes += newLikedStatus ? -1 : 1;
        alert('좋아요 처리 중 오류가 발생했습니다.');
      }
    },
    async toggleScrap() {
      if (!this.isAuthenticated) {
        this.$emit('auth-required');
        return;
      }
      if (this.localProblem.authorId == this.currentUserId) {
        alert("본인이 작성한 문제는 스크랩할 수 없습니다.");
        return;
      }
      if (!this.canLikeScrap) return; // 최종적으로 다시 한번 권한 확인

      const newScrappedStatus = !this.localProblem.isScrapped; // 'scrapped' -> 'isScrapped'
      // Optimistic UI update
      this.localProblem.isScrapped = newScrappedStatus;
      this.localProblem.totalScraps += newScrappedStatus ? 1 : -1; // 'scraps' -> 'totalScraps'

      try {
        await axios.post(`/api/scrap/${this.localProblem.id}`, {
          userId: this.currentUserId // userId를 본문의 currentUserId로 보냄
        });
        this.$emit('update-scrap', { id: this.localProblem.id, isScrapped: newScrappedStatus, totalScraps: this.localProblem.totalScraps });
      } catch (error) {
        console.error('스크랩 반영 실패:', error);
        // Revert UI if API call fails
        this.localProblem.isScrapped = !newScrappedStatus;
        this.localProblem.totalScraps += newScrappedStatus ? -1 : 1;
        alert('스크랩 처리 중 오류가 발생했습니다.');
      }
    },
    handleEditClick() {
      // 본인 문제만 수정 가능하도록 이미 v-if 조건이 있으므로 추가 검사는 불필요
      // 다만, isAuthenticated가 false인 경우 라우터 푸시 전에 auth-required 이벤트를 발생시키도록 합니다.
      if (!this.isAuthenticated) {
        this.$emit('auth-required');
        return;
      }
      this.$router.push(`/edit/${this.localProblem.id}`)
    },
    handleSolveClick() {
      if (this.isAuthenticated) {
        this.$emit('solve', this.localProblem.id); // 문제 ID만 전달하도록 수정. 필요에 따라 객체 전체를 전달해도 됨.
      } else {
        this.$emit('auth-required')
      }
    }
  }
}
</script>

<style scoped>
/* 기존 스타일 그대로 유지하며, .meta-right-group의 위치만 조정 */
.problem-summary {
  padding: 16px;
  border: 1px solid #e0d0ff; /* ProblemListItem과 유사한 색상으로 통일 */
  border-radius: 8px;
  background: #ffffff; /* ProblemListItem과 유사한 색상으로 통일 */
  width: 100%;
  box-sizing: border-box;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); /* ProblemListItem과 유사한 그림자 */
}
.problem-summary h3 {
  margin: 0;
  font-size: 18px;
  color: #5a2e87; /* ProblemListItem과 유사한 색상으로 통일 */
}
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px; /* ProblemListItem과 통일 */
  flex-wrap: wrap;
  gap: 8px;
}
.title-left {
  display: flex;
  align-items: baseline; /* ProblemListItem과 통일 */
  gap: 12px;
}
.problem-title {
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  max-width: 100%; /* ProblemListItem과 통일 */
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
}
.category-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px; /* margin: 8px 0; -> margin-bottom: 8px; ProblemListItem과 통일 */
  flex-wrap: wrap; /* ProblemListItem과 통일 */
  gap: 8px; /* ProblemListItem과 통일 */
}
.category-tags {
  display: flex;
  flex-wrap: wrap; /* ProblemListItem과 통일 */
  gap: 8px;
}
.tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 14px;
  font-size: 13px;
  color: white;
  white-space: nowrap; /* 태그가 줄바꿈되지 않도록 */
}
.meta {
  margin-top: 10px; /* ProblemListItem과 통일 */
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
  flex-wrap: wrap; /* ProblemListItem과 통일 */
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
/* ProblemListItem의 hover 효과를 가져옴 */
.icon-wrapper:hover:not(.disabled) {
  transform: translateY(-2px);
}
.icon-wrapper.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.icon {
  width: 20px;
  height: 20px;
  /* cursor: pointer; 삭제 (icon-wrapper에 있음) */
  /* transition: transform 0.2s ease; 삭제 (icon-wrapper에 있음) */
}
/* icon:hover 효과도 icon-wrapper에 통합 */
/* .icon:hover {
  transform: scale(1.1);
} */
.card-icon {
  width: 18px;
  height: 18px;
}
.btn-wrapper {
  display: flex;
  gap: 6px;
  flex-shrink: 0; /* ProblemListItem과 통일 */
}
.edit-btn, .solve-btn {
  padding: 6px 12px; /* ProblemListItem과 통일 */
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  white-space: nowrap;
  font-size: 14px;
  transition: background-color 0.2s ease, transform 0.1s ease, opacity 0.2s ease; /* ProblemListItem과 통일 */
}
.edit-btn {
  background-color: #fff4c1;
  color: #343a40; /* ProblemListItem과 통일 */
}
/* ProblemListItem의 hover, disabled 효과를 가져옴 */
.edit-btn:hover:not(:disabled) {
  background-color: #ffe066;
  transform: translateY(-1px);
}
.edit-btn:disabled, .solve-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: #e0e0e0;
  color: #a0a0a0;
}
.solve-btn {
  background-color: #a471ff;
  color: white;
}
/* ProblemListItem의 hover 효과를 가져옴 */
.solve-btn:hover:not(:disabled) {
  background-color: #854fe6;
  transform: translateY(-1px);
}

/* ProblemListItem에서 가져온 study-status 관련 스타일 */
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

/* ProblemListItem에서 가져온 study-card-summary 관련 스타일 */
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

.meta-card-count {
    font-size: 13px;
    color: #666;
    display: flex;
    align-items: center;
    gap: 4px;
    white-space: nowrap;
    flex-shrink: 0;
}

/* ProblemListItem에서 가져온 meta-right-group 관련 스타일 및 위치 조정 */
.meta-right-group {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-left: auto; /* 오른쪽에 붙도록 */
    flex-wrap: wrap;
}

/* 반응형 디자인 (ProblemListItem과 유사하게 추가) */
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
  .meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .meta-right-group {
    margin-left: 0;
    width: 100%;
    justify-content: flex-end;
  }
  .study-card-summary {
    margin-left: 0;
  }
}
</style>