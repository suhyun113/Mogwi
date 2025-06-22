<template>
  <section class="mypage-section my-problems-section">
    <div class="section-header">
      <div></div>
      <div class="delete-bar">
        <template v-if="isSelectionMode">
          <button class="delete-btn gray" :class="{ active: selectedProblems.length > 0 }" :disabled="selectedProblems.length === 0 || deleting" @click="onSelectDelete">
            <img :src="deleteIcon" alt="삭제" class="delete-icon" /> 선택 삭제 ({{ selectedProblems.length }})
          </button>
          <button class="delete-btn dark-gray" :disabled="deleting" @click="onCancelSelection" style="margin-left: 8px;">
            <img :src="deleteIcon" alt="취소" class="delete-icon" /> 취소
          </button>
        </template>
        <template v-else>
          <button class="delete-btn red" @click="onDeleteClick">
            <img :src="deleteIcon" alt="삭제" class="delete-icon" /> 삭제
          </button>
        </template>
      </div>
    </div>
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
          :isSelectionMode="isSelectionMode"
          :isSelected="selectedProblems.includes(problem.id)"
          @select-problem="onSelectProblem"
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
          :isSelectionMode="isSelectionMode"
          :isSelected="selectedProblems.includes(problem.id)"
          @select-problem="onSelectProblem"
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
          :isSelectionMode="isSelectionMode"
          :isSelected="selectedProblems.includes(problem.id)"
          @select-problem="onSelectProblem"
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
          :isSelectionMode="isSelectionMode"
          :isSelected="selectedProblems.includes(problem.id)"
          @select-problem="onSelectProblem"
        />
      </div>
    </div>
  </section>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import ProblemItem from './ProblemItem.vue';
import deleteIcon from '@/assets/icons/delete.png';
import axios from 'axios';

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
  emits: ['toggle-like', 'go-to-problem', 'edit-problem', 'delete-problem'],
  setup(props, { emit }) {
    const activeTab = ref('public'); // 'public' or 'private'
    const isWide = ref(window.innerWidth >= 900);
    const selectedProblems = ref([]); // 선택된 문제 id 배열
    const isSelectionMode = ref(false);
    const deleting = ref(false);

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

    // 삭제 버튼 클릭 시
    const onDeleteClick = () => {
      alert("삭제할 문제를 클릭하여 선택한 후, '선택 삭제' 버튼을 눌러주세요.");
      isSelectionMode.value = true;
      selectedProblems.value = [];
    };

    // 문제 클릭 시 선택/해제
    const onSelectProblem = (problemId) => {
      if (!isSelectionMode.value) return;
      const idx = selectedProblems.value.indexOf(problemId);
      if (idx === -1) {
        selectedProblems.value.push(problemId);
      } else {
        selectedProblems.value.splice(idx, 1);
      }
    };

    // 선택 삭제 버튼 클릭 시
    const onSelectDelete = async () => {
      if (selectedProblems.value.length === 0) return;
      if (!confirm(`선택된 문제 ${selectedProblems.value.length}개의 문제를 정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`)) return;
      deleting.value = true;
      try {
        for (const id of selectedProblems.value) {
          await axios.delete(`/api/problem/${id}`);
        }
        // 삭제 후 선택 해제 및 모드 종료
        selectedProblems.value = [];
        isSelectionMode.value = false;
        // emit으로 부모에게 삭제 알림(데이터 새로고침)
        emit('delete-problem');
      } catch (e) {
        alert('문제 삭제 중 오류가 발생했습니다.');
      } finally {
        deleting.value = false;
      }
    };

    // 취소 버튼 클릭 시
    const onCancelSelection = () => {
      isSelectionMode.value = false;
      selectedProblems.value = [];
    };

    return {
      activeTab,
      publicProblems,
      privateProblems,
      onToggleLike,
      isWide,
      isSelectionMode,
      selectedProblems,
      onDeleteClick,
      onSelectProblem,
      onSelectDelete,
      onCancelSelection,
      deleteIcon,
      deleting
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

.section-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 8px;
}
.delete-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin: 0;
  z-index: 10;
}
.delete-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  background: none;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
  transition: color 0.2s;
}
.delete-btn.red {
  color: #e2586a;
}
.delete-btn.gray {
  color: #888;
}
.delete-btn.gray:disabled {
  color: #d1d1d1 !important;
}
.delete-btn.dark-gray {
  color: #444;
}
.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.delete-icon {
  width: 24px;
  height: 24px;
  filter: none;
}
.delete-btn.gray .delete-icon {
  filter: grayscale(1) brightness(1.5);
}
.delete-btn.gray:disabled .delete-icon {
  filter: grayscale(1) brightness(2.2);
}
.delete-btn.gray.active {
  color: #e2586a !important;
}
.delete-btn.gray.active .delete-icon {
  filter: invert(36%) sepia(77%) saturate(749%) hue-rotate(314deg) brightness(97%) contrast(92%);
}
.delete-btn.dark-gray .delete-icon {
  filter: grayscale(1) brightness(0.5) contrast(1.2);
}
</style>