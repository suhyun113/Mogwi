<template>
  <div class="create-view">
    <h1 class="page-title">새 문제 생성하기</h1>
    <div v-if="loading" class="loading-message">데이터를 불러오는 중입니다...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>

    <div class="create-container">
      <ProblemForm
        v-model:title="problem.title"
        v-model:isPublic="problem.is_public"
        v-model:selectedTags="problem.selectedTags"
        v-model:description="problem.description"
        :allCategories="allCategories"
      />

      <div class="card-section">
        <h2 class="section-title">학습 카드 추가</h2>
        <p class="section-description">질문과 정답을 입력하고, 필요하다면 이미지를 첨부해보세요.</p>
        <div v-for="(card, index) in problem.cards" :key="card.id" class="card-item-wrapper">
          <CardInput
            :card="card"
            :index="index"
            @update:question="value => updateCardField(index, 'question', value)"
            @update:answer="value => updateCardField(index, 'answer', value)"
            @update:image_url="value => updateCardField(index, 'image_url', value)"
            @remove-card="removeCard(index)"
          />
        </div>
        <button @click="addCard" class="add-card-btn">
          <span class="icon">+</span> 카드 추가
        </button>
      </div>

      <div class="actions">
        <p v-if="submitError" class="error-msg">{{ submitError }}</p>
        <button
          @click="createProblem"
          :disabled="!isFormValid"
          class="create-problem-btn"
        >
          문제 생성하기
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import axios from 'axios';
import ProblemForm from '@/components/Create/ProblemForm.vue';
import CardInput from '@/components/Create/CardInput.vue';

export default {
  // 컴포넌트 등록
  components: {
    ProblemForm,
    CardInput
  },
  // setup 함수: Composition API 로직을 여기서 작성
  setup() {
    const store = useStore();
    const router = useRouter();

    const currentUserId = computed(() => store.state.store_userid);
    const isLoggedIn = computed(() => !!currentUserId.value);

    const loading = ref(true);
    const error = ref(null);
    const submitError = ref('');

    const allCategories = ref([]);

    const problem = ref({
      title: '',
      description: '',
      is_public: true, // 기본값은 공개
      selectedTags: [],
      cards: [{ id: 1, question: '', answer: '', image_url: '' }] // 최소 1개의 카드
    });

    let nextCardId = 2; // 다음 카드에 할당할 ID

    // 모든 카테고리 불러오기
    const fetchCategories = async () => {
      try {
        const response = await axios.get('/api/categories');
        allCategories.value = response.data;
        console.log("Fetched categories:", allCategories.value);
      } catch (err) {
        console.error('카테고리 불러오기 실패:', err);
        error.value = '카테고리를 불러오는 데 실패했습니다.';
      } finally {
        loading.value = false;
      }
    };

    // 필수 입력값 유효성 검사
    const isFormValid = computed(() => {
      // 로그인 상태 확인
      if (!isLoggedIn.value) {
        return false;
      }
      // 문제 제목 필수
      if (!problem.value.title.trim()) {
        return false;
      }
      // 태그 최소 1개 선택 필수
      if (problem.value.selectedTags.length === 0) {
        return false;
      }
      // 카드 최소 1개 필수
      if (problem.value.cards.length === 0) {
        return false;
      }
      // 모든 카드에 질문과 정답 필수
      const allCardsValid = problem.value.cards.every(card =>
        card.question.trim() && card.answer.trim()
      );
      if (!allCardsValid) {
        return false;
      }
      return true;
    });

    // 카드 추가
    const addCard = () => {
      problem.value.cards.push({ id: nextCardId++, question: '', answer: '', image_url: '' });
    };

    // 카드 제거
    const removeCard = (index) => {
      if (problem.value.cards.length > 1) { // 최소 1개의 카드는 유지
        problem.value.cards.splice(index, 1);
      } else {
        alert('최소 하나 이상의 카드가 필요합니다.');
      }
    };

    // 카드 필드 업데이트 헬퍼
    const updateCardField = (index, field, value) => {
      problem.value.cards[index][field] = value;
    };

    // 문제 생성 요청
    const createProblem = async () => {
      if (!isFormValid.value) {
        submitError.value = '모든 필수 정보를 입력하고, 각 카드에 질문과 정답을 입력해주세요.';
        return;
      }

      if (!isLoggedIn.value) {
        alert('로그인 후 문제 생성이 가능합니다.');
        router.push('/login'); // 로그인 페이지로 리디렉션 또는 로그인 모달 표시
        return;
      }

      submitError.value = ''; // 에러 메시지 초기화

      try {
        const payload = {
          title: problem.value.title,
          author_id: currentUserId.value,
          description: problem.value.description,
          is_public: problem.value.is_public ? 1 : 0,
          categories: problem.value.selectedTags,
          cards: problem.value.cards.map(card => ({
            question: card.question,
            answer: card.answer,
            image_url: card.image_url || null
          }))
        };

        const response = await axios.post('/api/problems', payload);

        if (response.data.status === 'OK') {
          alert('문제가 성공적으로 생성되었습니다!');
          router.push({ name: 'MyStudy' });
        } else {
          submitError.value = response.data.message || '문제 생성에 실패했습니다.';
        }
      } catch (err) {
        console.error('문제 생성 오류:', err);
        if (err.response && err.response.data && err.response.data.message) {
          submitError.value = `문제 생성 중 오류가 발생했습니다: ${err.response.data.message}`;
        } else {
          submitError.value = '문제 생성 중 알 수 없는 오류가 발생했습니다.';
        }
      }
    };

    onMounted(() => {
      fetchCategories();
      if (!isLoggedIn.value) {
        alert('로그인 후 문제 생성이 가능합니다.');
        router.push('/login');
      }
    });

    watch(isLoggedIn, (newVal) => {
      if (!newVal) {
        alert('로그아웃되었습니다. 문제 생성이 취소됩니다.');
        router.push('/login');
      }
    });

    // 템플릿에서 사용될 모든 반응형 데이터, computed 속성, 함수들을 반환합니다.
    return {
      loading,
      error,
      submitError,
      allCategories,
      problem,
      isFormValid,
      addCard,
      removeCard,
      updateCardField,
      createProblem
    };
  }
};
</script>

<style scoped>
/* 기존 스타일은 동일하게 유지됩니다. */
.create-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  background-color: #fdf8f4;
  min-height: 100vh;
  width: 100%;
  box-sizing: border-box;
  font-family: 'Pretendard', sans-serif;
}

.page-title {
  color: #5a2e87;
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 30px;
  text-align: center;
  width: 100%;
  max-width: 800px;
}

.create-container {
  background: white;
  border-radius: 15px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  padding: 30px;
  width: 100%;
  max-width: 800px;
  margin-bottom: 50px;
}

.loading-message, .error-message {
  color: #6c757d;
  font-size: 1.1rem;
  margin-top: 30px;
  text-align: center;
}

.section-title {
  color: #4a1e77;
  font-size: 1.8rem;
  font-weight: 600;
  margin-top: 30px;
  margin-bottom: 10px;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
}

.section-description {
  color: #777;
  font-size: 0.95rem;
  margin-bottom: 20px;
  line-height: 1.4;
}

.card-section {
  margin-top: 40px;
}

.card-item-wrapper {
  background-color: #f9f9f9;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease-in-out;
}

.card-item-wrapper:hover {
  border-color: #a471ff;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.add-card-btn {
  background-color: #e6e0f4; /* Light purple */
  color: #5a2e87; /* Dark purple */
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.2s;
  width: 100%;
  margin-top: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.add-card-btn:hover {
  background-color: #d1c4e9; /* Slightly darker light purple */
  transform: translateY(-2px);
}

.add-card-btn .icon {
  font-size: 1.5rem;
  line-height: 1;
}

.actions {
  margin-top: 40px;
  text-align: center;
}

.create-problem-btn {
  background-image: linear-gradient(to right, #a471ff 0%, #8c5dff 100%);
  color: white;
  border: none;
  padding: 15px 30px;
  border-radius: 10px;
  font-size: 1.3rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
  max-width: 300px;
  box-shadow: 0 8px 20px rgba(164, 113, 255, 0.4);
}

.create-problem-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 12px 25px rgba(164, 113, 255, 0.6);
  background-position: right center;
}

.create-problem-btn:disabled {
  background-image: linear-gradient(to right, #d1c4e9 0%, #b3a4d6 100%);
  cursor: not-allowed;
  opacity: 0.7;
  box-shadow: none;
}

.error-msg {
  color: #e74c3c;
  font-size: 0.95rem;
  margin-bottom: 15px;
  text-align: center;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
    margin-bottom: 20px;
  }
  .create-container {
    padding: 20px;
  }
  .section-title {
    font-size: 1.5rem;
  }
  .add-card-btn, .create-problem-btn {
    font-size: 1rem;
    padding: 10px 15px;
  }
}
</style>