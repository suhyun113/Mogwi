<template>
  <div class="mystudy">
    <div class="content-wrapper" :class="{ blurred: !isLoggedIn }">
      <OverallStudySummary
        :overallPerfectCount="overallPerfectCount"
        :overallVagueCount="overallVagueCount"
        :overallForgottenCount="overallForgottenCount"
        :overallTotalCards="overallTotalCards"
        :isLoggedIn="isLoggedIn"
        :username="username"
        class="overall-summary-placement"
      />

      <div v-if="loading && isLoggedIn" class="loading-message">데이터를 불러오는 중입니다...</div>
      <div v-else-if="error && isLoggedIn" class="error-message">{{ error }}</div>

      <ProblemListSection
        :ongoingProblems="ongoingProblems"
        :completedProblems="completedProblems"
        :isLoggedIn="isLoggedIn"
        :currentUserId="currentUserId"
        :username="username"
        @go-to-study="goToStudy"
        @auth-required="handleAuthRequired"
        @refresh-problems="fetchMyStudyData"
      />
    </div>

    <div v-if="!isLoggedIn" class="login-overlay">
      <div class="overlay-content">
        <img src="@/assets/mogwi-look.png" alt="모귀 캐릭터" class="overlay-mogwi" />
        <h2 class="overlay-title">로그인하고 나만의 학습 공간을 확인해보세요!</h2>
        <p class="overlay-description">학습 현황 분석, 문제 기록, 복습 등 모든 기능을 사용할 수 있어요.</p>
        <div class="overlay-actions">
          <button @click="goToLogin" class="action-button login-button">로그인</button>
          <button @click="goToRegister" class="action-button register-button">회원가입</button>
        </div>
      </div>
    </div>

    <LoginModal
      v-if="showLoginModal"
      @close="showLoginModal = false"
      @login-success="handleLoginSuccess"
      @open-register="handleOpenRegister"
    />
    <RegisterModal
      v-if="showRegisterModal"
      @close="showRegisterModal = false"
      @registration-success="handleRegistrationSuccess"
      @open-login="handleOpenLogin"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import axios from 'axios';

import OverallStudySummary from '@/components/MyStudy/OverallStudySummary.vue';
import ProblemListSection from '@/components/MyStudy/ProblemListSection.vue';
import LoginModal from '@/components/Login/LoginModal.vue';
import RegisterModal from '@/components/Register/RegisterModal.vue';


export default {
    name: 'MyStudy',
    components: {
        OverallStudySummary,
        ProblemListSection,
        LoginModal,
        RegisterModal,
    },
    setup() {
        const store = useStore();
        const router = useRouter();

        const username = computed(() => store.state.store_username);
        const currentUserId = computed(() => store.state.store_userid);
        const isLoggedIn = computed(() => !!currentUserId.value);

        const loading = ref(false);
        const error = ref(null);

        const showLoginModal = ref(false);
        const showRegisterModal = ref(false);

        const overallPerfectCount = ref(0);
        const overallVagueCount = ref(0);
        const overallForgottenCount = ref(0);
        const overallTotalCards = ref(0);

        const allUserProblems = ref([]);
        const ongoingProblems = computed(() =>
            allUserProblems.value.filter(p => p.studyStatus === 'ongoing' || p.studyStatus === 'new')
        );
        const completedProblems = computed(() =>
            allUserProblems.value.filter(p => p.studyStatus === 'completed')
        );

        const fetchMyStudyData = async () => {
            if (!isLoggedIn.value) {
                console.log("User not logged in. Not fetching study data.");
                overallPerfectCount.value = 0;
                overallVagueCount.value = 0;
                overallForgottenCount.value = 0;
                overallTotalCards.value = 0;
                allUserProblems.value = [];
                loading.value = false;
                error.value = null;
                return;
            }

            loading.value = true;
            error.value = null;

            try {
                console.log(`Fetching study summary for userId: ${currentUserId.value}`);
                const summaryResponse = await axios.get(`/api/mystudy/problem/${currentUserId.value}`);
                const summary = summaryResponse.data;
                overallPerfectCount.value = summary.perfect || 0;
                overallVagueCount.value = summary.vague || 0;
                overallForgottenCount.value = summary.forgotten || 0;
                overallTotalCards.value = summary.total || 0;

                console.log(`Fetching detailed problems for userId: ${currentUserId.value}`);
                const problemsResponse = await axios.get(`/api/problem/detail?currentUserId=${currentUserId.value}`);

                if (!problemsResponse.data || !Array.isArray(problemsResponse.data)) {
                    console.warn("API 응답 데이터가 유효한 배열이 아닙니다.", problemsResponse.data);
                    allUserProblems.value = [];
                    return;
                }

                allUserProblems.value = problemsResponse.data.map(problem => {
                    if (!problem) {
                        console.warn("Received null or undefined problem in the list, skipping.", problem);
                        return null;
                    }
                    // 임시 패치: authorId가 없으면, authorNickname이 내 닉네임과 같을 때 currentUserId로 세팅
                    let computedAuthorId = problem.authorId;
                    if (computedAuthorId === undefined || computedAuthorId === '' || computedAuthorId === null) {
                        if (problem.authorNickname && problem.authorNickname === username.value) {
                            computedAuthorId = currentUserId.value;
                        } else {
                            computedAuthorId = '';
                        }
                    }

                    const sortedCategories = Array.isArray(problem.categories)
                        ? problem.categories.sort((a, b) => a.id - b.id)
                        : [];

                    return {
                        ...problem,
                        isLiked: !!problem.isLiked,
                        isScrapped: !!problem.isScrapped,
                        authorNickname: problem.authorNickname || '알 수 없음',
                        categories: sortedCategories,
                        id: problem.id || `temp-${Math.random().toString(36).substr(2, 9)}`,
                        authorId: computedAuthorId, // authorId를 위에서 계산한 값으로 세팅
                        perfectCount: problem.perfectCount || 0,
                        vagueCount: problem.vagueCount || 0,
                        forgottenCount: problem.forgottenCount || 0,
                        cardCount: problem.cardCount || 0, // cardCount도 추가 (ProblemListItem에서 사용)
                    };
                }).filter(problem => problem !== null);

                console.log("MyStudy: Processed allUserProblems:", allUserProblems.value);

            } catch (err) {
                console.error("나의 학습 데이터 불러오기 실패:", err);
                if (err.response) {
                    const status = err.response.status;
                    const message = err.response.data.message || err.message;
                    if (status === 400) {
                        error.value = `요청 오류: ${message}. 사용자 ID 형식을 확인해주세요.`;
                    } else if (status === 404) {
                        error.value = `데이터를 찾을 수 없습니다: ${message}. 로그인 상태를 확인하거나, 아직 학습한 문제가 없을 수 있습니다.`;
                    } else if (status === 500) {
                        error.value = `서버 내부 오류: ${message}. 잠시 후 다시 시도해주세요.`;
                    } else {
                        error.value = `데이터 불러오기 실패 (${status}): ${message}`;
                    }
                } else if (err.request) {
                    error.value = "네트워크 오류: 서버에 연결할 수 없습니다. 인터넷 연결을 확인해주세요.";
                } else {
                    error.value = `알 수 없는 오류 발생: ${err.message}`;
                }
            } finally {
                loading.value = false;
            }
        };

        const sortProblems = (problems) => {
            // Assuming 'id' is a unique and stable identifier for each problem
            // You could also use a 'createdAt' timestamp if available and reliable
            return [...problems].sort((a, b) => {
                // Ensure both a.id and b.id are comparable (e.g., numbers or strings)
                if (typeof a.id === 'number' && typeof b.id === 'number') {
                    return a.id - b.id;
                }
                // Fallback for strings or other types if 'id' isn't purely numeric
                return String(a.id).localeCompare(String(b.id));
            });
        };

        const goToStudy = (problemId) => {
            router.push({ name: 'SolveView', params: { id: problemId } });
        };

        const handleAuthRequired = () => {
            alert("로그인이 필요한 서비스입니다.");
            showLoginModal.value = true;
        };

        const goToLogin = () => {
            showLoginModal.value = true;
        };

        const goToRegister = () => {
            showRegisterModal.value = true;
        };

        const handleLoginSuccess = () => {
            showLoginModal.value = false;
            fetchMyStudyData();
        };

        const handleRegistrationSuccess = () => {
            showRegisterModal.value = false;
            alert("회원가입이 완료되었습니다. 로그인 해주세요!");
            showLoginModal.value = true;
        };

        const handleOpenRegister = () => {
            showLoginModal.value = false;
            showRegisterModal.value = true;
        };

        const handleOpenLogin = () => {
            showRegisterModal.value = false;
            showLoginModal.value = true;
        };


        onMounted(() => {
            fetchMyStudyData();
        });

        watch(isLoggedIn, (newVal, oldVal) => {
            if (newVal !== oldVal) {
                fetchMyStudyData();
            }
        });

        return {
            username,
            currentUserId,
            isLoggedIn,
            loading,
            error,
            overallPerfectCount,
            overallVagueCount,
            overallForgottenCount,
            overallTotalCards,
            ongoingProblems,
            completedProblems,
            goToStudy,
            fetchMyStudyData,
            handleAuthRequired,
            goToLogin,
            goToRegister,
            showLoginModal,
            showRegisterModal,
            handleLoginSuccess,
            handleRegistrationSuccess,
            handleOpenRegister,
            handleOpenLogin,
            sortProblems
        };
    }
};
</script>

<style scoped>
.mystudy {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  background-color: #f7f3ff;
  min-height: 100vh;
  width: 100%;
  font-family: 'Pretendard', sans-serif;
  box-sizing: border-box;
  overflow-x: hidden;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  transition: filter 0.3s ease-in-out, transform 0.3s ease-in-out;
}
.content-wrapper.blurred {
  filter: blur(8px);
  pointer-events: none;
  user-select: none;
  transform: scale(1.01);
}

.login-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  box-sizing: border-box;
}

.overlay-content {
  background-color: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(5px);
  border-radius: 10px;
  padding: 25px 35px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.9);
}

.overlay-mogwi {
  width: 120px;
  margin-bottom: 20px;
}

.overlay-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #4a1e77;
  margin-bottom: 12px;
}

.overlay-description {
  font-size: 0.9rem;
  color: #666;
  margin-bottom: 25px;
  max-width: 400px;
  line-height: 1.6;
}

.overlay-actions {
  display: flex;
  gap: 20px;
}

:deep(html), :deep(body) {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  overflow-x: hidden;
}

.overall-summary-placement {
  z-index: 5;
  margin-top: 100px;
  width: 100%;
  max-width: 800px;
  box-sizing: border-box;
  padding: 0 20px;
  margin-bottom: 5px;
}

.problem-list-section {
  position: relative;
  z-index: 5;
  margin-top: 15px;
  width: 100%;
  max-width: 800px;
  box-sizing: border-box;
  padding: 0 20px;
}

.loading-message, .error-message {
  color: #6c757d;
  font-size: 1.1rem;
  margin-top: 30px;
  text-align: center;
}

.action-button {
  padding: 8px 20px;
  font-size: 0.9rem;
  font-weight: 700;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
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

@media (max-width: 768px) {
  .overall-summary-placement {
    margin-top: 70px;
    padding: 0 10px;
  }

  .problem-list-section {
    margin-top: 30px;
    padding: 0 10px;
  }

  .logged-out-state {
    padding: 30px 20px;
    margin-top: 70px;
  }

  .mogwi-character {
    width: 120px;
    margin-bottom: 25px;
  }

  .logged-out-message {
    font-size: 1.3rem;
    margin-bottom: 30px;
  }

  .logged-out-actions {
    flex-direction: column;
    gap: 15px;
    width: 100%;
  }

  .action-button {
    width: 90%;
    margin: 0 auto;
    font-size: 1.1rem;
    padding: 12px 25px;
  }
}
</style>