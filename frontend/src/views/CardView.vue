<template>
  <div class="solve-view">
    <div v-if="loading" class="loading">문제 카드들을 불러오는 중입니다...</div>
    <div v-else-if="currentProblemCard" class="solve-section-container">
      <!-- 이전 카드 버튼 -->
      <button
        v-if="currentCardIndex > 0"
        @click="prevCard"
        class="nav-arrow-button left-arrow"
      >
        &#9664; <!-- 왼쪽 화살표 문자 -->
      </button>

      <div class="solve-card-container">
        <!-- 문제 카드 컴포넌트 -->
        <ProblemSolveCard
          :problem="currentProblemCard"
          :current-card-index="currentCardIndex"
          :total-cards="shuffledProblemCards.length"
          :has-submitted="hasSubmitted"
          :is-correct-answer="isCorrectAnswer"
          :show-answer="showAnswer"
          @toggle-show-answer="toggleShowAnswer"
        />

        <!-- 정답 입력란 컴포넌트 -->
        <AnswerInputSection
          v-model="userAnswer"
          :is-disabled="hasSubmitted"
          @submit-answer="submitAnswer"
        />

        <!-- 학습 완료 버튼은 여기에 유지하거나, 필요에 따라 위치를 조정할 수 있습니다. -->
        <div class="navigation-buttons">
          <button v-if="hasSubmitted && currentCardIndex === shuffledProblemCards.length - 1" @click="finishStudy" class="nav-button finish-button">학습 완료</button>
        </div>
      </div>

      <!-- 다음 카드 버튼 -->
      <button
        v-if="currentCardIndex < shuffledProblemCards.length - 1"
        @click="nextCard"
        class="nav-arrow-button right-arrow"
      >
        &#9654; <!-- 오른쪽 화살표 문자 -->
      </button>
    </div>
    <div v-else class="no-problems">카드를 찾을 수 없습니다.</div>
  </div>
</template>

<script>
import axios from 'axios';
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import ProblemSolveCard from '@/components/ProblemSolveCard.vue';
import AnswerInputSection from '@/components/AnswerInputSection.vue';

export default {
  components: {
    ProblemSolveCard,
    AnswerInputSection
  },
  setup() {
    const store = useStore();
    const router = useRouter();

    const loading = ref(true);
    const allProblemCards = ref([]);
    const shuffledProblemCards = ref([]);
    const currentCardIndex = ref(0);
    const userAnswer = ref('');
    const hasSubmitted = ref(false);
    const isCorrectAnswer = ref(false);
    const showAnswer = ref(false);

    const currentUserId = computed(() => store.state.store_userid);

    const currentProblemCard = computed(() => {
      return shuffledProblemCards.value[currentCardIndex.value];
    });

    const fetchProblemCards = async () => {
      const problemId = router.currentRoute.value.params.id;
      const requestUrl = `/api/study/${problemId}/cards`;
      console.log('DEBUG: axios.get 요청 URL:', requestUrl);

      try {
        const response = await axios.get(requestUrl, {
          params: {
            currentUserId: currentUserId.value
          }
        });
        allProblemCards.value = response.data;
        shuffleCards();
        loading.value = false;
        console.log('DEBUG: 카드 불러오기 성공:', response.data);
      } catch (error) {
        console.error("문제 카드 불러오기 실패:", error.response ? error.response.data : error.message);
        console.error("에러 상태 코드:", error.response ? error.response.status : 'N/A');
        console.error("요청 URL:", error.config ? error.config.url : 'N/A');
        loading.value = false;
        alert("문제 카드 불러오기 실패했습니다. 콘솔을 확인해주세요.");
        router.push(`/card/${problemId}`);
      }
    };

    const shuffleCards = () => {
      let array = [...allProblemCards.value];
      for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
      }
      shuffledProblemCards.value = array;
      currentCardIndex.value = 0;
    };

    const submitAnswer = async () => {
      if (!userAnswer.value.trim()) {
        alert("정답을 입력해주세요!");
        return;
      }

      hasSubmitted.value = true;
      const problemId = router.currentRoute.value.params.id;
      const cardId = currentProblemCard.value.id;
      const correct = currentProblemCard.value.answer.trim().toLowerCase();
      const submittedAnswer = userAnswer.value.trim().toLowerCase();

      let newCardStatus = 'unsolved';
      if (submittedAnswer === correct) {
        isCorrectAnswer.value = true;
        newCardStatus = 'solved';
      } else {
        isCorrectAnswer.value = false;
        newCardStatus = 'review';
        showAnswer.value = true;
      }

      try {
        await axios.post(`/api/cards/${cardId}/status`, {
          userId: currentUserId.value,
          cardStatus: newCardStatus,
          problemId: problemId
        });
        currentProblemCard.value.cardStatus = newCardStatus;
        console.log(`카드 ${cardId} 상태 업데이트 성공: ${newCardStatus}`);
      } catch (error) {
        console.error(`카드 ${cardId} 상태 업데이트 실패:`, error.response || error);
        alert("카드 상태 업데이트에 실패했습니다. 콘솔을 확인해주세요.");
      }
    };

    const prevCard = () => {
      if (currentCardIndex.value > 0) {
        currentCardIndex.value--;
        resetCardState();
      }
    };

    const nextCard = () => {
      if (currentCardIndex.value < shuffledProblemCards.value.length - 1) {
        currentCardIndex.value++;
        resetCardState();
      } else {
        finishStudy();
      }
    };

    const resetCardState = () => {
      userAnswer.value = '';
      hasSubmitted.value = false;
      isCorrectAnswer.value = false;
      showAnswer.value = false;
    };

    const finishStudy = () => {
      alert("학습을 완료했습니다! 수고하셨습니다.");
      router.push(`/card/${router.currentRoute.value.params.id}`);
    };

    const toggleShowAnswer = () => {
      showAnswer.value = !showAnswer.value;
    };

    onMounted(fetchProblemCards);

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
      fetchProblemCards,
      shuffleCards,
      submitAnswer,
      prevCard,
      nextCard,
      finishStudy,
      toggleShowAnswer
    };
  }
};
</script>

<style scoped>
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
  padding: 20px;
  box-sizing: border-box; /* 패딩을 포함한 너비/높이 계산 */
}

.loading, .no-problems {
  color: #9ca3af;
  font-size: 1.25rem;
}

/* solve-section-container 추가: 카드와 화살표 버튼을 감싸는 컨테이너 */
.solve-section-container {
  display: flex;
  justify-content: center; /* 가운데 정렬 */
  align-items: center; /* 수직 가운데 정렬 */
  width: 100%;
  max-width: 600px; /* 화살표 버튼 포함한 전체 너비 */
  position: relative; /* 내부 요소 (화살표) 절대 위치 기준점 */
}

.solve-card-container {
  width: 100%;
  max-width: 250px; /* 트럼프 카드처럼 좁게 */
  text-align: center;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  /* solve-section-container가 중앙 정렬하므로 margin: auto 제거 */
}

/* 새롭게 추가된 화살표 버튼 스타일 */
.nav-arrow-button {
  background-color: #a471ff; /* StartButton과 동일한 보라색 */
  color: white;
  border: none;
  border-radius: 50%; /* 원형 버튼 */
  width: 50px; /* 버튼 크기 */
  height: 50px; /* 버튼 크기 */
  font-size: 1.5rem; /* 화살표 크기 */
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.2s;
  position: absolute; /* solve-section-container 기준 절대 위치 */
  top: 50%; /* 수직 중앙 */
  transform: translateY(-50%); /* 정확한 수직 중앙 정렬 */
  z-index: 10; /* 카드보다 위에 오도록 */
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); /* 그림자 추가 */
}

.nav-arrow-button.left-arrow {
  left: 10px; /* 왼쪽에서 10px 떨어짐 */
}

.nav-arrow-button.right-arrow {
  right: 10px; /* 오른쪽에서 10px 떨어짐 */
}

.nav-arrow-button:hover {
  background-color: #8b5cf6; /* StartButton의 hover 색상 */
  transform: translateY(-50%) scale(1.05); /* 호버 시 약간 확대 */
}

.navigation-buttons {
  margin-top: 20px;
  display: flex;
  justify-content: center; /* 학습 완료 버튼을 중앙에 정렬 */
  gap: 10px;
  width: 100%;
}

.nav-button {
  background-color: #a471ff;
  color: white;
  border: none;
  border-radius: 0.5rem;
  padding: 0.375rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.nav-button:hover {
  background-color: #8b5cf6;
}

.nav-button.finish-button {
  background-color: #7a4cb8;
}

.nav-button.finish-button:hover {
  background-color: #613e92;
}
</style> 