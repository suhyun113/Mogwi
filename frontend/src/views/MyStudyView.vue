<script>
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import axios from 'axios';

// Correct imports for components using export default
import OverallStudySummary from '@/components/MyStudy/OverallStudySummary.vue';
import ProblemListSection from '@/components/MyStudy/ProblemListSection.vue';

export default {
    /* eslint-disable */ // Add this line at the very top of the script section to disable all ESLint warnings/errors in this file.
    name: 'MyStudy',

    // Register components here
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

        // Overall Study Summary Data
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

                // **여기가 핵심입니다.**
                // 백엔드에서 받은 `categories` 데이터가 배열인지 확인하고,
                // 만약 `null`이나 `undefined`이면 빈 배열로 초기화하여 `v-for`가 오류 없이 동작하게 합니다.
                allUserProblems.value = problemsResponse.data.map(problem => ({
                    ...problem,
                    isLiked: problem.isLiked,
                    isScrapped: problem.isScrapped,
                    authorNickname: problem.authorNickname || '알 수 없음',
                    // `categories` 필드가 존재하지 않거나 null일 경우를 대비하여 빈 배열로 설정
                    // 백엔드에서 이 필드가 올바른 문자열 배열로 전달되는지 확인해야 합니다.
                    categories: Array.isArray(problem.categories) ? problem.categories : [],
                }));

                // **디버깅을 위해 추가: `allUserProblems`의 `categories` 내용 확인**
                console.log("MyStudy: Processed allUserProblems (with categories check):", allUserProblems.value);

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

        // ProblemListItem에서 이미 toggleLike와 toggleScrap을 자체적으로 처리하고
        // 'update-problem-data' 이벤트를 통해 상태를 동기화하므로,
        // MyStudy.vue에서 직접 좋아요/스크랩 토글 로직을 가질 필요는 없습니다.
        // ProblemListSection에서 ProblemListItem의 'update-problem-data'를
        // `handleUpdateProblemData`를 통해 처리하고 있으므로,
        // MyStudy.vue에서는 이 이벤트를 별도로 받을 필요가 없습니다.
        // 따라서 MyStudy.vue의 `@toggle-like`와 `@toggle-scrap` 이벤트 리스너는 제거합니다.

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
            // toggleLike, // ProblemListItem에서 직접 처리하므로 MyStudy에서는 제거
            // toggleScrap // ProblemListItem에서 직접 처리하므로 MyStudy에서는 제거
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
                @auth-required="$emit('auth-required')" @update-problem-data="handleUpdateProblemData" />
        </div>
    </div>
</template>

<style scoped>
.mystudy {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40px 20px;
    background-color: #fdf8f4; /* Light background */
    min-height: 100vh;
    font-family: 'Pretendard', sans-serif; /* Recommended font */
}

.page-title {
    color: #5a2e87; /* Deep purple */
    font-size: 2.2rem;
    font-weight: 700;
    margin-bottom: 40px;
    text-align: center;
    padding: 10px 20px;
    background-color: #e6d6ff; /* Lighter purple background */
    border-radius: 12px;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.loading-message, .error-message, .no-data-message {
    color: #6c757d;
    font-size: 1.1rem;
    margin-top: 30px;
    text-align: center;
}

/* 추가: 로그인하지 않았을 때 메시지 스타일 */
.not-logged-in-message {
    text-align: center;
    margin-top: 50px;
    padding: 30px;
    background-color: #fff3cd; /* Light yellow background */
    border: 1px solid #ffeeba;
    border-radius: 10px;
    color: #856404; /* Dark yellow text */
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