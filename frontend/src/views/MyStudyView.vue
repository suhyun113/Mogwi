<template>
    <div class="mystudy">
        <h1 class="page-title">
            {{ isLoggedIn ? `${username}님의 학습 페이지` : '나의 학습 페이지' }}
        </h1>

        <OverallStudySummary
            :overallPerfectCount="overallPerfectCount"
            :overallVagueCount="overallVagueCount"
            :overallForgottenCount="overallForgottenCount"
            :overallTotalCards="overallTotalCards"
            :isLoggedIn="isLoggedIn"
            class="overall-summary-placement"
        />

        <div v-if="loading && isLoggedIn" class="loading-message">데이터를 불러오는 중입니다...</div>
        <div v-else-if="error && isLoggedIn" class="error-message">{{ error }}</div>

        <ProblemListSection
            :ongoingProblems="ongoingProblems"
            :completedProblems="completedProblems"
            :isLoggedIn="isLoggedIn"
            :currentUserId="currentUserId"
            @go-to-study="goToStudy"
            @auth-required="handleAuthRequired"
            @refresh-problems="fetchMyStudyData" />

    </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import axios from 'axios';

import OverallStudySummary from '@/components/MyStudy/OverallStudySummary.vue';
import ProblemListSection from '@/components/MyStudy/ProblemListSection.vue';

export default {
    name: 'MyStudy',
    components: {
        OverallStudySummary,
        ProblemListSection
    },
    setup() {
        const store = useStore();
        const router = useRouter();

        const username = computed(() => store.state.store_username);
        const currentUserId = computed(() => store.state.store_userid);
        const isLoggedIn = computed(() => !!currentUserId.value);

        const loading = ref(false);
        const error = ref(null);

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
                const summaryResponse = await axios.get(`/api/mystudy/summary/${currentUserId.value}`);
                const summary = summaryResponse.data;
                overallPerfectCount.value = summary.perfect || 0;
                overallVagueCount.value = summary.vague || 0;
                overallForgottenCount.value = summary.forgotten || 0;
                overallTotalCards.value = summary.total || 0;

                console.log(`Fetching detailed problems for userId: ${currentUserId.value}`);
                const problemsResponse = await axios.get(`/api/mystudy/problems/detail/${currentUserId.value}`);

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
                    return {
                        ...problem,
                        isLiked: !!problem.isLiked,
                        isScrapped: !!problem.isScrapped,
                        authorNickname: problem.authorNickname || '알 수 없음',
                        categories: Array.isArray(problem.categories) ? problem.categories : [],
                        id: problem.id || `temp-${Math.random().toString(36).substr(2, 9)}`,
                        authorId: problem.authorId,
                        perfectCount: problem.perfectCount || 0,
                        vagueCount: problem.vagueCount || 0,
                        forgottenCount: problem.forgottenCount || 0,
                        cardCount: problem.cardCount || 0,
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

        const goToStudy = (problemId) => {
            router.push({ name: 'SolveView', params: { id: problemId } });
        };

        const handleAuthRequired = () => {
            alert("로그인이 필요한 서비스입니다.");
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
        };
    }
};
</script>

<style scoped>
.mystudy {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40px 20px;
    background-color: #fdf8f4;
    height: 100vh;
    width: 100vw;
    position: fixed;
    top: 0;
    left: 0;
    overflow: hidden;
    font-family: 'Pretendard', sans-serif;
}

/* 전역 스타일 */
:deep(html), :deep(body) {
    margin: 0;
    padding: 0;
    height: 100%;
    width: 100%;
    overflow: hidden;
    position: fixed;
}

/* 페이지 제목: 왼쪽 상단 고정 */
.page-title {
    color: #5a2e87;
    font-size: 2.2rem;
    font-weight: 700;
    background-color: transparent;
    border-radius: 0;
    box-shadow: none;
    padding: 0;
    position: absolute;
    top: 120px;
    left: 40px;
    margin: 0;
    text-align: left;
    width: auto;
    z-index: 10;
}

/* OverallStudySummary 컴포넌트 배치 조정 */
/* OverallStudySummary에 직접 적용할 클래스 */
.overall-summary-placement {
    z-index: 5;
    margin-top: 160px; /* 100px에서 160px로 증가 */
    width: 100%;
    max-width: 800px;
    box-sizing: border-box;
    padding: 0 20px;
    margin-bottom: 15px;
}

/* ProblemListSection 배치 조정 */
.problem-list-section {
    position: relative;
    z-index: 5;
    margin-top: 30px; /* 15px에서 30px로 증가 */
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

/* 반응형 디자인 */
@media (max-width: 768px) {
    .page-title {
        font-size: 1.8rem;
        top: 20px;
        left: 20px;
    }
    .overall-summary-placement {
        margin-top: 70px; /* 모바일에서 제목과의 간격 줄임 */
        padding: 0 10px; /* 모바일에서 좌우 패딩 조정 */
    }
    .problem-list-section {
        margin-top: 30px;
        padding: 0 10px;
    }
}
</style>