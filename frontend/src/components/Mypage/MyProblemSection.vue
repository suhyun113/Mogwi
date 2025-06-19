<template>
  <section class="mypage-section my-problems-section">
    <h2 class="section-title">내가 만든 문제</h2>
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
          @click="goToProblem(problem.id)"
        >
          <template #actions>
            <button @click.stop="editProblem(problem.id)" class="action-button edit-button">
              <i class="fas fa-edit"></i> 수정
            </button>
            <button @click.stop="deleteProblem(problem.id)" class="action-button delete-button">
              <i class="fas fa-trash-alt"></i> 삭제
            </button>
          </template>
        </ProblemListItem>
      </div>

      <div v-if="activeTab === 'private'" class="problem-list">
        <p v-if="privateProblems.length === 0" class="no-problems-message">
          <i class="fas fa-frown"></i> 아직 비공개 문제가 없습니다.
        </p>
        <ProblemListItem
          v-for="problem in privateProblems"
          :key="problem.id"
          :problem="problem"
          @click="goToProblem(problem.id)"
        >
          <template #actions>
            <button @click.stop="editProblem(problem.id)" class="action-button edit-button">
              <i class="fas fa-edit"></i> 수정
            </button>
            <button @click.stop="deleteProblem(problem.id)" class="action-button delete-button">
              <i class="fas fa-trash-alt"></i> 삭제
            </button>
          </template>
        </ProblemListItem>
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
  emits: ['go-to-problem', 'edit-problem', 'delete-problem'],
  setup(props, { emit }) {
    const activeTab = ref('public'); // 'public' or 'private'

    const publicProblems = computed(() =>
      props.myProblems.filter(problem => problem.is_public)
    );

    const privateProblems = computed(() =>
      props.myProblems.filter(problem => !problem.is_public)
    );

    const goToProblem = (problemId) => {
      emit('go-to-problem', problemId);
    };

    const editProblem = (problemId) => {
      emit('edit-problem', problemId);
    };

    const deleteProblem = (problemId) => {
      emit('delete-problem', problemId);
    };

    return {
      activeTab,
      publicProblems,
      privateProblems,
      goToProblem,
      editProblem,
      deleteProblem,
    };
  },
};
</script>

<style scoped>
.my-problems-section {
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

.action-button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
  padding: 5px 10px;
  border-radius: 5px;
  transition: background-color 0.2s ease, color 0.2s ease;
  display: flex;
  align-items: center;
  gap: 5px;
}

.edit-button {
  color: #2196F3; /* Blue */
}

.edit-button:hover {
  background-color: #e3f2fd;
}

.delete-button {
  color: #F44336; /* Red */
}

.delete-button:hover {
  background-color: #ffebee;
}

@media (max-width: 768px) {
  .my-problems-section {
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
  .action-button {
    font-size: 0.8rem;
    padding: 3px 8px;
  }
}
</style>