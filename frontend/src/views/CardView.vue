<template>
  <div class="solve-view">
    <div v-if="loading" class="loading">문제 카드들을 불러오는 중입니다...</div>
    <div v-else-if="currentProblemCard" class="solve-card-container">
      <div class="problem-card" :class="getCardStatusClass(currentProblemCard.cardStatus)">
        <h3 class="card-question">{{ currentProblemCard.question }}</h3>
        <div class="card-count">{{ currentCardIndex + 1 }} / {{ shuffledProblemCards.length }}</div>
        <button v-if="showAnswer && !isCorrectAnswer" @click="toggleShowAnswer" class="show-answer-button">정답 숨기기</button>
        <button v-else-if="!showAnswer && !isCorrectAnswer && hasSubmitted" @click="toggleShowAnswer" class="show-answer-button">정답 보기</button>
        <div v-if="showAnswer && !isCorrectAnswer" class="correct-answer-display">
          정답: {{ currentProblemCard.answer }}
        </div>
      </div>

      <div class="answer-section">
        <input
          type="text"
          v-model="userAnswer"
          placeholder="정답을 입력하세요"
          @keyup.enter="submitAnswer"
          class="answer-input"
          :disabled="hasSubmitted"
        />
        <button @click="submitAnswer" class="submit-button" :disabled="hasSubmitted">정답 제출</button>
      </div>

      <div class="navigation-buttons">
        <button v-if="currentCardIndex > 0" @click="prevCard" class="nav-button">이전 카드</button>
        <button v-if="hasSubmitted && currentCardIndex < shuffledProblemCards.length - 1" @click="nextCard" class="nav-button">다음 카드</button>
        <button v-else-if="hasSubmitted && currentCardIndex === shuffledProblemCards.length - 1" @click="finishStudy" class="nav-button finish-button">학습 완료</button>
      </div>
    </div>
    <div v-else class="no-problems">카드를 찾을 수 없습니다.</div>
  </div>
</template>

<script>
import axios from 'axios';
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';

export default {
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
    const isAuthenticated = computed(() => !!store.state.store_userid);

    const currentProblemCard = computed(() => {
      return shuffledProblemCards.value[currentCardIndex.value];
    });

    const fetchProblemCards = async () => {
      const problemId = router.currentRoute.value.params.id;
      const requestUrl = `/api/study/${problemId}/cards`; // 변경됨

      try {
        const response = await axios.get(requestUrl, {
          params: { currentUserId: currentUserId.value }
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
      } catch (error) {
        console.error("카드 상태 업데이트 실패:", error);
        alert("카드 상태 업데이트 실패");
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
      alert("학습을 완료했습니다!");
      router.push(`/study/${router.currentRoute.value.params.id}`);
    };

    const toggleShowAnswer = () => {
      showAnswer.value = !showAnswer.value;
    };

    const getCardStatusClass = (status) => {
      if (status === 'solved') return 'card-solved';
      if (status === 'review') return 'card-review';
      if (status === 'unsolved') return 'card-unsolved';
      return '';
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
      isAuthenticated,
      submitAnswer,
      prevCard,
      nextCard,
      finishStudy,
      toggleShowAnswer,
      getCardStatusClass
    };
  }
};
</script>
