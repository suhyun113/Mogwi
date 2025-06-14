<template>
  <div class="solve-view">
    <div v-if="loading" class="loading">문제 카드들을 불러오는 중입니다...</div>
    <div v-else-if="currentProblemCard">
      <h1 v-if="problemTitle" class="problem-title">{{ problemTitle }}</h1>

      <div class="solve-section-container">
        <button
          v-if="currentCardIndex > 0 && !disablePrevButtonAfterFirstNext"
          @click="prevCard"
          class="nav-arrow-button left-arrow"
        >
          &#9664;
        </button>

        <CardCountDisplay
          :current-card-index="currentCardIndex"
          :total-cards="shuffledProblemCards.length"
        />

        <div class="problem-card-wrapper">
          <ProblemSolveCard
            :problem="currentProblemCard"
            :has-submitted="hasSubmitted"
            :is-correct-answer="isCorrectAnswer"
            :show-answer="showAnswer"
            @toggle-show-answer="toggleShowAnswer"
            @status-changed="updateCardStatus"
          />
        </div>

        <AnswerInputSection
          v-model="userAnswer"
          :is-disabled="hasSubmitted"
          @submit-answer="submitAnswer"
        />

        <div class="navigation-buttons">
          <button
            v-if="hasSubmitted && currentCardIndex === shuffledProblemCards.length - 1"
            @click="finishStudy"
            class="nav-button finish-button"
          >학습 완료</button>
        </div>

        <button
          :disabled="!hasSubmitted"
          v-if="currentCardIndex < shuffledProblemCards.length - 1"
          @click="nextCard"
          class="nav-arrow-button right-arrow"
        >
          &#9654;
        </button>
      </div>
    </div>
    <div v-else class="no-problems">카드를 찾을 수 없습니다.</div>

    <StudyResultModal
      :is-visible="showResultModal"
      :perfect-count="perfectCount"
      :vague-count="vagueCount"
      :forgotten-count="forgottenCount"
      :total-cards="allProblemCards.length"
      :problem-id="parseInt($route.params.id)" @close="closeResultModal"
    />
  </div>
</template>

<script>
import axios from 'axios';
import { ref, computed, watch } from 'vue'; // Import 'watch'
import { useStore } from 'vuex';
import { useRouter, useRoute } from 'vue-router'; // Import 'useRoute'
import ProblemSolveCard from '@/components/Solve/ProblemSolveCard.vue';
import AnswerInputSection from '@/components/Solve/AnswerInputSection.vue';
import CardCountDisplay from '@/components/Solve/CardCountDisplay.vue';
import StudyResultModal from '@/components/Study/StudyResultModal.vue';

export default {
  components: {
    ProblemSolveCard,
    AnswerInputSection,
    CardCountDisplay,
    StudyResultModal
  },
  setup() {
    const store = useStore();
    const router = useRouter();
    const route = useRoute(); // Initialize useRoute

    const loading = ref(true);
    const allProblemCards = ref([]);
    const shuffledProblemCards = ref([]);
    const currentCardIndex = ref(0);
    const userAnswer = ref('');
    const hasSubmitted = ref(false);
    const isCorrectAnswer = ref(false);
    const showAnswer = ref(false);
    const problemTitle = ref('');

    const showResultModal = ref(false);
    const perfectCount = ref(0);
    const vagueCount = ref(0);
    const forgottenCount = ref(0);
    const disablePrevButtonAfterFirstNext = ref(false);

    const currentUserId = computed(() => store.state.store_userid);
    const currentProblemCard = computed(() => shuffledProblemCards.value[currentCardIndex.value]);

    const initializeStudy = async () => {
      loading.value = true;
      console.log('Initializing study session...');

      currentCardIndex.value = 0;
      userAnswer.value = '';
      hasSubmitted.value = false;
      isCorrectAnswer.value = false;
      showAnswer.value = false;
      perfectCount.value = 0;
      vagueCount.value = 0;
      forgottenCount.value = 0;
      allProblemCards.value = [];
      shuffledProblemCards.value = [];
      showResultModal.value = false; // Ensure modal is closed
      disablePrevButtonAfterFirstNext.value = false; // Reset prev button state

      const problemId = route.params.id;
      if (!problemId) {
        console.error("문제 ID를 찾을 수 없습니다.");
        loading.value = false;
        router.push('/'); // Or some error page
        return;
      }

      try {
        console.log(`Fetching cards for problem ID: ${problemId} with user ID: ${currentUserId.value}`);
        const response = await axios.get(`/api/study/${problemId}/solve`, {
          params: { currentUserId: currentUserId.value }
        });
        allProblemCards.value = response.data.map(card => ({
          ...card,
          cardStatus: card.cardStatus || 'new'
        }));
        shuffleCards(); // 3. Shuffle them immediately after fetching

        // Also fetch problem title
        const problemResponse = await axios.get(`/api/problems/${problemId}`, {
          params: { currentUserId: currentUserId.value }
        });
        problemTitle.value = problemResponse.data.title;

      } catch (error) {
        console.error("문제 카드 불러오기 실패:", error);
        alert("문제 카드 불러오기 실패: " + (error.response?.data?.message || error.message));
        router.push(`/card/${problemId}`); // Fallback
      } finally {
        loading.value = false;
      }
    };

    const shuffleCards = () => {
      const array = [...allProblemCards.value];
      for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
      }
      shuffledProblemCards.value = array;
      // After shuffling, ensure we are at the first card
      currentCardIndex.value = 0;
      console.log('Cards shuffled. Starting from index:', currentCardIndex.value);
    };

    const submitAnswer = async () => {
      if (!userAnswer.value.trim()) {
        alert("정답을 입력해주세요!");
        return;
      }

      hasSubmitted.value = true;

      const correct = (currentProblemCard.value.correct || '').trim().toLowerCase();
      const submittedAnswer = (userAnswer.value || '').trim().toLowerCase();

      if (submittedAnswer === correct) {
        isCorrectAnswer.value = true;
        // Only change status to 'perfect' if it's not already perfect or vague
        if (currentProblemCard.value.cardStatus !== 'perfect' && currentProblemCard.value.cardStatus !== 'vague') {
          currentProblemCard.value.cardStatus = 'perfect';
        }
      } else {
        isCorrectAnswer.value = false;
        // Only change status to 'forgotten' if it's not already forgotten
        if (currentProblemCard.value.cardStatus !== 'forgotten') {
          currentProblemCard.value.cardStatus = 'forgotten';
        }
        showAnswer.value = true;
      }
      await saveCardStatus(); // Await saving status before moving on
    };

    const updateCardStatus = async (newStatus) => {
      currentProblemCard.value.cardStatus = newStatus;
      await saveCardStatus();
    };

    const saveCardStatus = async () => {
      const card = currentProblemCard.value;
      const problemId = parseInt(route.params.id); // Use route here
      const cardId = card.id;
      const cardStatus = card.cardStatus;
      const userId = currentUserId.value;

      if (!['perfect', 'forgotten', 'vague'].includes(cardStatus)) return;

      try {
        await axios.post(`/api/solve/${cardId}/status`, {
          userId,
          cardStatus,
          problemId
        });
      } catch (error) {
        console.error("카드 상태 저장 실패:", error);
        alert("카드 상태 저장 중 오류 발생");
      }
    };

    const nextCard = async () => {
      // Ensure current card status is saved before moving to the next
      if (!hasSubmitted.value) {
        // Force submission or alert if user tries to skip without submitting
        alert("먼저 답을 제출해주세요!");
        return;
      }

      if (currentCardIndex.value < shuffledProblemCards.value.length - 1) {
        currentCardIndex.value++;
        resetCardState();
        disablePrevButtonAfterFirstNext.value = true;
      } else {
        finishStudy();
      }
    };

    const prevCard = () => {
      if (currentCardIndex.value > 0) {
        currentCardIndex.value--;
        resetCardState();
      }
    };

    const resetCardState = () => {
      userAnswer.value = '';
      hasSubmitted.value = false;
      isCorrectAnswer.value = false;
      showAnswer.value = false;
    };

    const calculateStudyResults = () => {
      // Iterate through the allProblemCards to get final counts
      // This is important because individual card statuses are updated live
      perfectCount.value = allProblemCards.value.filter(card => card.cardStatus === 'perfect').length;
      vagueCount.value = allProblemCards.value.filter(card => card.cardStatus === 'vague').length;
      forgottenCount.value = allProblemCards.value.filter(card => card.cardStatus === 'forgotten').length;
    };

    const finishStudy = () => {
      calculateStudyResults();
      showResultModal.value = true;
    };

    const closeResultModal = () => {
      showResultModal.value = false;
      // After closing modal, optionally navigate away or do nothing.
      // If you want to go to a default problem list or home:
      // router.push(`/card/${route.params.id}`); // This would just reload the same study view.
      // router.push('/some-other-route'); // e.g., to the problem list
    };

    const toggleShowAnswer = () => {
      showAnswer.value = !showAnswer.value;
    };

    // --- Watchers for route changes ---
    watch(() => route.params.id, async (newId, oldId) => {
      if (newId && newId !== oldId) {
        console.log(`Route param ID changed from ${oldId} to ${newId}. Triggering initializeStudy.`);
        await initializeStudy();
      }
    }, { immediate: true }); // immediate: true to run on initial component mount

    watch(() => route.query.reset, async (newVal, oldVal) => {
      if (newVal && newVal !== oldVal) {
        console.log(`Reset query param detected (${oldVal} -> ${newVal}). Triggering initializeStudy.`);
        await initializeStudy();
      }
    });

    // --- On Mounted (for initial fetch, but now mostly handled by immediate watcher) ---
    // onMounted(async () => {
    //   // This might not be strictly necessary with immediate: true on the watcher
    //   // but can act as a fallback or for other one-time setups.
    //   // await initializeStudy();
    // });

    return {
      loading,
      allProblemCards,
      shuffledProblemCards,
      currentCardIndex,
      userAnswer,
      hasSubmitted,
      isCorrectAnswer,
      showAnswer,
      currentProblemCard,
      currentUserId,
      problemTitle,
      disablePrevButtonAfterFirstNext,
      showResultModal,
      perfectCount,
      vagueCount,
      forgottenCount,
      fetchProblemCards: initializeStudy,
      shuffleCards,
      submitAnswer,
      prevCard,
      nextCard,
      finishStudy,
      closeResultModal,
      toggleShowAnswer,
      updateCardStatus
    };
  }
};
</script>

<style scoped>
/* (스타일은 변경 없음) */
.solve-view {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #fdf8f4;
  overflow: hidden; /* 스크롤바 생성 방지 */
  padding: 20px; /* 전체 뷰에 패딩 유지 */
  box-sizing: border-box; /* 패딩을 포함한 너비/높이 계산 */
}

.loading, .no-problems {
  color: #9ca3af;
  font-size: 1.25rem;
}

/* solve-section-container: 카드, 입력란, 버튼, 화살표를 감싸는 핵심 컨테이너 */
.solve-section-container {
  display: flex;
  flex-direction: column; /* 아이템들을 세로로 정렬 */
  justify-content: center; /* 세로축 가운데 정렬 */
  align-items: center; /* 가로축 가운데 정렬 */
  width: 100%;
  max-width: 450px; /* 전체 컨테이너의 최대 너비 조정 */
  position: relative; /* 내부 요소 (화살표) 절대 위치 기준점 */
  padding: 20px; /* solve-section-container 내부에 패딩 추가 */
  box-sizing: border-box; /* 패딩 포함 너비 계산 */
  border-radius: 12px; /* 부드러운 모서리 */
  /* 배경색과 그림자는 더 이상 solve-section-container에 두지 않습니다. */
}

.problem-card-wrapper { /* ProblemSolveCard를 감싸는 래퍼 */
  width: 100%;
  max-width: 300px; /* 카드 자체의 너비 조절 */
  text-align: center;
  margin-bottom: 0px; /* ProblemSolveCard와 AnswerInputSection 사이 간격 증가 */
  display: flex;
  flex-direction: column;
  align-items: center; /* 내부 ProblemSolveCard를 중앙 정렬 */
}

.nav-arrow-button {
  background-color: #a471ff;
  color: white;
  border: none;
  border-radius: 50%;
  width: 55px;
  height: 55px;
  font-size: 1.6rem;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.2s;
  position: fixed; /* 화면 기준 고정 */
  top: 50%; /* 화면 중앙 수직 정렬 */
  transform: translateY(-50%);
  z-index: 1000; /* 최상단 표시 */
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
}

.nav-arrow-button:hover {
  background-color: #8b5cf6; /* StartButton의 hover 색상 */
  transform: translateY(-50%) scale(1.1); /* 호버 시 더 크게 확대 */
}

.nav-arrow-button:disabled {
  background-color: #d3d3d3;
  color: #888888;
  cursor: not-allowed;
  transform: translateY(-50%) scale(1);
  pointer-events: none;
}

.nav-arrow-button.left-arrow {
  left: 40px; /* 화면 왼쪽 끝에서 떨어진 거리 */
}

.nav-arrow-button.right-arrow {
  right: 40px; /* 화면 오른쪽 끝에서 떨어진 거리 */
}

.navigation-buttons {
  margin-top: 25px; /* AnswerInputSection과 간격 증가 */
  display: flex;
  justify-content: center; /* 학습 완료 버튼을 중앙에 정렬 */
  gap: 15px; /* 버튼 간격 증가 */
  width: 100%;
  max-width: 350px; /* 정답 입력란과 시각적 통일성을 위해 조정 */
}

.nav-button {
  background-color: #a471ff;
  color: white;
  border: none;
  border-radius: 0.75rem; /* 버튼 모서리 둥글게 */
  padding: 0.5rem 1.25rem; /* 패딩 증가 */
  font-size: 0.95rem; /* 폰트 크기 약간 증가 */
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.2s;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.nav-button:hover {
  background-color: #8b5cf6;
  transform: translateY(-2px); /* 호버 시 살짝 올라가는 효과 */
}

.nav-button.finish-button {
  background-color: #7a4cb8; /* 더 진한 보라색 */
}

.nav-button.finish-button:hover {
  background-color: #613e92;
}

.problem-title {
  font-size: 1.6rem;
  font-weight: bold;
  color: #4a3f69;
  margin: 0 auto 30px;    /* 가운데 정렬 + 아래 여백 */
  text-align: center;
  width: 100%;
  max-width: 300px;
  line-height: 1.4;

  background-color: #e6d6ff; /* 아주 연한 보라색 배경 */
  padding: 10px 20px;         /* 살짝 얇게 */
  border-radius: 8px;         /* 카드, 버튼과 통일된 둥글기 */
  border: 1px solid #c9b3ff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08); /* 은은한 그림자 */
  box-sizing: border-box;
}
</style>