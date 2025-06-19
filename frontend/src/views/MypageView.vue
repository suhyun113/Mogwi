<template>
  <div class="mypage-view">
    <div v-if="loading" class="loading-message">사용자 정보를 불러오는 중입니다...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else-if="!isLoggedIn" class="logged-out-prompt">
      <img src="@/assets/mogwi-character.png" alt="모귀 캐릭터" class="mogwi-character-small" />
      <p class="logged-out-message">
        <i class="fas fa-lock"></i> 마이페이지는 로그인 후 이용 가능합니다.
      </p>
      <button @click="showLoginModal = true" class="login-button">로그인</button>
    </div>
    <div v-else class="mypage-layout">
      <!-- Left Sidebar Navigation -->
      <aside class="sidebar">
        <!-- 'MY PAGE' 영어 텍스트 제거 -->
        <h2 class="sidebar-title" style="display: none;">MY PAGE</h2> 
        <nav class="sidebar-nav">
          <a href="#" :class="{ 'nav-item': true, 'active': activeSection === 'profile' }" @click.prevent="activeSection = 'profile'">
            <i class="fas fa-user-circle"></i> 내 정보
          </a>
          <a href="#" :class="{ 'nav-item': true, 'active': activeSection === 'liked-scrapped' }" @click.prevent="activeSection = 'liked-scrapped'">
            <i class="fas fa-heart"></i> 좋아요 & 스크랩
          </a>
          <a href="#" :class="{ 'nav-item': true, 'active': activeSection === 'my-problems' }" @click.prevent="activeSection = 'my-problems'">
            <i class="fas fa-folder-open"></i> 내가 만든 문제
          </a>
          <!-- Add more navigation items if needed -->
          <a href="#" class="nav-item nav-item-danger" @click.prevent="handleDeleteAccount">
            <i class="fas fa-user-times"></i> 회원 탈퇴
          </a>
        </nav>
      </aside>

      <!-- Main Content Area -->
      <main class="main-content">
        <!-- '내 정보' 텍스트 제거 (타이틀 없음) -->

        <section v-if="activeSection === 'profile'" class="content-section">
          <UserProfile
            :nickname="userNickname"
            @update-nickname="handleNicknameUpdate"
            @edit-info="handleEditInfo"
          />
        </section>

        <section v-else-if="activeSection === 'liked-scrapped'" class="content-section">
          <LikedScrapSection
            :likedProblems="likedProblems"
            :scrapedProblems="scrapedProblems"
            @go-to-problem="goToProblem"
          />
        </section>

        <section v-else-if="activeSection === 'my-problems'" class="content-section">
          <MyProblemSection
            :myProblems="myProblems"
            @go-to-problem="goToProblem"
            @edit-problem="editProblem"
            @delete-problem="deleteProblem"
          />
        </section>
      </main>
    </div>

    <LoginModal v-if="showLoginModal" @close="showLoginModal = false" @open-register="openRegisterModal" />
    <RegisterModal v-if="showRegisterModal" @close="showRegisterModal = false" @open-login="openLoginModal" />

    <div v-if="showNicknameUpdateMessage" :class="['alert-message', nicknameUpdateStatus]">
      {{ nicknameUpdateMessage }}
    </div>

    <!-- Custom Confirmation Modal for Delete Account or Problem -->
    <div v-if="showDeleteConfirmModal" class="modal-overlay">
      <div class="modal-content">
        <h3 class="modal-title">
            {{ deleteTarget === 'account' ? '회원 탈퇴 확인' : '문제 삭제 확인' }}
        </h3>
        <p class="modal-message">
            {{ deleteTarget === 'account' ?
               '정말로 회원 탈퇴를 하시겠습니까? 이 작업은 되돌릴 수 없습니다.' :
               '정말로 이 문제를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.' }}
        </p>
        <div class="modal-actions">
          <button @click="deleteTarget === 'account' ? confirmDeleteAccount() : confirmDeleteProblem()" class="confirm-button">
            {{ deleteTarget === 'account' ? '탈퇴하기' : '삭제하기' }}
          </button>
          <button @click="cancelDelete" class="cancel-button">취소</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useStore } from 'vuex';
import axios from 'axios';

// 필요한 컴포넌트들을 import 합니다.
import UserProfile from '@/components/Mypage/UserProfile.vue';
import LikedScrapSection from '@/components/Mypage/LikedScrapSection.vue';
import MyProblemSection from '@/components/Mypage/MyProblemSection.vue';
import LoginModal from '@/components/Login/LoginModal.vue';
import RegisterModal from '@/components/Register/RegisterModal.vue';

export default {
  name: 'MypageView',
  components: {
    UserProfile,
    LikedScrapSection,
    MyProblemSection,
    LoginModal,
    RegisterModal,
  },
  setup() {
    const store = useStore();
    const isLoggedIn = computed(() => !!store.state.store_userid);
    const currentUserId = computed(() => store.state.store_userid);

    const loading = ref(true);
    const error = ref(null);

    const userNickname = ref('');
    const likedProblems = ref([]);
    const scrapedProblems = ref([]);
    const myProblems = ref([]);

    const activeSection = ref('profile');

    const showLoginModal = ref(false);
    const showRegisterModal = ref(false);
    const showDeleteConfirmModal = ref(false);
    const deleteTarget = ref(null); // 'account' or 'problem'
    const problemIdToDelete = ref(null); // 삭제할 문제의 ID

    const showNicknameUpdateMessage = ref(false);
    const nicknameUpdateMessage = ref('');
    const nicknameUpdateStatus = ref('');

    const fetchMypageData = async () => {
      if (!isLoggedIn.value) {
        loading.value = false;
        return;
      }

      loading.value = true;
      error.value = null;

      try {
        const userResponse = await axios.get(`/api/user/${currentUserId.value}`);
        userNickname.value = userResponse.data.nickname;

        const likedScrapResponse = await axios.get(`/api/mypage/problems/liked-scraped/${currentUserId.value}`);
        likedProblems.value = likedScrapResponse.data.likedProblems;
        scrapedProblems.value = likedScrapResponse.data.scrapedProblems;

        const myProblemsResponse = await axios.get(`/api/mypage/problems/my-created/${currentUserId.value}`);
        myProblems.value = myProblemsResponse.data.myProblems;

      } catch (err) {
        console.error('마이페이지 데이터 불러오기 실패:', err);
        error.value = '마이페이지 데이터를 불러오는 데 실패했습니다.';
      } finally {
        loading.value = false;
      }
    };

    // 회원 탈퇴 확인 모달 띄우기
    const handleDeleteAccount = () => {
      deleteTarget.value = 'account'; // 대상 설정
      showDeleteConfirmModal.value = true;
    };

    // 삭제 취소 (공통)
    const cancelDelete = () => {
      showDeleteConfirmModal.value = false;
      deleteTarget.value = null;
      problemIdToDelete.value = null; // 초기화
      nicknameUpdateMessage.value = '삭제 작업이 취소되었습니다.';
      nicknameUpdateStatus.value = 'info'; // 정보성 메시지
      showNicknameUpdateMessage.value = true;
      setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
    };

    // 회원 탈퇴 실행
    const confirmDeleteAccount = async () => {
      showDeleteConfirmModal.value = false; // 모달 닫기
      deleteTarget.value = null; // 대상 초기화
      if (!isLoggedIn.value) {
        nicknameUpdateMessage.value = '로그인 후 이용해주세요.';
        nicknameUpdateStatus.value = 'error';
        showNicknameUpdateMessage.value = true;
        setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
        return;
      }
      try {
        // 실제 회원 탈퇴 API 호출 (예시)
        // await axios.delete(`/api/user/${currentUserId.value}/delete-account`);
        // store.dispatch('logout'); // Vuex 스토어에서 로그아웃 처리
        nicknameUpdateMessage.value = '회원 탈퇴가 성공적으로 처리되었습니다. (기능 미구현)';
        nicknameUpdateStatus.value = 'success';
        showNicknameUpdateMessage.value = true;
        setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
        // 페이지 새로고침 또는 로그인 페이지로 리디렉션
        // window.location.reload(); // 또는 router.push('/login');
      } catch (err) {
        console.error('회원 탈퇴 실패:', err);
        nicknameUpdateMessage.value = '회원 탈퇴 중 오류가 발생했습니다. 다시 시도해주세요.';
        nicknameUpdateStatus.value = 'error';
        showNicknameUpdateMessage.value = true;
        setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
      }
    };

    // 문제 삭제 실행
    const confirmDeleteProblem = async () => {
      showDeleteConfirmModal.value = false; // 모달 닫기
      deleteTarget.value = null; // 대상 초기화
      if (!problemIdToDelete.value) {
        nicknameUpdateMessage.value = '삭제할 문제 ID를 찾을 수 없습니다.';
        nicknameUpdateStatus.value = 'error';
        showNicknameUpdateMessage.value = true;
        setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
        return;
      }

      try {
        await axios.delete(`/api/problems/${problemIdToDelete.value}`); // ProblemController의 API 사용
        myProblems.value = myProblems.value.filter(p => p.id !== problemIdToDelete.value);
        nicknameUpdateMessage.value = '문제가 성공적으로 삭제되었습니다.';
        nicknameUpdateStatus.value = 'success';
      } catch (err) {
        console.error('문제 삭제 실패:', err);
        nicknameUpdateMessage.value = '문제 삭제에 실패했습니다. 다시 시도해주세요.';
        nicknameUpdateStatus.value = 'error';
      } finally {
        showNicknameUpdateMessage.value = true;
        setTimeout(() => {
          showNicknameUpdateMessage.value = false;
        }, 3000);
        problemIdToDelete.value = null; // 문제 ID 초기화
      }
    };


    const openLoginModal = () => {
      showRegisterModal.value = false;
      showLoginModal.value = true;
    };

    const openRegisterModal = () => {
      showLoginModal.value = false;
      showRegisterModal.value = true;
    };

    onMounted(() => {
      fetchMypageData();
    });

    watch(isLoggedIn, (newVal, oldVal) => {
      if (newVal !== oldVal) {
        if (newVal) {
          fetchMypageData();
        } else {
          userNickname.value = '';
          likedProblems.value = [];
          scrapedProblems.value = [];
          myProblems.value = [];
          loading.value = false;
          error.value = null;
          activeSection.value = 'profile';
        }
      }
    }, { immediate: true });

    return {
      loading,
      error,
      isLoggedIn,
      userNickname,
      likedProblems,
      scrapedProblems,
      myProblems,
      activeSection,
      showLoginModal,
      showRegisterModal,
      openLoginModal,
      openRegisterModal,
      showNicknameUpdateMessage,
      nicknameUpdateMessage,
      nicknameUpdateStatus,
      showDeleteConfirmModal,
      deleteTarget,
      handleDeleteAccount,
      confirmDeleteAccount,
      confirmDeleteProblem,
      cancelDelete,
    };
  },
};
</script>

<style scoped>
/* Google Fonts - Inter (or Pretendard if available via local import) */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

.mypage-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  background-color: #f7f3ff; /* 연한 보라색 배경 */
  min-height: 100vh;
  width: 100%;
  box-sizing: border-box;
  font-family: 'Inter', 'Pretendard', sans-serif;
  color: #333;
}

.mypage-layout {
  display: flex;
  width: 100%;
  max-width: 100%;
  background: transparent; /* 흰색 배경 제거 */
  border-radius: 12px;
  /* 변경: 그림자 및 테두리 제거 */
  box-shadow: none; /* 그림자 제거 */
  border: none; /* 테두리 제거 */
  overflow: hidden;
  min-height: 700px;
  align-items: flex-start; /* 메인 컨텐츠와 사이드바 상단 정렬 */
  margin-left: 0;
  margin-right: auto;
}

@media (min-width: 1200px) {
  .mypage-layout {
    justify-content: flex-start;
    margin-left: 0;
    margin-right: auto;
  }
}

/* Sidebar Styling */
.sidebar {
  flex: 0 0 250px;
  background-color: transparent; /* 배경 제거 */
  padding: 30px 20px;
  border-right: 1px solid #efdfff;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 60px;
  margin-left: 140px;
}

.sidebar-title {
  /* 'MY PAGE' 영어 텍스트 제거 */
  display: none;
  font-size: 1.8rem;
  font-weight: 700;
  color: #6a0dad;
  margin-bottom: 40px;
  text-align: center;
  width: 100%;
  padding-bottom: 15px;
  border-bottom: 1px solid #e9dffc;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  font-size: 1.05rem;
  font-weight: 500;
  color: #555;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.2s ease-in-out;
  cursor: pointer;
  background-color: transparent;
  width: 100%;
  box-sizing: border-box;
}

.nav-item i {
  font-size: 1.1rem;
  color: #8a2be2;
}

.nav-item:hover {
  background-color: #f0e6ff;
  color: #6a0dad;
}

.nav-item.active {
  background-color: #e6e0ff;
  color: #6a0dad;
  font-weight: 600;
  box-shadow: 0 1px 6px rgba(106, 13, 219, 0.1);
  position: relative;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  height: 80%;
  width: 4px;
  background-color: #8a2be2;
  border-radius: 2px;
}

.nav-item.active i {
  color: #6a0dad;
}

.nav-item-danger {
  color: #e2586a;
  margin-top: 20px;
}
.nav-item-danger:hover {
  background-color: #ffe6e9;
  color: #d13a4f;
}


/* Main Content Styling */
.main-content {
  flex: 1;
  padding: 0 40px 40px 40px; /* 상단 패딩 더 줄임 */
  background-color: transparent; /* 흰색 배경 제거 */
  margin-top: 100px;
}

.content-section {
  background: none;
  border-radius: 0;
  padding: 0;
}

/* General Messages */
.loading-message, .error-message {
  color: #6c757d;
  font-size: 1.2rem;
  margin-top: 50px;
  text-align: center;
  width: 100%;
}

/* Logged-out Prompt - 기존 디자인 유지 (모귀 캐릭터와 잘 어울림) */
.logged-out-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  padding: 60px 40px;
  width: 100%;
  max-width: 600px;
  margin-top: 80px;
  border: 1px solid #e0d0ff;
}

.mogwi-character-small {
  width: 150px;
  height: auto;
  margin-bottom: 20px;
}

.logged-out-message {
  font-size: 1.6rem;
  color: #4a1e77;
  margin-bottom: 30px;
  line-height: 1.6;
  display: flex;
  align-items: center;
  gap: 15px;
  font-weight: 600;
}

.logged-out-message .fas {
  font-size: 2.2rem;
  color: #8c5dff;
}

/* 로그인 버튼 스타일도 통일된 보라색 그라데이션으로 변경 */
.login-button {
  background-image: linear-gradient(to right, #8a2be2 0%, #a471ff 100%);
  color: white;
  border: none;
  padding: 15px 35px;
  border-radius: 10px;
  font-size: 1.25rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 20px rgba(138, 43, 226, 0.4);
}

.login-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 25px rgba(138, 43, 226, 0.6);
  background-position: right center;
}

/* Alert Message (bottom fixed) */
.alert-message {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  padding: 15px 30px;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: 600;
  color: white;
  z-index: 1000;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  animation: fadeInOut 3s forwards;
}

.alert-message.success {
  background-color: #4CAF50;
}

.alert-message.error {
  background-color: #F44336;
}

@keyframes fadeInOut {
  0% { opacity: 0; transform: translateX(-50%) translateY(20px); }
  10% { opacity: 1; transform: translateX(-50%) translateY(0); }
  90% { opacity: 1; transform: translateX(-50%) translateY(0); }
  100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
}

/* Custom Modal Styling (for confirmation) */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1001;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  text-align: center;
  width: 90%;
  max-width: 450px;
  animation: modalFadeIn 0.3s ease-out;
}

.modal-title {
  font-size: 1.8rem;
  color: #333;
  margin-bottom: 20px;
  font-weight: 600;
}

.modal-message {
  font-size: 1.1rem;
  color: #666;
  margin-bottom: 30px;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.confirm-button, .cancel-button {
  padding: 12px 25px;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.confirm-button {
  background-color: #8a2be2;
  color: white;
  box-shadow: 0 4px 10px rgba(138, 43, 226, 0.3);
}

.confirm-button:hover {
  background-color: #6a0dad;
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(138, 43, 226, 0.4);
}

.cancel-button {
  background-color: #e9ecef;
  color: #444;
}

.cancel-button:hover {
  background-color: #dee2e6;
  transform: translateY(-2px);
}

@keyframes modalFadeIn {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Media Queries for Responsiveness */
@media (max-width: 992px) {
  .mypage-layout {
    flex-direction: column;
    max-width: 700px;
  }

  .sidebar {
    flex: none;
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #efdfff;
    padding: 20px;
  }

  .sidebar-nav {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
  }

  .nav-item {
    padding: 10px 15px;
    font-size: 0.95rem;
    gap: 10px;
  }

  /* 모바일에서 사이드바 메뉴는 왼쪽 보더 강조 제거 */
  .nav-item.active::before {
    display: none;
  }

  .sidebar-title {
    margin-bottom: 20px;
    font-size: 1.5rem;
  }

  .main-content {
    padding: 30px 20px;
  }

  .page-title {
    font-size: 2rem;
    text-align: center;
    margin-bottom: 25px;
  }
}

@media (max-width: 576px) {
  .mypage-view {
    padding: 20px 10px;
  }

  .mypage-layout {
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  }

  .sidebar {
    padding: 15px;
  }

  .sidebar-title {
    font-size: 1.3rem;
    margin-bottom: 15px;
  }

  .nav-item {
    font-size: 0.9rem;
    padding: 8px 12px;
    gap: 8px;
  }

  .nav-item i {
    font-size: 1rem;
  }

  .main-content {
    padding: 20px 15px;
  }

  .page-title {
    font-size: 1.8rem;
    margin-bottom: 20px;
  }

  .logged-out-prompt {
    padding: 40px 20px;
    margin-top: 50px;
  }

  .mogwi-character-small {
    width: 100px;
    margin-bottom: 15px;
  }

  .logged-out-message {
    font-size: 1.3rem;
    margin-bottom: 25px;
    gap: 10px;
  }

  .logged-out-message .fas {
    font-size: 1.8rem;
  }

  .login-button {
    padding: 12px 25px;
    font-size: 1.1rem;
  }

  .alert-message {
    font-size: 0.95rem;
    padding: 12px 20px;
    bottom: 20px;
  }

  .modal-content {
    padding: 20px;
    max-width: 320px;
  }

  .modal-title {
    font-size: 1.5rem;
    margin-bottom: 15px;
  }

  .modal-message {
    font-size: 0.95rem;
    margin-bottom: 20px;
  }

  .confirm-button, .cancel-button {
    padding: 10px 20px;
    font-size: 0.9rem;
  }
}
</style>
