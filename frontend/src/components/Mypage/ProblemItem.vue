<template>
  <div :class="[itemClass, { selected: isSelected }]" @click="handleItemClick">
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
        <ProblemTag
          v-for="tag in localProblem.categories"
          :key="tag.id"
          :tagName="formatTagName(tag.tag_name)"
          :backgroundColor="getColor(tag)"
        />
      </div>
      <div class="btn-wrapper">
        <template v-if="isAuthenticated && localProblem.authorId == currentUserId">
          <button
            class="edit-btn"
            @click.stop="handleEditClick"
            :disabled="isSelectionMode"
          >
            수정
          </button>
        </template>
        <button
          class="solve-btn"
          @click.stop="handleSolveClick"
          :disabled="isSelectionMode"
        >
          {{ localProblem.studyStatus === 'completed' ? '다시 풀기' : '문제 풀기' }}
        </button>
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

      <div class="meta-right-group">
        <div v-if="localProblem.studyStatus !== 'new' && localProblem.studyStatus !== ''" class="study-card-summary">
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
import heartOff from '@/assets/icons/like_outline.png'
import heartOn from '@/assets/icons/like_filled.png'
import scrapOff from '@/assets/icons/scrap_outline.png'
import scrapOn from '@/assets/icons/scrap_filled.png'
import cardIcon from '@/assets/icons/card.png'
import ProblemTag from '@/components/MyStudy/ProblemTag.vue'
import axios from 'axios';

export default {
  name: 'ProblemListItem',
  components: { ProblemTag },
  props: {
    problem: Object,
    isAuthenticated: Boolean,
    currentUserId: [String, Number],
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isLiked: Boolean,
    isScrapped: Boolean,
    isEditable: Boolean,
    showPublicTag: Boolean,
  },
  emits: ['auth-required', 'go-to-study', 'toggle-selection', 'update-like', 'update-scrap', 'select-problem', 'delete-problem'],
  data() {
    return {
      localProblem: { ...this.problem },
      heartOff,
      heartOn,
      scrapOff,
      scrapOn,
      cardIcon,
    };
  },
  computed: {
    itemClass() {
      return {
        'problem-item': true,
        'selectable': this.isSelectionMode && this.isAuthenticated,
        'selected': this.isSelected,
      };
    },
    canLikeScrap() {
      return this.isAuthenticated && !this.isSelectionMode;
    }
  },
  watch: {
    problem: {
      handler(newVal) {
        this.localProblem = {
          ...newVal,
          // props로 전달된 isLiked, isScrapped를 우선적으로 사용하고, 없으면 problem 객체의 값을 사용
          liked: this.isLiked !== undefined ? this.isLiked : (newVal.liked ?? false),
          scrapped: this.isScrapped !== undefined ? this.isScrapped : (newVal.scrapped ?? false),
          authorName:
            newVal.authorName ||
            newVal.author_name ||
            newVal.username ||
            '알 수 없음',
          authorId: newVal.authorId?.toString() || newVal.author_id?.toString() || null
        };
        // problem 객체 변경 시 콘솔 로그 출력
        this.logEditButtonConditions();
      },
      immediate: true,
      deep: true,
    },
    // isLiked prop이 변경될 때 localProblem.liked 업데이트
    isLiked(newVal) {
      this.localProblem.liked = newVal;
    },
    // isScrapped prop이 변경될 때 localProblem.scrapped 업데이트
    isScrapped(newVal) {
      this.localProblem.scrapped = newVal;
    },
    // isAuthenticated, currentUserId prop이 변경될 때마다 콘솔 로그 출력
    isAuthenticated() {
      this.logEditButtonConditions();
    },
    currentUserId() {
      this.logEditButtonConditions();
    }
  },
  mounted() {
    // 컴포넌트가 마운트될 때 초기 값들을 콘솔에 출력
    this.logEditButtonConditions();
  },
  methods: {
    // 조건들을 한 번에 출력하는 메서드
    logEditButtonConditions() {
      const isAuth = this.isAuthenticated;
      const authorId = this.localProblem.authorId;
      const currentId = this.currentUserId;
      const conditionMet = isAuth && (authorId == currentId);

      console.log('--- ProblemListItem: Edit Button Conditions ---');
      console.log(`문제 제목: ${this.localProblem.title}`);
      console.log(`isAuthenticated: ${isAuth}`);
      console.log(`Problem Author ID: ${authorId} (Type: ${typeof authorId})`);
      console.log(`Current User ID: ${currentId} (Type: ${typeof currentId})`);
      console.log(`ID 일치 여부 (==): ${authorId == currentId}`); // 동등 비교
      console.log(`ID 일치 여부 (===): ${authorId === currentId}`); // 엄격 비교 (타입까지)
      console.log(`최종 조건 (isAuthenticated && authorId == currentUserId): ${conditionMet}`);
      console.log('--------------------------------------------');
    },
    formatTagName(name) {
      return name.startsWith('#') ? name : `#${name}`;
    },
    getColor(tag) {
      return tag.color_code || '#ccc';
    },
    getStatusText(status) {
      return { new: '진행 전', ongoing: '진행 중', completed: '완료', '': '' }[status] || '';
    },
    getStatusClass(status) {
      return { new: 'status-new', ongoing: 'status-ongoing', completed: 'status-completed', '': 'status-none' }[status] || '';
    },
    handleItemClick() {
      if (this.isSelectionMode) {
        this.$emit('select-problem', this.localProblem.id);
        return;
      }
      if (!this.isAuthenticated) {
        this.$emit('auth-required');
        return;
      }
      if (this.isSelectionMode) {
        this.$emit('toggle-selection', this.localProblem.id);
      }
    },
    async toggleLike() {
      if (!this.canLikeScrap) {
        if (!this.isAuthenticated) {
          this.$emit('auth-required');
        }
        return;
      }

      const originalLiked = this.localProblem.liked;
      const originalLikes = this.localProblem.likes;

      this.localProblem.liked = !originalLiked;
      this.localProblem.likes += this.localProblem.liked ? 1 : -1;

      try {
        await axios.post(`/api/like/${this.localProblem.id}`, {
          userId: this.currentUserId
        });
        // 좋아요/스크랩 수 변화만 MypageView로 전달하여 전체 데이터를 다시 불러오게 함
        this.$emit('update-like', { ...this.localProblem });
      } catch (error) {
        console.error('좋아요 반영 실패:', error);
        this.localProblem.liked = originalLiked;
        this.localProblem.likes = originalLikes;
      }
    },
    async toggleScrap() {
      if (!this.canLikeScrap) {
        if (!this.isAuthenticated) {
          this.$emit('auth-required');
        }
        return;
      }

      const originalScrapped = this.localProblem.scrapped;
      const originalScraps = this.localProblem.scraps;

      this.localProblem.scrapped = !originalScrapped;
      this.localProblem.scraps += this.localProblem.scrapped ? 1 : -1;

      try {
        await axios.post(`/api/scrap/${this.localProblem.id}`, {
          userId: this.currentUserId
        });
        // 좋아요/스크랩 수 변화만 MypageView로 전달하여 전체 데이터를 다시 불러오게 함
        this.$emit('update-scrap', { ...this.localProblem });
      } catch (error) {
        console.error('스크랩 반영 실패:', error);
        this.localProblem.scrapped = originalScrapped;
        this.localProblem.scraps = originalScraps;
      }
    },
    handleEditClick() {
      const problem = this.localProblem;
      this.$router.push({
        path: `/edit/${problem.id}`,
        query: { isPublic: problem.isPublic }
      });
    },
    handleSolveClick() {
      this.$router.push(`/study/${this.localProblem.id}`);
    }
  }
};
</script>

<style scoped>
/* 기존 스타일은 변경 사항 없음 */
.problem-item {
  padding: 16px;
  border: 1px solid #e0d0ff;
  border-radius: 8px;
  background: #ffffff;
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 15px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  position: relative;
}

.problem-item:hover:not(.selectable) {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.problem-item.selectable {
  cursor: pointer;
  border-color: #a471ff;
}

.problem-item.selected {
  border: 2px solid #e2586a !important;
  box-shadow: 0 0 0 3px rgba(226, 88, 106, 0.15);
}

.problem-item h3 {
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
  min-width: 0;
  width: 100%;
}
.problem-title {
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
  min-width: 0;
}
.author {
  font-size: 13px;
  color: #888;
  white-space: nowrap;
  flex-shrink: 0;
  /* margin-left: 12px; // gap으로 대체 가능 */
}

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

.status-none {
  padding: 1px;
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
  flex-wrap: wrap;
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
  transition: background-color 0.2s ease, transform 0.1s ease, opacity 0.2s ease;
}
.edit-btn {
  background-color: #fff4c1;
  color: #343a40;
}
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
.solve-btn:hover:not(:disabled) {
  background-color: #854fe6;
  transform: translateY(-1px);
}

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

.meta-right-group {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  flex-wrap: wrap;
}

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