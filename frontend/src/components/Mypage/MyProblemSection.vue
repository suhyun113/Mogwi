<template>
  <section class="mypage-section my-problems-section">
    <div class="tabs">
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

    <div class="tab-content">
      <div v-if="activeTab === 'public'" class="problem-list">
        <p v-if="publicProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 공개한 문제가 없습니다.
        </p>
        <ProblemListItem
          v-for="problem in publicProblems"
          :key="problem.id"
          :problem="problem"
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
        <ProblemListItem
          v-for="problem in privateProblems"
          :key="problem.id"
          :problem="problem"
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
import { ref, computed } from 'vue';
import ProblemListItem from './ProblemListItem.vue';

export default {
  name: 'MyProblemSection',
  components: {
    ProblemListItem,
  },
  props: {
    myProblems: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['toggle-like'],
  setup(props, { emit }) {
    const activeTab = ref('public'); // 'public' or 'private'

    const publicProblems = computed(() =>
      props.myProblems.filter(problem => problem.is_public)
    );

    const privateProblems = computed(() =>
      props.myProblems.filter(problem => !problem.is_public)
    );

    const onToggleLike = (problem) => {
      emit('toggle-like', problem);
    };

    return {
      activeTab,
      publicProblems,
      privateProblems,
      onToggleLike,
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
  .my-problems-section {
    padding: 20px;
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