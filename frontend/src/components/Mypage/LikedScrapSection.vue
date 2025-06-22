<template>
  <section class="mypage-section liked-scrap-section">
    <!-- 토글 버튼: 좁은 화면에서만 보임 -->
    <div class="tabs" v-if="!isWide">
      <button
        :class="{ 'tab-button': true, active: activeTab === 'liked' }"
        @click="activeTab = 'liked'"
      >
        <i class="fas fa-heart"></i> 좋아요한 문제
      </button>
      <button
        :class="{ 'tab-button': true, active: activeTab === 'scraped' }"
        @click="activeTab = 'scraped'"
      >
        <i class="fas fa-bookmark"></i> 스크랩한 문제
      </button>
    </div>

    <!-- 넓은 화면: 2단 컬럼 -->
    <div v-if="isWide" class="dual-column-layout">
      <div class="problem-list liked-list">
        <div class="problem-list-title liked-title">좋아요한 문제</div>
        <p v-if="likedProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 좋아요한 문제가 없습니다.
        </p>
        <ProblemItem
          v-for="problem in likedProblems"
          :key="problem.id"
          :problem="problem"
          :isLiked="true"
          :isScrapped="problem.scrapped"
          :showPublicTag="false"
          @update-like="onUpdateLike"
          @update-scrap="onUpdateScrap"
          @go-to-problem="handleGoToProblem"
          :isAuthenticated="isAuthenticated"
          :currentUserId="currentUserId"
        />
      </div>
      <div class="problem-list scraped-list">
        <div class="problem-list-title scraped-title">스크랩한 문제</div>
        <p v-if="scrapedProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 스크랩한 문제가 없습니다.
        </p>
        <ProblemItem
          v-for="problem in scrapedProblems"
          :key="problem.id"
          :problem="problem"
          :isLiked="problem.liked"
          :isScrapped="true"
          :showPublicTag="false"
          @update-like="onUpdateLike"
          @update-scrap="onUpdateScrap"
          @go-to-problem="handleGoToProblem"
          :isAuthenticated="isAuthenticated"
          :currentUserId="currentUserId"
        />
      </div>
    </div>

    <!-- 좁은 화면: 기존 토글 방식 -->
    <div v-else class="tab-content">
      <div v-if="activeTab === 'liked'" class="problem-list">
        <p v-if="likedProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 좋아요한 문제가 없습니다.
        </p>
        <ProblemItem
          v-for="problem in likedProblems"
          :key="problem.id"
          :problem="problem"
          :isLiked="true"
          :isScrapped="problem.scrapped"
          :showPublicTag="false"
          @update-like="onUpdateLike"
          @update-scrap="onUpdateScrap"
          @go-to-problem="handleGoToProblem"
          :isAuthenticated="isAuthenticated"
          :currentUserId="currentUserId"
        />
      </div>
      <div v-if="activeTab === 'scraped'" class="problem-list">
        <p v-if="scrapedProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 스크랩한 문제가 없습니다.
        </p>
        <ProblemItem
          v-for="problem in scrapedProblems"
          :key="problem.id"
          :problem="problem"
          :isLiked="problem.liked"
          :isScrapped="true"
          :showPublicTag="false"
          @update-like="onUpdateLike"
          @update-scrap="onUpdateScrap"
          @go-to-problem="handleGoToProblem"
          :isAuthenticated="isAuthenticated"
          :currentUserId="currentUserId"
        />
      </div>
    </div>
  </section>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue';
import ProblemItem from './ProblemItem.vue';

export default {
  name: 'LikedScrapSection',
  components: {
    ProblemItem,
  },
  props: {
    likedProblems: {
      type: Array,
      default: () => [],
    },
    scrapedProblems: {
      type: Array,
      default: () => [],
    },
    isAuthenticated: Boolean,
    currentUserId: [String, Number],
  },
  emits: ['update-like', 'update-scrap', 'go-to-problem'],
  setup(props, { emit }) {
    const activeTab = ref('liked');
    const isWide = ref(window.innerWidth >= 900);
    const handleResize = () => {
      isWide.value = window.innerWidth >= 900;
    };
    onMounted(() => {
      window.addEventListener('resize', handleResize);
    });
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize);
    });

    const onUpdateLike = (problem) => {
      emit('update-like', problem);
    };

    const onUpdateScrap = (problem) => {
      emit('update-scrap', problem);
    };

    const handleGoToProblem = (problemId) => {
      emit('go-to-problem', problemId);
    };

    return {
      activeTab,
      isWide,
      onUpdateLike,
      onUpdateScrap,
      handleGoToProblem,
    };
  },
};
</script>

<style scoped>
.liked-scrap-section {
  background-color: transparent;
  border: none;
  border-radius: 0;
  padding: 0;
  box-shadow: none;
  margin-left: 40px;
}

.dual-column-layout {
  display: flex;
  gap: 32px;
}

.liked-list, .scraped-list {
  flex: 1;
  min-width: 0;
}

.problem-list-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #5a2e87;
  padding: 10px 0 14px 0;
  text-align: center;
  border-bottom: 1.5px solid #ede3ff;
  margin-bottom: 18px;
  letter-spacing: 0.01em;
}

.tabs {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
  gap: 24px;
  flex-direction: row;
  flex-wrap: nowrap;
  width: 100%;
  overflow-x: auto;
  padding: 10px 0 18px 0;
}

.tab-button {
  min-width: 110px;
  max-width: 180px;
  align-items: center;
  padding: 8px 16px;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 5px;
  border: none;
  background: #ede3ff;
  color: #5a2e87;
  margin: 0 2px;
  transition: background 0.18s, color 0.18s;
  white-space: nowrap;
  flex-shrink: 0;
  cursor: pointer;
  letter-spacing: 0.01em;
}

.tab-button.active {
  background:#5a2e87;
  color: #fff;
}

.tab-button:hover:not(.active) {
  background: #d6c2f7;
  color: #5a2e87;
}

.problem-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.no-problems-message {
  text-align: center;
  color: #888;
  font-size: 1.1rem;
  padding: 30px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.no-problems-message .fas {
  font-size: 2rem;
  color: #bbb;
}

@media (max-width: 900px) {
  .dual-column-layout {
    display: none;
  }
  .tabs, .tab-content {
    display: block;
  }
}
@media (min-width: 900px) {
  .tabs, .tab-content {
    display: none !important;
  }
  .dual-column-layout {
    display: flex;
  }
}

@media (max-width: 768px) {
  .liked-scrap-section {
    padding: 20px;
  }
  .tabs {
    flex-wrap: nowrap;
    flex-direction: row;
    gap: 16px;
    justify-content: center;
    width: 100%;
    overflow-x: auto;
    align-items: center;
  }
  .tab-button {
    min-width: 100px;
    max-width: 150px;
    padding: 8px 15px;
    font-size: 0.9rem;
    border-radius: 5px;
  }
}
</style>