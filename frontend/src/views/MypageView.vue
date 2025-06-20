<template>
  <div class="mypage-view-wrapper">
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
          <a href="#" class="nav-item nav-item-danger" @click.prevent="handleDeleteAccount">
            <i class="fas fa-user-times"></i> 회원 탈퇴
          </a>
        </nav>
      </aside>

      <main class="main-content">
        <section v-if="activeSection === 'profile'" class="content-section">
          <UserProfile
            :nickname="userNickname"
            :userEmail="userEmail"
            @edit-info="showEditProfileModal = true"
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

    <EditProfileModal
      v-if="showEditProfileModal"
      :initialNickname="userNickname"
      :initialEmail="userEmail"
      @close="showEditProfileModal = false"
      @update-profile="handleProfileUpdateFromModal"
    />
  </div> </template>

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
import EditProfileModal from '@/components/Mypage/EditProfileModal.vue'; // EditProfileModal import 확인

export default {
  name: 'MypageView',
  components: {
    UserProfile,
    LikedScrapSection,
    MyProblemSection,
    LoginModal,
    RegisterModal,
    EditProfileModal, // EditProfileModal 등록
  },
  setup() {
    const store = useStore();
    const router = useRouter();

    const isLoggedIn = computed(() => !!store.state.store_userid);
    const currentUserId = computed(() => store.state.store_userid);

    const loading = ref(true);
    const error = ref(null);

    const userNickname = ref('');
    const userEmail = ref(''); // 사용자 이메일을 저장할 ref 추가
    const likedProblems = ref([]);
    const scrapedProblems = ref([]); // <-- **FIX: Declare scrapedProblems here**
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
          userEmail.value = userResponse.data.user.usermail; // 이메일 정보도 저장
        } else {
          userNickname.value = '알 수 없음';
          userEmail.value = '알 수 없음';
        }

        const likedScrapResponse = await axios.get(`/api/mypage/problems/liked-scraped/${currentUserId.value}`);
        likedProblems.value = likedScrapResponse.data.likedProblems;
        scrapedProblems.value = likedScrapResponse.data.scrapedProblems; // <-- **FIX: This line is now correct as `scrapedProblems` is defined.**

        const myProblemsResponse = await axios.get(`/api/problem?currentUserId=${currentUserId.value}&onlyMine=true`);
        myProblems.value = myProblemsResponse.data;

      } catch (err) {
        console.error('마이페이지 데이터 불러오기 실패:', err);
        error.value = '마이페이지 데이터를 불러오는 데 실패했습니다.';
      } finally {
        loading.value = false;
      }
    };

    // EditProfileModal에서 프로필 정보가 업데이트되었을 때 호출될 함수
    const handleProfileUpdateFromModal = (updatedData) => {
      // 닉네임과 이메일 정보 업데이트
      if (updatedData.nickname) {
        userNickname.value = updatedData.nickname;
        nicknameUpdateMessage.value = '닉네임이 성공적으로 변경되었습니다.';
        nicknameUpdateStatus.value = 'success';
      }
      if (updatedData.email) {
        userEmail.value = updatedData.email; // 이메일 업데이트
      }

      showNicknameUpdateMessage.value = true;
      setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);

      // 모달 닫기
      showEditProfileModal.value = false;

      // 필요하다면 변경된 정보를 UI에 반영하기 위해 마이페이지 데이터 다시 불러오기
      // fetchMypageData(); // 닉네임과 이메일만 변경된 경우라면 굳이 전체를 다시 불러올 필요는 없을 수 있음
    };


    // --- (이하 기존 코드 유지) ---
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
        nicknameUpdateMessage.value = '회원 탈퇴가 성공적으로 처리되었습니다. (기능 미구현)';
        nicknameUpdateStatus.value = 'success';
        showNicknameUpdateMessage.value = true;
        setTimeout(() => { showNicknameUpdateMessage.value = false; }, 3000);
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
        router.push(`/problem/edit/${problemId}`);
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

    onMounted(() => {
      fetchMypageData();
      document.body.classList.add('hide-vertical-scroll');
    });
    onUnmounted(() => {
      document.body.classList.remove('hide-vertical-scroll');
    });

    watch(isLoggedIn, (newVal, oldVal) => {
      if (newVal !== oldVal) {
        if (newVal) {
          fetchMypageData();
        } else {
          userNickname.value = '';
          userEmail.value = ''; // 로그아웃 시 이메일도 초기화
          likedProblems.value = [];
          scrapedProblems.value = []; // <-- **FIX: Ensure this is reset on logout.**
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
      userEmail, // userEmail도 return에 추가
      likedProblems,
      scrapedProblems, // <-- **FIX: scrapedProblems must be returned from setup.**
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
      handleProfileUpdateFromModal, // 모달 업데이트 핸들러
      goToProblem,
      editProblem,
      deleteProblem,
    };
  },
};
</script>

<style>
/* 전역 스타일: HTML 및 BODY에 적용하여 스크롤바를 없앱니다. */
html, body {
    margin: 0;
    padding: 0;
    height: 100%; /* 뷰포트 높이 전체를 사용 */
    box-sizing: border-box; /* 모든 요소에 적용 (패딩/보더를 너비/높이에 포함) */
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
  padding: 40px 20px;
  background-color: #f7f3ff; /* 연한 보라색 배경 */
  min-height: 100vh; /* 이 부분을 유지하면서 내부 콘텐츠가 넘치지 않게 합니다. */
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
  height: calc(100vh - 80px); /* 뷰포트 높이에서 상하 패딩을 뺀 값으로 설정 */
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
  /* 높이 조정을 통해 부모와 동일하게 채우기 */
  height: 100%;
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
  padding: 0 40px 40px 40px;
  background-color: transparent;
  margin-top: 100px;
  height: calc(100% - 100px);
  overflow-y: hidden !important;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* Chrome, Safari, Opera 숨기기 */
.main-content::-webkit-scrollbar {
    display: none;
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
  height: auto; /* 높이를 콘텐츠에 맞게 조절 */
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
  }

  .sidebar {
    flex: none;
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #efdfff;
    padding: 20px;
    margin-top: 0; /* 모바일에서 마진 제거 */
    margin-left: 0; /* 모바일에서 마진 제거 */
    height: auto; /* 모바일에서 높이 자동 조절 */
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
    margin-top: 0; /* 모바일에서 마진 제거 */
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