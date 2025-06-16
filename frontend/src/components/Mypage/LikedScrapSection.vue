<template>
  <section class="mypage-section liked-scrap-section">
    <h2 class="section-title">활동 내역</h2>
    <div class="tabs">
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

    <div class="tab-content">
      <div v-if="activeTab === 'liked'" class="problem-list">
        <p v-if="likedProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 좋아요한 문제가 없습니다.
        </p>
        <ProblemListItem
          v-for="problem in likedProblems"
          :key="problem.id"
          :problem="problem"
          @click="goToProblem(problem.id)"
        />
      </div>

      <div v-if="activeTab === 'scraped'" class="problem-list">
        <p v-if="scrapedProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 스크랩한 문제가 없습니다.
        </p>
        <ProblemListItem
          v-for="problem in scrapedProblems"
          :key="problem.id"
          :problem="problem"
          @click="goToProblem(problem.id)"
        />
      </div>
    </div>
  </section>
</template>

<script>
import { ref } from 'vue';
import ProblemListItem from './ProblemListItem.vue';

export default {
  name: 'LikedScrapSection',
  components: {
    ProblemListItem,
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
  },
  emits: ['go-to-problem'],
  setup(props, { emit }) {
    const activeTab = ref('liked'); // 'liked' or 'scraped'

    const goToProblem = (problemId) => {
      emit('go-to-problem', problemId);
    };

    return {
      activeTab,
      goToProblem,
    };
  },
};
</script>

<style scoped>
.liked-scrap-section {
  background-color: #fcf9fc;
  border: 1px solid #ede1ff;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.section-title {
  color: #5a2e87;
  font-size: 1.8rem;
  font-weight: 600;
  margin-bottom: 20px;
  text-align: center;
  border-bottom: 1px dashed #f0e6ff;
  padding-bottom: 15px;
}

.tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
  gap: 10px;
}

.tab-button {
  background-color: #e9dffc;
  color: #6a3d9a;
  border: 1px solid #dcd0f0;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-button:hover {
  background-color: #dcd0f0;
}

.tab-button.active {
  background-color: #8c5dff;
  color: white;
  border-color: #8c5dff;
  font-weight: 600;
}

.tab-button.active:hover {
  background-color: #7a4bb7;
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

@media (max-width: 768px) {
  .liked-scrap-section {
    padding: 20px;
  }
  .section-title {
    font-size: 1.5rem;
  }
  .tabs {
    flex-wrap: wrap;
    gap: 8px;
  }
  .tab-button {
    padding: 8px 15px;
    font-size: 0.9rem;
    flex-grow: 1;
    justify-content: center;
  }
}
</style>