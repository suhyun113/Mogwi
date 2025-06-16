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
    <div v-else class="mypage-container">
      <h1 class="page-title">내 정보</h1>

      <UserProfile
        :nickname="userNickname"
        @update-nickname="handleNicknameUpdate"
        @edit-info="handleEditInfo"
      />

      <LikedScrapSection
        :likedProblems="likedProblems"
        :scrapedProblems="scrapedProblems"
        @go-to-problem="goToProblem"
      />

      <MyProblemSection
        :myProblems="myProblems"
        @go-to-problem="goToProblem"
        @edit-problem="editProblem"
        @delete-problem="deleteProblem"
      />
    </div>

    <LoginModal v-if="showLoginModal" @close="showLoginModal = false" @open-register="openRegisterModal" />
    <RegisterModal v-if="showRegisterModal" @close="showRegisterModal = false" @open-login="openLoginModal" />

    <div v-if="showNicknameUpdateMessage" :class="['alert-message', nicknameUpdateStatus]">
      {{ nicknameUpdateMessage }}
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
import LoginModal from '@/components/Login/LoginModal.vue'; // 기존 로그인 모달 재활용
import RegisterModal from '@/components/Register/RegisterModal.vue'; // 기존 회원가입 모달 재활용

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

    // 사용자 데이터
    const userNickname = ref('');
    const likedProblems = ref([]);
    const scrapedProblems = ref([]);
    const myProblems = ref([]);

    // 모달 관련
    const showLoginModal = ref(false);
    const showRegisterModal = ref(false);

    // 닉네임 업데이트 알림
    const showNicknameUpdateMessage = ref(false);
    const nicknameUpdateMessage = ref('');
    const nicknameUpdateStatus = ref(''); // 'success' or 'error'

    // 데이터 불러오기
    const fetchMypageData = async () => {
      if (!isLoggedIn.value) {
        loading.value = false;
        return;
      }

      loading.value = true;
      error.value = null;

      try {
        // 1. 사용자 닉네임 및 기본 정보
        const userResponse = await axios.get(`/api/user/${currentUserId.value}`); // 예시 API 경로
        userNickname.value = userResponse.data.nickname;

        // 2. 좋아요 및 스크랩한 문제 목록
        const likedScrapResponse = await axios.get(`/api/mypage/problems/liked-scraped/${currentUserId.value}`); // 예시 API 경로
        likedProblems.value = likedScrapResponse.data.likedProblems;
        scrapedProblems.value = likedScrapResponse.data.scrapedProblems;

        // 3. 내가 만든 문제 목록
        const myProblemsResponse = await axios.get(`/api/mypage/problems/my-created/${currentUserId.value}`); // 예시 API 경로
        myProblems.value = myProblemsResponse.data.myProblems;

      } catch (err) {
        console.error('마이페이지 데이터 불러오기 실패:', err);
        error.value = '마이페이지 데이터를 불러오는 데 실패했습니다.';
      } finally {
        loading.value = false;
      }
    };

    // 닉네임 업데이트 핸들러
    const handleNicknameUpdate = async (newNickname) => {
      if (!isLoggedIn.value) {
        // 로그인되지 않았다면 처리할 수 없음
        return;
      }
      try {
        await axios.put(`/api/user/${currentUserId.value}/nickname`, { nickname: newNickname }); // 예시 API 경로
        userNickname.value = newNickname; // 성공 시 닉네임 업데이트
        nicknameUpdateMessage.value = '닉네임이 성공적으로 변경되었습니다.';
        nicknameUpdateStatus.value = 'success';
      } catch (err) {
        console.error('닉네임 업데이트 실패:', err);
        nicknameUpdateMessage.value = '닉네임 변경에 실패했습니다. 다시 시도해주세요.';
        nicknameUpdateStatus.value = 'error';
      } finally {
        showNicknameUpdateMessage.value = true;
        setTimeout(() => {
          showNicknameUpdateMessage.value = false;
        }, 3000); // 3초 후 메시지 사라짐
      }
    };

    // 정보 수정 버튼 클릭 핸들러 (실제로는 별도 모달이나 페이지로 이동할 수 있음)
    const handleEditInfo = () => {
      alert('정보 수정 페이지로 이동합니다. (구현 필요)');
      // 예: router.push('/settings/profile');
    };

    // 문제 상세 페이지로 이동
    const goToProblem = (problemId) => {
      alert(`문제 ID ${problemId} 상세 페이지로 이동 (구현 필요)`);
      // 예: router.push(`/problem/${problemId}`);
    };

    // 내가 만든 문제 수정 (모달 또는 페이지 이동)
    const editProblem = (problemId) => {
      alert(`문제 ID ${problemId} 수정 페이지/모달 열기 (구현 필요)`);
      // 예: router.push(`/problem/edit/${problemId}`);
    };

    // 내가 만든 문제 삭제
    const deleteProblem = async (problemId) => {
      if (!confirm('정말로 이 문제를 삭제하시겠습니까?')) {
        return;
      }
      try {
        await axios.delete(`/api/problem/${problemId}`); // 예시 API 경로
        myProblems.value = myProblems.value.filter(p => p.id !== problemId);
        alert('문제가 성공적으로 삭제되었습니다.');
      } catch (err) {
        console.error('문제 삭제 실패:', err);
        alert('문제 삭제에 실패했습니다. 다시 시도해주세요.');
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

    // 초기 데이터 로드
    onMounted(() => {
      fetchMypageData();
    });

    // 로그인 상태 변경 감지하여 데이터 다시 불러오기
    watch(isLoggedIn, (newVal, oldVal) => {
      if (newVal !== oldVal) {
        if (newVal) {
          fetchMypageData();
        } else {
          // 로그아웃 시 데이터 초기화
          userNickname.value = '';
          likedProblems.value = [];
          scrapedProblems.value = [];
          myProblems.value = [];
          loading.value = false;
          error.value = null;
        }
      }
    }, { immediate: true }); // 컴포넌트 초기 렌더링 시에도 실행

    return {
      loading,
      error,
      isLoggedIn,
      userNickname,
      likedProblems,
      scrapedProblems,
      myProblems,
      handleNicknameUpdate,
      handleEditInfo,
      goToProblem,
      editProblem,
      deleteProblem,
      showLoginModal,
      showRegisterModal,
      openLoginModal,
      openRegisterModal,
      showNicknameUpdateMessage,
      nicknameUpdateMessage,
      nicknameUpdateStatus,
    };
  },
};
</script>

<style scoped>
.mypage-view {
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

.mypage-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  padding: 40px;
  width: 100%;
  max-width: 900px;
  margin-top: 20px;
  border: 1px solid #e0d0ff;
  display: flex;
  flex-direction: column;
  gap: 30px; /* 섹션 간 간격 */
}

.loading-message, .error-message {
  color: #6c757d;
  font-size: 1.1rem;
  margin-top: 30px;
  text-align: center;
}

.page-title {
  color: #4a1e77;
  font-size: 2.8rem;
  font-weight: 700;
  margin-bottom: 30px;
  text-align: center;
  border-bottom: 3px solid #f0e6ff;
  padding-bottom: 20px;
}

/* 로그인되지 않았을 때 프롬프트 (ReportView와 유사) */
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

.login-button {
  background-image: linear-gradient(to right, #8c5dff 0%, #a471ff 100%);
  color: white;
  border: none;
  padding: 15px 35px;
  border-radius: 10px;
  font-size: 1.25rem;
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
  background-color: #4CAF50; /* Green */
}

.alert-message.error {
  background-color: #F44336; /* Red */
}

@keyframes fadeInOut {
  0% { opacity: 0; transform: translateX(-50%) translateY(20px); }
  10% { opacity: 1; transform: translateX(-50%) translateY(0); }
  90% { opacity: 1; transform: translateX(-50%) translateY(0); }
  100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
}

@media (max-width: 768px) {
  .mypage-container {
    padding: 20px;
    gap: 20px;
  }
  .page-title {
    font-size: 2rem;
    margin-bottom: 20px;
    padding-bottom: 15px;
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
}
</style>