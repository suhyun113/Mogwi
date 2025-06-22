<template>
  <section class="mypage-section my-problems-section">
    <!-- 토글 버튼: 좁은 화면에서만 보임 -->
    <div class="tabs" v-if="!isWide">
      <button
        :class="{ 'tab-button': true, active: activeTab === 'public' }"
        @click="activeTab = 'public'"
      >
        <i class="fas fa-globe"></i> 공개 문제
      </button>
      <button
        :class="{ 'tab-button': true, active: activeTab === 'private' }"
        @click="activeTab = 'private'"
      >
        <i class="fas fa-lock"></i> 비공개 문제
      </button>
    </div>

    <!-- 넓은 화면: 2단 컬럼 -->
    <div v-if="isWide" class="dual-column-layout">
      <div class="problem-list public-list">
        <div class="problem-list-title"><h3>공개 문제</h3></div>
        <p v-if="publicProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 공개한 문제가 없습니다.
        </p>
        <ProblemItem
          v-for="problem in publicProblems"
          :key="problem.id"
          :problem="problem"
          :is-authenticated="isAuthenticated"
          :current-user-id="currentUserId"
          :isLiked="problem.isLiked"
          :showPublicTag="true"
          :showCounts="true"
          :likeCount="problem.likeCount"
          :scrapCount="problem.scrapCount"
          @toggle-like="onToggleLike"
        />
      </div>
      <div class="problem-list private-list">
        <div class="problem-list-title"><h3>비공개 문제</h3></div>
        <p v-if="privateProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 비공개 문제가 없습니다.
        </p>
        <ProblemItem
          v-for="problem in privateProblems"
          :key="problem.id"
          :problem="problem"
          :is-authenticated="isAuthenticated"
          :current-user-id="currentUserId"
          :isLiked="problem.isLiked"
          :showPublicTag="true"
          :showCounts="true"
          :likeCount="problem.likeCount"
          :scrapCount="problem.scrapCount"
          @toggle-like="onToggleLike"
        />
      </div>
    </div>

    <!-- 좁은 화면: 기존 토글 방식 -->
    <div v-else class="tab-content">
      <div v-if="activeTab === 'public'" class="problem-list">
        <p v-if="publicProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 공개한 문제가 없습니다.
        </p>
        <ProblemItem
          v-for="problem in publicProblems"
          :key="problem.id"
          :problem="problem"
          :is-authenticated="isAuthenticated"
          :current-user-id="currentUserId"
          :isLiked="problem.isLiked"
          :showPublicTag="true"
          :showCounts="true"
          :likeCount="problem.likeCount"
          :scrapCount="problem.scrapCount"
          @toggle-like="onToggleLike"
        />
      </div>
      <div v-if="activeTab === 'private'" class="problem-list">
        <p v-if="privateProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 비공개 문제가 없습니다.
        </p>
        <ProblemItem
          v-for="problem in privateProblems"
          :key="problem.id"
          :problem="problem"
          :is-authenticated="isAuthenticated"
          :current-user-id="currentUserId"
          :isLiked="problem.isLiked"
          :showPublicTag="true"
          :showCounts="true"
          :likeCount="problem.likeCount"
          :scrapCount="problem.scrapCount"
          @toggle-like="onToggleLike"
        />
      </div>
    </div>
  </section>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import ProblemItem from './ProblemItem.vue';

export default {
  name: 'MyProblemSection',
  components: {
    ProblemItem,
  },
  props: {
    myProblems: {
      type: Array,
      default: () => [],
    },
    isAuthenticated: {
      type: Boolean,
      default: false,
    },
    currentUserId: {
      type: [String, Number],
      default: null,
    },
  },
  emits: ['toggle-like'],
  setup(props, { emit }) {
    const activeTab = ref('public'); // 'public' or 'private'
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

    const publicProblems = computed(() =>
      props.myProblems.filter(problem => problem.isPublic)
    );

    const privateProblems = computed(() =>
      props.myProblems.filter(problem => !problem.isPublic)
    );

    const onToggleLike = (problem) => {
      emit('toggle-like', problem);
    };

    return {
      activeTab,
      publicProblems,
      privateProblems,
      onToggleLike,
      isWide,
    };
  },
};
</script>

<style scoped>
.my-problems-section {
  background-color: transparent;
  border: none;
  border-radius: 0;
  padding: 0;
  box-shadow: none;
  margin-left: 40px;
}

.tabs {
  display: flex !important;
  flex-direction: row !important;
  flex-wrap: nowrap !important;
  justify-content: center;
  align-items: center;
  width: 100%;
  gap: 16px;
  overflow-x: auto;
  padding: 10px 0 18px 0;
  margin-bottom: 20px;
}

.tab-button {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  min-width: 110px;
  max-width: 180px;
  padding: 10px 18px;
  font-size: 0.9rem;
  font-weight: 600;
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
  background: #5a2e87;
  color: #fff;
}

.tab-button:hover:not(.active) {
  background: #d6c2f7;
  color: #5a2e87;
}

.dual-column-layout {
  display: flex;
  gap: 32px;
}
.public-list, .private-list {
  flex: 1;
  min-width: 0;
}

@media (max-width: 900px) {
  .dual-column-layout {
    display: none;
  }
  .tabs, .tab-content {
    display: block;
  }
  .my-problems-section {
    margin-left: 128px !important;
    padding-left: 0 !important;
    padding-right: 0 !important;
    width: calc(100% - 128px);
    display: flex;
    flex-direction: column;
    align-items: flex-start;
  }
  .tabs {
    justify-content: center !important;
    width: auto;
    margin-left: auto;
    margin-right: auto;
    display: flex;
  }
  .tab-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
  }
  .tab-button {
    min-width: 110px;
    max-width: 180px;
    padding: 8px 16px;
    font-size: 0.9rem;
    border-radius: 5px;
  }
  .problem-list-title {
    display: none;
  }
}
@media (min-width: 900px) {
  .tabs, .tab-content {
    display: none !important;
  }
  .dual-column-layout {
    display: flex;
  }
  .my-problems-section {
    margin-left: 40px;
    width: calc(100% - 40px);
    align-items: flex-start;
  }
}

.problem-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  color: #5a2e87;
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

@media (max-width: 768px) {
  .my-problems-section {
    padding: 20px;
  }
  .tabs {
    flex-wrap: nowrap;
    flex-direction: row;
    gap: 8px;
    justify-content: center;
    width: 100%;
    overflow-x: auto;
    align-items: center;
  }
  .tab-button {
    min-width: 80px;
    max-width: 140px;
    width: auto;
    white-space: nowrap;
    justify-content: center;
    flex-shrink: 0;
  }
}

.problem-list-title {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px 0 14px 0;
  border-bottom: 1.5px solid #ede3ff;
  margin-bottom: 18px;
}
.problem-list-title h3 {
  font-size: 1.15rem;
  font-weight: 700;
  color: #5a2e87;
  margin: 0;
  letter-spacing: 0.01em;
}
</style>