<template>
  <div class="mypage-view-wrapper">
    <div v-if="loading && isLoggedIn" class="loading-message">사용자 정보를 불러오는 중입니다...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    
    <div class="mypage-layout">
      <aside class="sidebar">
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
          <a v-if="isLoggedIn" href="#" class="nav-item nav-item-danger" @click.prevent="handleDeleteAccount">
            <i class="fas fa-user-times"></i> 회원 탈퇴
          </a>
        </nav>
      </aside>

      <main class="main-content">
        <section v-if="activeSection === 'profile'" class="content-section">
          <UserProfile
            :nickname="userNickname"
            :userEmail="userEmail"
            :isLoggedIn="isLoggedIn"
            @edit-info="showEditProfileModal = true"
          />
        </section>

        <div v-else class="content-wrapper" :class="{ blurred: !isLoggedIn }">
          <section v-if="activeSection === 'liked-scrapped'" class="content-section">
            <LikedScrapSection
              :likedProblems="likedProblems"
              :scrapedProblems="scrapedProblems"
              @go-to-problem="goToProblem"
              :isAuthenticated="isLoggedIn"
              :currentUserId="currentUserId"
              @update-like="fetchMypageData"
              @update-scrap="fetchMypageData"
            />
          </section>

          <section v-else-if="activeSection === 'my-problems'" class="content-section">
            <MyProblemSection
              :myProblems="myProblems"
              @go-to-problem="goToProblem"
              @edit-problem="editProblem"
              @delete-problem="fetchMypageData"
              :isAuthenticated="isLoggedIn"
              :currentUserId="currentUserId"
            />
          </section>

          <div v-if="!isLoggedIn" class="content-login-prompt">
            <img src="@/assets/mogwi-confused.png" alt="모귀 캐릭터" class="prompt-mogwi-character" />
            <p class="prompt-message">로그인하시면<br>이용할 수 있는 기능이에요!</p>
            <div class="prompt-actions">
              <button @click="openLoginModal" class="action-button login-button">로그인</button>
              <button @click="openRegisterModal" class="action-button register-button">회원가입</button>
            </div>
          </div>
        </div>
      </main>
    </div>

    <LoginModal v-if="showLoginModal" @close="showLoginModal = false" @open-register="openRegisterModal" />
    <RegisterModal v-if="showRegisterModal" @close="showRegisterModal = false" @open-login="openLoginModal" />

    <div v-if="showNicknameUpdateMessage" :class="['alert-message', nicknameUpdateStatus]">
      {{ nicknameUpdateMessage }}
    </div>

    <EditProfileModal
      v-if="showEditProfileModal"
      :initialNickname="userNickname"
      :initialEmail="userEmail"
      :userId="currentUserId" @close="showEditProfileModal = false"
      @update-profile="handleProfileUpdateFromModal"
    />

    <DeleteAccountModal
      v-if="showDeleteConfirmModal && deleteTarget === 'account'"
      @confirm="confirmDeleteAccount"
      @cancel="cancelDelete"
    />
  </div>
</template>

<script>
/* eslint-disable vue/valid-v-else */
/* eslint-disable vue/no-unused-components */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useStore } from 'vuex';
import axios from 'axios';
import { useRouter } from 'vue-router';

// 필요한 컴포넌트들을 import 합니다.
import UserProfile from '@/components/Mypage/UserProfile.vue';
import LikedScrapSection from '@/components/Mypage/LikedScrapSection.vue';
import MyProblemSection from '@/components/Mypage/MyProblemSection.vue';
import LoginModal from '@/components/Login/LoginModal.vue';
import RegisterModal from '@/components/Register/RegisterModal.vue';
import DeleteAccountModal from '@/components/Mypage/DeleteAccountModal.vue';
import EditProfileModal from '@/components/Mypage/EditProfileModal.vue';

export default {
  name: 'MypageView',
  components: {
    UserProfile,
    LikedScrapSection,
    MyProblemSection,
    LoginModal,
    RegisterModal,
    DeleteAccountModal,
    EditProfileModal,
  },
  setup() {
    const store = useStore();
    const router = useRouter();

    const isLoggedIn = computed(() => !!store.state.store_userid);
    const currentUserId = computed(() => store.state.store_userid);

    const loading = ref(true);
    const error = ref(null);

    const userNickname = ref('');
    const userEmail = ref('');
    const likedProblems = ref([]);
    const scrapedProblems = ref([]);
    const myProblems = ref([]);

    const activeSection = ref('profile');

    const showLoginModal = ref(false);
    const showRegisterModal = ref(false);
    const showDeleteConfirmModal = ref(false);
    const showEditProfileModal = ref(false);
    const deleteTarget = ref(null);
    const problemIdToDelete = ref(null);

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
        if (userResponse.data.status === 'OK' && userResponse.data.user) {
          userNickname.value = userResponse.data.user.username;
          userEmail.value = userResponse.data.user.usermail;
        } else {
          userNickname.value = '알 수 없음';
          userEmail.value = '알 수 없음';
        }

        // 내가 좋아요 누른 문제만 조회
        const likedResponse = await axios.get(`/api/problem/detail?currentUserId=${currentUserId.value}&onlyLiked=true`);
        likedProblems.value = likedResponse.data.map(problem => ({
          ...problem,
          liked: problem.isLiked,
          scrapped: problem.isScrapped,
          likes: problem.totalLikes || 0,
          scraps: problem.totalScraps || 0
        }));

        // 내가 스크랩 누른 문제만 조회
        const scrapResponse = await axios.get(`/api/problem/detail?currentUserId=${currentUserId.value}&onlyScrapped=true`);
        scrapedProblems.value = scrapResponse.data.map(problem => ({
          ...problem,
          liked: problem.isLiked,
          scrapped: problem.isScrapped,
          likes: problem.totalLikes || 0,
          scraps: problem.totalScraps || 0
        }));

        const myProblemsResponse = await axios.get(`/api/problem/detail?currentUserId=${currentUserId.value}&onlyMine=true`);
        myProblems.value = myProblemsResponse.data.map(problem => ({
          ...problem,
          liked: problem.isLiked,
          scrapped: problem.isScrapped,
          likes: problem.totalLikes || 0,
          scraps: problem.totalScraps || 0
        }));

      } catch (err) {
        console.error('마이페이지 데이터 불러오기 실패:', err);
        error.value = '마이페이지 데이터를 불러오는 데 실패했습니다.';
      } finally {
        loading.value = false;
      }
    };

    const handleDeleteAccount = () => {
      deleteTarget.value = 'account';
      showDeleteConfirmModal.value = true;
    };

    const cancelDelete = () => {
      showDeleteConfirmModal.value = false;
      deleteTarget.value = null;
      problemIdToDelete.value = null;
      nicknameUpdateMessage.value = '삭제 작업이 취소되었습니다.';
      nicknameUpdateStatus.value = 'info';
      showNicknameUpdateMessage.value = true;
      setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
    };

    const confirmDeleteAccount = async () => {
      showDeleteConfirmModal.value = false;
      deleteTarget.value = null;

      if (!isLoggedIn.value) {
        nicknameUpdateMessage.value = '로그인 후 이용해주세요.';
        nicknameUpdateStatus.value = 'error';
        showNicknameUpdateMessage.value = true;
        setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
        return;
      }

      try {
        // 실제 사용자 삭제 API 호출
        await axios.delete(`/api/user/${currentUserId.value}`);

        alert('회원 탈퇴가 성공적으로 처리되었습니다.');

        // 로그아웃 처리 및 메인으로 이동
        store.dispatch('logout'); // 로그아웃 처리
        router.push('/'); // 메인으로 이동

      } catch (err) {
        console.error('회원 탈퇴 실패:', err);

        nicknameUpdateMessage.value = '회원 탈퇴 중 오류가 발생했습니다. 다시 시도해주세요.';
        nicknameUpdateStatus.value = 'error';
        showNicknameUpdateMessage.value = true;
        setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
      }
    };

    const confirmDeleteProblem = async () => {
      showDeleteConfirmModal.value = false;
      deleteTarget.value = null;
      if (!problemIdToDelete.value) {
        nicknameUpdateMessage.value = '삭제할 문제 ID를 찾을 수 없습니다.';
        nicknameUpdateStatus.value = 'error';
        showNicknameUpdateMessage.value = true;
        setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
        return;
      }

      try {
        await axios.delete(`/api/problem/${problemIdToDelete.value}`);
        myProblems.value = myProblems.value.filter(p => p.id !== problemIdToDelete.value);
        nicknameUpdateMessage.value = '문제가 성공적으로 삭제되었습니다.';
        nicknameUpdateStatus.value = 'success';
        await fetchMypageData(); // 삭제 후 자동 새로고침
      } catch (err) {
        console.error('문제 삭제 실패:', err);
        nicknameUpdateMessage.value = '문제 삭제에 실패했습니다. 다시 시도해주세요.';
        nicknameUpdateStatus.value = 'error';
      } finally {
        showNicknameUpdateMessage.value = true;
        setTimeout(() => {
          showNicknameUpdateMessage.value = false;
        }, 3000);
        problemIdToDelete.value = null;
      }
    };

    const goToProblem = (problemId) => {
        router.push(`/problem/${problemId}`);
    };

    const editProblem = (problemId) => {
        router.push(`/edit/${problemId}`);
    };

    const deleteProblem = (problemId) => {
        deleteTarget.value = 'problem';
        problemIdToDelete.value = problemId;
        showDeleteConfirmModal.value = true;
    };

    const openLoginModal = () => {
      showRegisterModal.value = false;
      showLoginModal.value = true;
    };

    const openRegisterModal = () => {
      showLoginModal.value = false;
      showRegisterModal.value = true;
    };

    const handleProfileUpdateFromModal = async (updatedData) => {
      // Assuming updatedData contains { nickname, email }
      userNickname.value = updatedData.nickname;
      userEmail.value = updatedData.email;
      // Optionally show a success message
      nicknameUpdateMessage.value = '프로필 정보가 성공적으로 업데이트되었습니다.';
      nicknameUpdateStatus.value = 'success';
      showNicknameUpdateMessage.value = true;
      setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
      showEditProfileModal.value = false; // Close the modal
    };


    onMounted(() => {
      fetchMypageData();
      document.body.style.overflow = 'hidden';
    });
    onUnmounted(() => {
      document.body.style.overflow = '';
    });

    watch(isLoggedIn, (newVal, oldVal) => {
      if (newVal !== oldVal) {
        if (newVal) {
          fetchMypageData();
        } else {
          userNickname.value = '';
          userEmail.value = '';
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
      userEmail,
      likedProblems,
      scrapedProblems,
      myProblems,
      activeSection,
      showLoginModal,
      currentUserId,
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
      showEditProfileModal,
      handleProfileUpdateFromModal,
      goToProblem,
      editProblem,
      deleteProblem,
      fetchMypageData // LikedScrapSection에서 좋아요/스크랩 업데이트 시 데이터 다시 불러오도록 노출
    };
  },
};
</script>

<style>
/* 전역 스타일: HTML 및 BODY에 적용하여 다른 페이지에서 스크롤이 정상 작동하도록 합니다. */
html, body {
    margin: 0;
    padding: 0;
    height: 100%; /* 뷰포트 높이 전체를 사용 */
    box-sizing: border-box; /* 모든 요소에 적용 (패딩/보더를 너비/높이에 포함) */
    /* overflow: hidden; 제거 - 다른 페이지 스크롤 허용 */
}

*, *::before, *::after {
    box-sizing: inherit; /* 상속받도록 설정 */
}
</style>

<style scoped>
/* Google Fonts - Inter (or Pretendard if available via local import) */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

.mypage-view-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 20px 40px 20px;
  background-color: #f7f3ff; /* 연한 보라색 배경 */
  width: 100%;
  box-sizing: border-box;
  font-family: 'Inter', 'Pretendard', sans-serif;
  color: #333;
  height: 100vh; /* 뷰포트 전체 높이를 차지 */
  overflow-y: hidden; /* **마이페이지 전체 세로 스크롤 제거** */
  position: relative;
  z-index: 1;
}

.mypage-view-wrapper::after {
  content: '';
  position: absolute;
  right: 4vw;
  bottom: 9vh;
  width: 50vw;
  height: 50vw;
  max-width: 700px;
  max-height: 700px;
  background-image: url('@/assets/mogwi-look.png');
  background-repeat: no-repeat;
  background-size: contain;
  background-position: right bottom;
  opacity: 0.18;
  pointer-events: none;
  z-index: 0;
}

/* 컴포넌트들이 배경 위에 오도록 z-index 1 부여 */
.mypage-layout,
.logged-out-prompt,
.alert-message,
.LoginModal,
.RegisterModal,
.EditProfileModal,
.DeleteAccountModal {
  position: relative;
  z-index: 1;
}

.mypage-layout {
  display: flex;
  width: 100%;
  max-width: 1630px;
  background: transparent;
  border-radius: 12px;
  box-shadow: none;
  border: none;
  align-items: flex-start;
  margin-left: auto;
  margin-right: auto;
  height: 100%;
  margin-top: 60px;
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
  flex: 0 0 300px;
  background-color: transparent;
  padding: 10px 20px 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 10px;
  margin-left: 60px;
  height: auto;
  position: sticky;
  top: 40px;
  overflow-y: visible;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* Chrome, Safari, Opera 숨기기 */
.sidebar::-webkit-scrollbar {
    display: none;
}

.sidebar-title {
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
  padding-bottom: 40px;
  height: 100%;
  position: relative;
}

.sidebar-nav::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  width: 0;
  height: calc(100% + 400px); /* 메뉴 높이 + 매우 긴 연장 길이 */
  border-right: 1px solid #efdfff;
  pointer-events: none;
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
  padding: 0 60px 60px 24px;
  background-color: transparent;
  margin-top: 20px;
  /* 뷰포트 높이 기준으로 정확히 계산하여 넘치지 않도록 함 */
  height: calc(100vh - 20px - 20px); /* mypage-view-wrapper의 상하 패딩 (20px) 및 main-content의 상단 마진 (20px) 고려 */
  overflow-y: auto 제거로 내부 스크롤도 제거 */
}

.content-section {
  background: none;
  border-radius: 0;
  padding: 0;
  margin-bottom: 40px;
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
  /* logged-out-prompt가 뷰포트를 넘어가지 않도록 최대 높이 설정 */
  max-height: calc(100vh - 40px - 80px); /* mypage-view-wrapper 패딩 및 상단 마진 고려 */
  overflow-y: auto; /* 내부 콘텐츠가 넘칠 경우 스크롤 허용 */
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
    height: auto; /* 모바일에서는 높이 자동 조절 */
    margin-top: 0;
  }

  .sidebar {
    flex: none;
    width: 100%;
    /* border-right: none; */
    border-bottom: 1px solid #efdfff;
    padding: 10px 20px 40px 20px;
    margin-top: 0;
    margin-left: 0;
    height: auto;
    position: static;
    overflow-y: visible;
  }

  .sidebar-nav {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    padding-bottom: 0;
  }

  .sidebar-nav::after {
    display: none;
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
    margin-top: 30px; /* 모바일에서 섹션을 조금 더 아래로 */
    height: auto; /* 모바일에서 높이 자동 조절 */
    overflow-y: visible; /* 모바일에서는 스크롤 숨기지 않음 */
  }

  .page-title {
    font-size: 2rem;
    text-align: center;
    margin-bottom: 25px;
  }
}

@media (max-width: 576px) {
  .mypage-view-wrapper {
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
    padding: 0 10px !important;
    width: 100% !important;
    margin-top: 0;
    height: 100vh;
    box-sizing: border-box;
    overflow-y: auto;
  }

  .content-section {
    margin: 0 auto;
    width: 100%;
    max-width: 480px;
    float: none;
    display: block;
    box-shadow: none !important;
    border: none !important;
    border-radius: 0 !important;
    background: none !important;
  }

  .user-profile-section,
  .my-problems-section,
  .liked-scrap-section {
    padding: 20px 10px;
    width: 100%;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.03);
    margin-left: 0 !important;
    margin-right: 0 !important;
    height: 100%;
    overflow-y: visible;
  }

  .tab-content {
    max-height: calc(100vh - 120px);
    overflow-y: auto;
    height: 100%;
  }
  .problem-list {
    overflow-y: auto;
    max-height: 60vh;
    padding-bottom: 10px;
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

@media (max-width: 900px) {
  .main-content {
    padding-left: 0 !important;
    padding-right: 0 !important;
  }
  .user-profile-section {
    margin-left: 128px !important;
    width: calc(100% - 128px) !important;
    align-items: flex-start;
  }
}

.content-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.content-wrapper.blurred > .content-section {
  filter: blur(8px);
  pointer-events: none;
  user-select: none;
}

.content-login-prompt {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(2px);
  border-radius: 12px;
}

.prompt-mogwi-character {
  width: 130px;
  margin-bottom: 20px;
  opacity: 0.9;
}

.prompt-message {
  font-size: 1.2rem;
  font-weight: 600;
  color: #4a1e77;
  margin-bottom: 25px;
  text-align: center;
  line-height: 1.5;
}

.prompt-actions {
  display: flex;
  gap: 15px;
}

.action-button {
  padding: 8px 20px;
  font-size: 0.9rem;
  font-weight: 700;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border: none;
  min-width: 120px;
}
.login-button {
  background-color: #8c5dff;
  color: white;
}
.login-button:hover {
  background-color: #794cff;
}
.register-button {
  background-color: #ffffff;
  color: #5a2e87;
  border: 2px solid #a471ff;
}
.register-button:hover {
  background-color: #f0e6ff;
}
</style>