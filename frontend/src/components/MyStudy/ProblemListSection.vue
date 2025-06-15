<template>
    <section class="problems-list-section">
        <h2>학습 문제 목록</h2>

        <ProblemTabs
            v-if="showTabs"
            :ongoingCount="ongoingProblems.length"
            :completedCount="completedProblems.length"
            :activeTab="activeTab"
            @change-tab="changeTab"
        />

        <div class="problem-lists-container" :class="{ 'single-column-layout': showTabs }">
            <div
                class="problem-list-column ongoing-column"
                v-show="!showTabs || activeTab === 'ongoing'"
            >
                <h3>현재 진행 중인 학습 ({{ ongoingProblems.length }}개)</h3>
                <ul v-if="ongoingProblems.length > 0" class="problem-list">
                    <ProblemListItem
                        v-for="problem in ongoingProblems"
                        :key="problem.id"
                        :problem="problem"
                        :isAuthenticated="isLoggedIn"
                        :currentUserId="currentUserId"
                        @go-to-study="handleGoToStudy"
                        @auth-required="handleAuthRequired"
                        @problem-action-success="handleProblemActionSuccess" />
                </ul>
                <p v-else class="no-data-message">현재 진행 중인 학습이 없습니다.</p>
            </div>

            <div
                class="problem-list-column completed-column"
                v-show="!showTabs || activeTab === 'completed'"
            >
                <h3>완료한 학습 ({{ completedProblems.length }}개)</h3>
                <ul v-if="completedProblems.length > 0" class="problem-list">
                    <ProblemListItem
                        v-for="problem in completedProblems"
                        :key="problem.id"
                        :problem="problem"
                        :isAuthenticated="isLoggedIn"
                        :currentUserId="currentUserId"
                        @go-to-study="handleGoToStudy"
                        @auth-required="handleAuthRequired"
                        @problem-action-success="handleProblemActionSuccess" />
                </ul>
                <p v-else class="no-data-message">아직 완료한 학습이 없습니다.</p>
            </div>
        </div>
    </section>
</template>

<script>
import { ref, computed } from 'vue';
import ProblemTabs from './ProblemTabs.vue';
import ProblemListItem from './ProblemListItem.vue';

export default {
    name: 'ProblemListSection',
    components: {
        ProblemTabs,
        ProblemListItem
    },
    props: {
        ongoingProblems: {
            type: Array,
            default: () => []
        },
        completedProblems: {
            type: Array,
            default: () => []
        },
        isLoggedIn: {
            type: Boolean,
            default: false
        },
        currentUserId: {
            type: String,
            default: ''
        }
    },
    setup(props, { emit }) {
        const activeTab = ref('ongoing');

        const showTabs = computed(() => {
            return window.innerWidth <= 768;
        });

        const changeTab = (tab) => {
            activeTab.value = tab;
        };

        const handleGoToStudy = (problemId) => {
            emit('go-to-study', problemId);
        };

        const handleAuthRequired = () => {
            emit('auth-required');
        };

        // 좋아요/스크랩 등의 액션 성공 시 MyStudy.vue에 알림
        const handleProblemActionSuccess = () => {
            emit('refresh-problems'); // 새로운 이벤트 발생
        };

        return {
            activeTab,
            showTabs,
            changeTab,
            handleGoToStudy,
            handleAuthRequired,
            handleProblemActionSuccess // 리턴에 추가
        };
    }
};
</script>

<style scoped>
.problems-list-section {
    width: 100%;
    max-width: 1200px;
    background-color: #ffffff;
    border-radius: 15px;
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    padding: 30px;
    border: 1px solid #e0d0ff;
    margin-top: 40px; /* OverallStudySummary와의 간격 */
}

.problems-list-section h2 {
    color: #5a2e87;
    font-size: 1.8rem;
    font-weight: 600;
    margin-bottom: 25px;
    border-bottom: 2px solid #a471ff;
    padding-bottom: 10px;
    text-align: center;
}

/* 가로 배치를 위한 컨테이너 */
.problem-lists-container {
    display: flex;
    justify-content: center;
    gap: 30px;
    flex-wrap: nowrap; /* 기본적으로 줄바꿈 방지 (큰 화면) */
}

/* 작은 화면일 때 컬럼을 하나로 만들고 줄바꿈 허용 */
.problem-lists-container.single-column-layout {
    flex-direction: column;
    gap: 0; /* 컬럼 간 기본 간격 제거 */
    justify-content: flex-start; /* 작은 화면에서는 중앙 정렬 필요 없음 */
}

.problem-list-column {
    flex: 1;
    min-width: 350px; /* 최소 너비 유지 */
}

.problem-lists-container.single-column-layout .problem-list-column {
    min-width: unset; /* 단일 컬럼일 때 최소 너비 제한 해제 */
    width: 100%; /* 전체 너비 사용 */
}

.problems-list-section h3 {
    color: #7a4cb8;
    font-size: 1.3rem;
    font-weight: 600;
    margin-top: 0;
    margin-bottom: 20px;
    padding-left: 10px;
    border-left: 5px solid #a471ff;
}

/* 작은 화면일 때 h3 마진 조정 */
.problem-lists-container.single-column-layout .problem-list-column:first-of-type h3 {
    margin-top: 0; /* 첫 번째 컬럼의 h3 상단 마진 제거 */
}

.problems-list-container.single-column-layout .problem-list-column:last-of-type h3 {
    margin-top: 30px; /* 두 번째 컬럼의 h3 상단 마진 추가 (탭이 보일 때) */
}

.problem-list {
    list-style: none;
    padding: 0;
    margin: 0;
}

.no-data-message {
    text-align: center;
    color: #888;
    margin-top: 20px;
    font-size: 1.1rem;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
    .problems-list-section {
        padding: 20px; /* Smaller padding on mobile */
        margin-top: 20px;
    }
    .problems-list-section h2 {
        font-size: 1.5rem;
        margin-bottom: 20px;
    }
    .problem-lists-container {
        flex-direction: column; /* 세로로 다시 배치 */
        gap: 0;
        justify-content: flex-start;
    }
    .problem-list-column {
        min-width: unset;
        width: 100%;
    }
    .problems-list-section h3 {
        margin-top: 30px;
    }
    .problems-list-container.single-column-layout .problem-list-column:first-of-type h3 {
        margin-top: 0;
    }
    .problems-list-container.single-column-layout .problem-list-column:last-of-type h3 {
        margin-top: 30px;
    }
}
</style>