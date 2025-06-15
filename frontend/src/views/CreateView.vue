<template>
  <div class="create-view">
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
            @update:image_file="file => handleImageFile(index, file)"
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
          :disabled="!isFormValid || isUploadingImages"
          class="create-problem-btn"
        >
          {{ isUploadingImages ? '이미지 업로드 중...' : '문제 생성하기' }}
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
  components: {
    ProblemForm,
    CardInput
  },
  setup() {
    const store = useStore();
    const router = useRouter();

    const currentUserId = computed(() => store.state.store_userid);
    const isLoggedIn = computed(() => !!currentUserId.value);

    const loading = ref(true);
    const error = ref(null);
    const submitError = ref('');
    const isUploadingImages = ref(false);

    const allCategories = ref([]);

    const problem = ref({
      title: '',
      description: '',
      is_public: true,
      selectedTags: [],
      cards: [{ id: 1, question: '', answer: '', image_url: '', image_file: null }]
    });

    let nextCardId = 2;

    const fetchCategories = async () => {
      try {
        // Axios 기본 URL 설정 (main.js 또는 여기에 설정)
        const response = await axios.get('/api/categories');
        allCategories.value = response.data.sort((a, b) => a.id - b.id);
      } catch (err) {
        console.error('카테고리 불러오기 실패:', err);
        error.value = '카테고리를 불러오는 데 실패했습니다.';
      } finally {
        loading.value = false;
      }
    };

    const isFormValid = computed(() => {
      if (!isLoggedIn.value) {
        return false;
      }
      if (!problem.value.title.trim()) {
        return false;
      }
      // 태그는 최소 1개 이상, 최대 3개
      if (problem.value.selectedTags.length === 0 || problem.value.selectedTags.length > 3) {
          return false;
      }
      if (problem.value.cards.length === 0) {
        return false;
      }
      const allCardsValid = problem.value.cards.every(card =>
        card.question.trim() && card.answer.trim()
      );
      if (!allCardsValid) {
        return false;
      }
      return true;
    });

    const addCard = () => {
      problem.value.cards.push({ id: nextCardId++, question: '', answer: '', image_url: '', image_file: null });
    };

    const removeCard = (index) => {
      if (problem.value.cards.length > 1) {
        problem.value.cards.splice(index, 1);
      } else {
        alert('최소 하나 이상의 카드가 필요합니다.');
      }
    };

    const updateCardField = (index, field, value) => {
      problem.value.cards[index][field] = value;
    };

    const handleImageFile = (index, file) => {
      problem.value.cards[index].image_file = file;
    };

    const uploadImages = async () => {
        isUploadingImages.value = true;
        submitError.value = '';

        const uploadPromises = problem.value.cards.map(async (card, index) => {
            if (card.image_file) {
                const formData = new FormData();
                // 백엔드 FileUploadController의 @RequestParam("file")과 일치시켜야 함
                formData.append('file', card.image_file);

                try {
                    // TODO: 실제 백엔드 서버 URL과 포트로 변경하세요!
                    // 예시: 'http://localhost:8000/api/upload-image' 또는 '/api/upload-image' (proxy 설정 사용 시)
                    const response = await axios.post('/api/upload-image', formData, {
                        headers: {
                            'Content-Type': 'multipart/form-data'
                        }
                    });

                    if (response.data.status === 'OK' && response.data.imageUrl) {
                        card.image_url = response.data.imageUrl; // 서버에서 반환된 URL로 업데이트
                        card.image_file = null; // 업로드 완료된 파일 객체는 제거
                        console.log(`Card ${index + 1} image uploaded successfully:`, card.image_url);
                    } else {
                        console.error(`이미지 업로드 실패: 카드 ${index + 1} -`, response.data.message);
                        submitError.value = `카드 ${index + 1} 이미지 업로드에 실패했습니다: ${response.data.message || '알 수 없는 오류'}`;
                        throw new Error(`이미지 업로드 실패: ${response.data.message || '알 수 없는 오류'}`);
                    }
                } catch (uploadErr) {
                    console.error(`카드 ${index + 1} 이미지 업로드 중 오류 발생:`, uploadErr);
                    // Network errors or server errors will also be caught here
                    submitError.value = `카드 ${index + 1} 이미지 업로드 중 오류가 발생했습니다: ${uploadErr.message || '네트워크 오류'}`;
                    throw uploadErr; // Propagate the error to stop Promise.all
                }
            }
        });

        try {
            await Promise.all(uploadPromises);
            return true; // All images uploaded successfully
        } catch (e) {
            // An error occurred during one of the image uploads, error message already set
            return false; // Image upload failed
        } finally {
            isUploadingImages.value = false; // 이미지 업로드 상태 종료
        }
    };

    const createProblem = async () => {
      if (!isFormValid.value) {
        submitError.value = '모든 필수 정보를 입력하고, 각 카드에 질문과 정답을 입력해주세요. 태그는 1~3개 선택해야 합니다.';
        return;
      }

      if (!isLoggedIn.value) {
        alert('로그인 후 문제 생성이 가능합니다.');
        router.push('/login');
        return;
      }

      submitError.value = '';
      // isUploadingImages는 uploadImages 함수 내부에서 관리되므로 여기서 직접 true로 설정할 필요 없음

      // 1. 모든 이미지 업로드
      const imagesUploaded = await uploadImages();
      if (!imagesUploaded) {
          // 이미지 업로드 실패 시 이미 submitError가 설정되어 있으므로 추가 처리 없음
          return;
      }

      // 2. 모든 이미지 URL이 설정된 최종 문제 데이터를 서버에 전송
      try {
        const payload = {
          title: problem.value.title,
          author_id: currentUserId.value,
          description: problem.value.description,
          is_public: problem.value.is_public ? 1 : 0, // 백엔드에서 Integer로 받으므로 1 또는 0으로 변환
          categories: problem.value.selectedTags, // 이미 ID 배열
          cards: problem.value.cards.map(card => ({
            question: card.question,
            answer: card.answer,
            image_url: card.image_url || null // 업로드된 URL 또는 null
          }))
        };

        const response = await axios.post('/api/problems', payload);

        if (response.data.status === 'OK') {
          alert('문제가 성공적으로 생성되었습니다!');
          router.push('mystudy'); // 이름으로 라우트 이동
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
      } finally {
        // 문제 생성 완료 후에도 isUploadingImages를 false로 설정 (uploadImages에서 이미 했지만 한 번 더 확인)
        isUploadingImages.value = false;
      }
    };

    onMounted(() => {
      fetchCategories();
      if (!isLoggedIn.value) {
        // router.push('/login'); // 이미 watch에서 처리하고 있으므로 중복 가능
      }
    });

    watch(isLoggedIn, (newVal) => {
      if (!newVal) {
        alert('로그아웃되었습니다. 문제 생성이 취소됩니다.');
        router.push('/login');
      }
    });

    return {
      loading,
      error,
      submitError,
      isUploadingImages,
      allCategories,
      problem,
      isFormValid,
      addCard,
      removeCard,
      updateCardField,
      handleImageFile,
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

.create-container {
  background: white;
  border-radius: 0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  padding: 40px;
  width: 100%;
  max-width: 800px;
  margin-bottom: 20px;
  margin-top: 20px;
  border: 1px solid #eee;
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

/* New style for file input */
.form-input-file {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 0.95rem;
  color: #555;
  box-sizing: border-box;
  transition: border-color 0.3s, box-shadow 0.3s;
  background-color: #f8f8f8;
  cursor: pointer;
}

.form-input-file:focus {
  border-color: #a471ff;
  box-shadow: 0 0 0 3px rgba(164, 113, 255, 0.15);
  outline: none;
}

.clear-image-btn {
  background-color: #f4e8f9;
  color: #5a2e87;
  border: 1px solid #a471ff;
  padding: 8px 15px;
  border-radius: 5px;
  font-size: 0.9rem;
  cursor: pointer;
  margin-top: 10px;
  transition: background-color 0.2s;
}

.clear-image-btn:hover {
  background-color: #e6d7f0;
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