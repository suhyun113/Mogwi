<script>
import { ref, computed, onMounted } from 'vue';
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

        const loading = ref(true);
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
            loading.value = true;
            error.value = null;

            if (!isLoggedIn.value) {
                console.warn("User not logged in. Cannot fetch my study data.");
                loading.value = false;
                return;
            }

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
                    // 임시 패치: authorId가 없으면, authorNickname이 내 닉네임과 같을 때 currentUserId로 세팅
                    let computedAuthorId = problem.authorId;
                    if (computedAuthorId === undefined || computedAuthorId === '' || computedAuthorId === null) {
                        if (problem.authorNickname && problem.authorNickname === username.value) {
                            computedAuthorId = currentUserId.value;
                        } else {
                            computedAuthorId = '';
                        }
                    }
                    return {
                        ...problem,
                        isLiked: problem.isLiked,
                        isScrapped: problem.isScrapped,
                        authorNickname: problem.authorNickname || '알 수 없음',
                        categories: Array.isArray(problem.categories) ? problem.categories : [],
                        id: problem.id || `temp-${Math.random().toString(36).substr(2, 9)}`,
                        authorId: computedAuthorId, // authorId를 위에서 계산한 값으로 세팅
                        perfectCount: problem.perfectCount || 0,
                        vagueCount: problem.vagueCount || 0,
                        forgottenCount: problem.forgottenCount || 0,
                        cardCount: problem.cardCount || 0, // cardCount도 추가 (ProblemListItem에서 사용)
                    };
                }).filter(problem => problem !== null);

                console.log("MyStudy: Processed allUserProblems (with categories and card counts):", allUserProblems.value);

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
            router.push({ name: 'SolveView', params: { problemId: problemId } });
        };

        onMounted(() => {
            fetchMyStudyData();
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
            fetchMyStudyData, // ProblemListSection에서 재호출할 수 있도록 노출
        };
    }
};
</script>

<template>
    <div class="mystudy">
        <h1 class="page-title">
            {{ isLoggedIn ? `${username}님의 학습 페이지` : '나의 학습 페이지' }}
        </h1>

        <div v-if="loading && isLoggedIn" class="loading-message">데이터를 불러오는 중입니다...</div>
        <div v-else-if="error && isLoggedIn" class="error-message">{{ error }}</div>
        <div v-else-if="!isLoggedIn" class="not-logged-in-message">
            <p>로그인이 필요한 서비스입니다.</p>
            <p>로그인하시면 학습 현황을 확인하실 수 있습니다.</p>
        </div>

        <div v-else>
            <OverallStudySummary
                :overallPerfectCount="overallPerfectCount"
                :overallVagueCount="overallVagueCount"
                :overallForgottenCount="overallForgottenCount"
                :overallTotalCards="overallTotalCards"
            />

            <ProblemListSection
                :ongoingProblems="ongoingProblems"
                :completedProblems="completedProblems"
                :isLoggedIn="isLoggedIn"
                :currentUserId="currentUserId"
                @go-to-study="goToStudy"
                @auth-required="$emit('auth-required')"
                @refresh-problems="fetchMyStudyData" />
        </div>
    </div>
</template>

<style scoped>
.mystudy {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40px 20px;
    background-color: #fdf8f4;
    min-height: 100vh;
    font-family: 'Pretendard', sans-serif;
}

.page-title {
    color: #5a2e87;
    font-size: 2.2rem;
    font-weight: 700;
    margin-bottom: 40px;
    text-align: center;
    padding: 10px 20px;
    background-color: #e6d6ff;
    border-radius: 12px;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.loading-message, .error-message, .no-data-message {
    color: #6c757d;
    font-size: 1.1rem;
    margin-top: 30px;
    text-align: center;
}

.not-logged-in-message {
    text-align: center;
    margin-top: 50px;
    padding: 30px;
    background-color: #fff3cd;
    border: 1px solid #ffeeba;
    border-radius: 10px;
    color: #856404;
    font-size: 1.1rem;
    max-width: 600px;
    margin-left: auto;
    margin-right: auto;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}
.not-logged-in-message p {
    margin-bottom: 15px;
}
</style>