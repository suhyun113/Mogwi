<template>
  <div class="edit-view">
    <div v-if="loading" class="loading-message">문제 정보를 불러오는 중입니다...</div>
    <div v-else-if="error && isLoggedIn" class="error-message">{{ error }}</div>

    <div v-else-if="!isLoggedIn" class="logout-prompt">
      <p class="logout-message">
        <i class="fas fa-lock"></i> 문제 수정은 로그인 후 이용 가능합니다.
      </p>
      <button @click="showLoginModal = true" class="login-button">로그인</button>
    </div>

    <div v-else class="edit-container">
      <h1 class="page-title">문제 수정</h1>
      <ProblemForm
        v-model:title="problem.title"
        v-model:isPublic="problem.is_public"
        v-model:selectedTags="problem.categories"
        v-model:description="problem.description"
        :allCategories="allCategories"
      />

      <div class="card-section">
        <h2 class="section-title">학습 카드 수정</h2>
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
          @click="updateProblem"
          :disabled="!isFormValid || isUploadingImages"
          class="update-problem-btn"
        >
          {{ isUploadingImages ? '이미지 업로드 중...' : '문제 수정 완료' }}
        </button>
      </div>
    </div>

    <LoginModal v-if="showLoginModal" @close="showLoginModal = false" @open-register="openRegisterModal" />
    <RegisterModal v-if="showRegisterModal" @close="closeRegisterModal" />
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import ProblemForm from '@/components/Create/ProblemForm.vue'; // Re-use the form component
import CardInput from '@/components/Create/CardInput.vue';     // Re-use the card input component
import LoginModal from '@/components/Login/LoginModal.vue';
import RegisterModal from '@/components/Register/RegisterModal.vue';

export default {
  name: 'EditView',
  components: {
    ProblemForm,
    CardInput,
    LoginModal,
    RegisterModal,
  },
  setup() {
    const store = useStore();
    const route = useRoute();
    const router = useRouter();

    const currentUserId = computed(() => store.state.store_userid);
    const isLoggedIn = computed(() => !!currentUserId.value);

    const problemId = ref(null); // The ID of the problem to edit

    const loading = ref(true);
    const error = ref(null);
    const submitError = ref('');
    const isUploadingImages = ref(false);

    const allCategories = ref([]);

    const problem = ref({
      title: '',
      description: '',
      is_public: true,
      categories: [], // Renamed from selectedTags to match backend 'categories'
      cards: [],
    });

    // Modal state
    const showLoginModal = ref(false);
    const showRegisterModal = ref(false);

    let nextCardId = 1; // To ensure unique keys for new cards, start from 1 or current max + 1

    const fetchCategories = async () => {
      try {
        const response = await axios.get('/api/categories');
        allCategories.value = response.data.sort((a, b) => a.id - b.id);
      } catch (err) {
        console.error('카테고리 불러오기 실패:', err);
        error.value = '카테고리를 불러오는 데 실패했습니다.';
      }
    };

    const fetchProblemData = async (id) => {
      loading.value = true;
      error.value = null;
      try {
        const response = await axios.get(`/api/problem/${id}`);
        const fetchedProblem = response.data;

        // Ensure current user is the author if this is for private problems
        // For public problems, any logged-in user might be allowed to suggest edits,
        // but for this example, we assume only the author can edit.
        if (fetchedProblem.authorId !== currentUserId.value) {
            router.replace('/403'); // Or redirect to a permission denied page
            return;
        }

        problem.value.title = fetchedProblem.title;
        problem.value.description = fetchedProblem.description || '';
        problem.value.is_public = fetchedProblem.is_public === 1; // Convert 0/1 to boolean
        // Map category objects to their IDs for ProblemForm's selectedTags prop
        problem.value.categories = fetchedProblem.categories.map(cat => cat.id);

        // Assign existing cards and ensure unique IDs for them (for v-for key)
        problem.value.cards = fetchedProblem.cards.map(card => ({
            id: card.id, // Use actual card ID if available, otherwise generate
            question: card.question,
            answer: card.answer,
            image_url: card.image_url,
            image_file: null // No file initially when loading from server
        }));

        // Find the maximum existing card ID to avoid conflicts with new cards
        if (problem.value.cards.length > 0) {
            nextCardId = Math.max(...problem.value.cards.map(card => card.id)) + 1;
        } else {
            nextCardId = 1;
        }

      } catch (err) {
        console.error('문제 정보 불러오기 실패:', err);
        error.value = '문제 정보를 불러오는 데 실패했습니다.';
        if (err.response && err.response.status === 404) {
            error.value = '존재하지 않는 문제이거나, 접근 권한이 없습니다.';
        }
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
      if (problem.value.categories.length === 0 || problem.value.categories.length > 3) {
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
        // Only upload if a new file is selected for the card
        if (card.image_file) {
          const formData = new FormData();
          formData.append('file', card.image_file);

          try {
            const response = await axios.post('/api/upload-image', formData, {
              headers: {
                'Content-Type': 'multipart/form-data'
              }
            });

            if (response.data.status === 'OK' && response.data.imageUrl) {
              card.image_url = response.data.imageUrl;
              card.image_file = null; // Clear file after successful upload
              console.log(`Card ${index + 1} image uploaded successfully:`, card.image_url);
            } else {
              console.error(`이미지 업로드 실패: 카드 ${index + 1} -`, response.data.message);
              submitError.value = `카드 ${index + 1} 이미지 업로드에 실패했습니다: ${response.data.message || '알 수 없는 오류'}`;
              throw new Error(`이미지 업로드 실패: ${response.data.message || '알 수 없는 오류'}`);
            }
          } catch (uploadErr) {
            console.error(`카드 ${index + 1} 이미지 업로드 중 오류 발생:`, uploadErr);
            submitError.value = `카드 ${index + 1} 이미지 업로드 중 오류가 발생했습니다: ${uploadErr.message || '네트워크 오류'}`;
            throw uploadErr;
          }
        }
      });

      try {
        await Promise.all(uploadPromises);
        return true;
      } catch (e) {
        return false;
      } finally {
        isUploadingImages.value = false;
      }
    };

    const updateProblem = async () => {
      if (!isFormValid.value) {
        submitError.value = '모든 필수 정보를 입력하고, 각 카드에 질문과 정답을 입력해주세요. 태그는 1~3개 선택해야 합니다.';
        return;
      }

      submitError.value = '';

      const imagesUploaded = await uploadImages();
      if (!imagesUploaded) {
        return;
      }

      try {
        const payload = {
          title: problem.value.title,
          author_id: currentUserId.value, // Ensure author_id is sent for validation
          description: problem.value.description,
          is_public: problem.value.is_public ? 1 : 0,
          categories: problem.value.categories, // Send category IDs
          cards: problem.value.cards.map(card => ({
            // If card.id is null/undefined for new cards, backend should handle it (e.g., auto-increment)
            // If it's an existing card, sending its ID allows backend to update it
            id: card.id, // Send existing card IDs for update or null for new cards
            question: card.question,
            answer: card.answer,
            image_url: card.image_url || null
          }))
        };

        const response = await axios.put(`/api/problems/${problemId.value}`, payload);

        if (response.data.status === 'OK') {
          alert('문제가 성공적으로 수정되었습니다!');
          router.push(`/problem/${problemId.value}`); // Redirect to the problem detail page
        } else {
          submitError.value = response.data.message || '문제 수정에 실패했습니다.';
        }
      } catch (err) {
        console.error('문제 수정 오류:', err);
        if (err.response && err.response.data && err.response.data.message) {
          submitError.value = `문제 수정 중 오류가 발생했습니다: ${err.response.data.message}`;
        } else {
          submitError.value = '문제 수정 중 알 수 없는 오류가 발생했습니다.';
        }
      } finally {
        isUploadingImages.value = false;
      }
    };

    const openLoginModal = () => {
      showLoginModal.value = true;
    };

    const openRegisterModal = () => {
      showLoginModal.value = false;
      showRegisterModal.value = true;
    };

    const closeRegisterModal = () => {
      showRegisterModal.value = false;
    };

    // On mount, get problem ID from route and fetch data
    onMounted(async () => {
      problemId.value = route.params.id;
      if (problemId.value) {
        await fetchCategories();
        if (isLoggedIn.value) { // Only fetch problem data if logged in
            await fetchProblemData(problemId.value);
        } else {
            loading.value = false; // Stop loading if not logged in
        }
      } else {
        error.value = '수정할 문제 ID를 찾을 수 없습니다.';
        loading.value = false;
      }
    });

    // Watch for login status changes
    watch(isLoggedIn, async (newVal, oldVal) => {
      if (newVal !== oldVal) {
        if (newVal && problemId.value) {
          await fetchCategories(); // Ensure categories are loaded
          await fetchProblemData(problemId.value); // Fetch problem data if just logged in
          showLoginModal.value = false; // Close modal if login was successful
        } else if (!newVal) {
            // If logged out, clear data or show login prompt
            problem.value = {
                title: '', description: '', is_public: true, categories: [], cards: []
            };
            loading.value = false;
            error.value = null;
        }
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
      updateProblem, // Changed from createProblem
      isLoggedIn,
      showLoginModal,
      openLoginModal,
      showRegisterModal,
      openRegisterModal,
      closeRegisterModal,
    };
  },
};
</script>

<style scoped>
/* Inherit most styles from CreateView.vue, with minor adjustments */
@import url('https://fonts.googleapis.com/css2?family=Pretendard:wght@300;400;500;600;700&display=swap');

.edit-view {
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
    font-size: 2.5rem;
    color: #4a1e77;
    margin-bottom: 40px;
    font-weight: 700;
    text-align: center;
    width: 100%;
}

.edit-container {
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

/* Logout Prompt (from CreateView, reused) */
.logout-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  padding: 60px 30px;
  width: 100%;
  max-width: 500px;
  margin: 80px auto;
  border: 1px solid #eee;
}

.logout-message {
  font-size: 1.5rem;
  color: #4a1e77;
  margin-bottom: 30px;
  line-height: 1.6;
  display: flex;
  align-items: center;
  gap: 15px;
}

.logout-message .fas {
  font-size: 2rem;
  color: #8c5dff;
}

.login-button {
  background-image: linear-gradient(to right, #8c5dff 0%, #a471ff 100%);
  color: white;
  border: none;
  padding: 15px 30px;
  border-radius: 10px;
  font-size: 1.2rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 20px rgba(140, 93, 255, 0.4);
}

.login-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 25px rgba(140, 93, 255, 0.6);
  background-position: right center;
}

/* Ensure the main container doesn't show when logged out */
.edit-view > .edit-container {
    display: v-bind("isLoggedIn && !loading && !error ? 'block' : 'none'"); /* Hide form when logged out or loading/error */
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
  background-color: #e6e0f4;
  color: #5a2e87;
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
  background-color: #d1c4e9;
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

.update-problem-btn { /* Renamed class for clarity */
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

.update-problem-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 12px 25px rgba(164, 113, 255, 0.6);
  background-position: right center;
}

.update-problem-btn:disabled {
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

/* Media Queries for Responsiveness */
@media (max-width: 768px) {
  .edit-container {
    padding: 20px;
  }
  .page-title {
    font-size: 2rem;
    margin-bottom: 30px;
  }
  .section-title {
    font-size: 1.5rem;
  }
  .add-card-btn, .update-problem-btn {
    font-size: 1rem;
    padding: 10px 15px;
  }
  .logout-prompt {
    padding: 40px 20px;
    margin: 50px auto;
  }
  .logout-message {
    font-size: 1.2rem;
    gap: 10px;
  }
  .logout-message .fas {
    font-size: 1.7rem;
  }
  .login-button {
    padding: 12px 25px;
    font-size: 1rem;
  }
}
</style>