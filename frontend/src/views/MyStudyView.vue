<script setup>
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import axios from 'axios';

// assets/icons/card.png 경로를 import 합니다.
import cardIcon from '@/assets/icons/card.png';
// 좋아요 아이콘 경로를 import 합니다.
import likeFilledIcon from '@/assets/icons/like_filled.png';
import likeOutlineIcon from '@/assets/icons/like_outline.png';
// 스크랩 아이콘 경로를 import 합니다.
import scrapFilledIcon from '@/assets/icons/scrap_filled.png';
import scrapOutlineIcon from '@/assets/icons/scrap_outline.png';


const store = useStore();
const router = useRouter();

const username = computed(() => store.state.store_username);
// Crucial: This computed property directly uses store.state.store_userid.
// Ensure store_userid is set correctly (e.g., "확인1", not "1:1") during login.
const currentUserId = computed(() => store.state.store_userid);
const isLoggedIn = computed(() => !!currentUserId.value); // Determine login status based on userId presence

const loading = ref(true);
const error = ref(null);

// Overall Study Summary Data
const overallPerfectCount = ref(0);
const overallVagueCount = ref(0);
const overallForgottenCount = ref(0);
const overallTotalCards = ref(0);

const overallPerfectPercentage = computed(() =>
    overallTotalCards.value > 0 ? ((overallPerfectCount.value / overallTotalCards.value) * 100).toFixed(1) : 0
);
const overallVaguePercentage = computed(() =>
    overallTotalCards.value > 0 ? ((overallVagueCount.value / overallTotalCards.value) * 100).toFixed(1) : 0
);
const overallForgottenPercentage = computed(() =>
    overallTotalCards.value > 0 ? ((overallForgottenCount.value / overallTotalCards.value) * 100).toFixed(1) : 0
);

const allUserProblems = ref([]);
const ongoingProblems = computed(() =>
    // 'new' 상태도 진행 중으로 간주
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
        // error.value = "로그인이 필요한 서비스입니다."; // Displayed by v-else-if="!isLoggedIn"
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
        
        // Backend now returns isLiked, isScrapped (as boolean-like integers), authorNickname, and categories
        allUserProblems.value = problemsResponse.data.map(problem => ({
            ...problem,
            isLiked: problem.isLiked, // Should be 0 or 1, convert to boolean if necessary
            isScrapped: problem.isScrapped, // Should be 0 or 1, convert to boolean if necessary
            authorNickname: problem.authorNickname || '알 수 없음',
            categories: problem.categories || [],
            // ProblemController does not return 'authorId' for this endpoint,
            // so if you need it, add 'u.userid AS author_id' to your backend query.
            // problem.authorId will be undefined if not returned by backend.
        }));

    } catch (err) {
        console.error("나의 학습 데이터 불러오기 실패:", err);
        if (err.response) {
            // Backend responded with a status code that falls out of the range of 2xx
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
            // The request was made but no response was received
            error.value = "네트워크 오류: 서버에 연결할 수 없습니다. 인터넷 연결을 확인해주세요.";
        } else {
            // Something happened in setting up the request that triggered an Error
            error.value = `알 수 없는 오류 발생: ${err.message}`;
        }
    } finally {
        loading.value = false;
    }
};

const goToStudy = (problemId) => {
    router.push({ name: 'SolveView', params: { problemId: problemId } });
};

// 좋아요 상태 토글 함수
const toggleLike = async (problemId) => {
    if (!isLoggedIn.value) {
        alert("로그인 후 이용 가능한 서비스입니다.");
        return;
    }
    try {
        const response = await axios.post(`/api/mystudy/problems/${problemId}/toggle-status`, {
            userId: currentUserId.value,
            field: 'isLiked'
        });
        if (response.data.status === 'OK') {
            const problemIndex = allUserProblems.value.findIndex(p => p.id === problemId);
            if (problemIndex !== -1) {
                allUserProblems.value[problemIndex].isLiked = response.data.newStatus;
                // 좋아요 수 업데이트 (백엔드에서 totalLikes를 같이 보내줄 경우)
                if (response.data.totalLikes !== undefined) {
                    allUserProblems.value[problemIndex].totalLikes = response.data.totalLikes;
                }
            }
        } else {
            alert(`좋아요 상태 변경 실패: ${response.data.message || '알 수 없는 오류'}`);
        }
    } catch (err) {
        console.error(`문제 좋아요 상태 토글 실패 (문제 ID: ${problemId}):`, err);
        if (err.response) {
            alert(`좋아요 상태 변경 중 오류 발생: ${err.response.data.message || err.message}`);
        } else {
            alert('좋아요 상태 변경 중 네트워크 오류가 발생했습니다.');
        }
    }
};

// 스크랩 상태 토글 함수
const toggleScrap = async (problemId) => {
    if (!isLoggedIn.value) {
        alert("로그인 후 이용 가능한 서비스입니다.");
        return;
    }
    try {
        const response = await axios.post(`/api/mystudy/problems/${problemId}/toggle-status`, {
            userId: currentUserId.value,
            field: 'isScrapped'
        });
        if (response.data.status === 'OK') {
            const problemIndex = allUserProblems.value.findIndex(p => p.id === problemId);
            if (problemIndex !== -1) {
                allUserProblems.value[problemIndex].isScrapped = response.data.newStatus;
                // 스크랩 수 업데이트 (백엔드에서 totalScraps를 같이 보내줄 경우)
                if (response.data.totalScraps !== undefined) {
                    allUserProblems.value[problemIndex].totalScraps = response.data.totalScraps;
                }
            }
        } else {
            alert(`스크랩 상태 변경 실패: ${response.data.message || '알 수 없는 오류'}`);
        }
    } catch (err) {
        console.error(`문제 스크랩 상태 토글 실패 (문제 ID: ${problemId}):`, err);
        if (err.response) {
            alert(`스크랩 상태 변경 중 오류 발생: ${err.response.data.message || err.message}`);
        } else {
            alert('스크랩 상태 변경 중 네트워크 오류가 발생했습니다.');
        }
    }
};

const getStatusText = (status) => {
    switch (status) {
        case 'new': return '시작 전';
        case 'ongoing': return '진행 중';
        case 'completed': return '완료';
        default: return '알 수 없음';
    }
};

onMounted(() => {
    fetchMyStudyData();
});

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
            <section class="overall-summary-section">
                <h2>전체 학습 현황</h2>
                <div class="summary-stats">
                    <div class="stat-item perfect">
                        <span class="label">완벽한 기억:</span>
                        <span class="value">{{ overallPerfectCount }}개</span>
                    </div>
                    <div class="stat-item vague">
                        <span class="label">희미한 기억:</span>
                        <span class="value">{{ overallVagueCount }}개</span>
                    </div>
                    <div class="stat-item forgotten">
                        <span class="label">사라진 기억:</span>
                        <span class="value">{{ overallForgottenCount }}개</span>
                    </div>
                    <div class="stat-item total">
                        <span class="label">총 학습 카드:</span>
                        <span class="value">{{ overallTotalCards }}개</span>
                    </div>
                </div>

                <div class="progress-bar-container">
                    <div
                        v-if="overallPerfectPercentage > 0"
                        class="progress-bar perfect"
                        :style="{ width: overallPerfectPercentage + '%' }"
                        title="완벽한 기억"
                    ></div>
                    <div
                        v-if="overallVaguePercentage > 0"
                        class="progress-bar vague"
                        :style="{ width: overallVaguePercentage + '%' }"
                        title="희미한 기억"
                    ></div>
                    <div
                        v-if="overallForgottenPercentage > 0"
                        class="progress-bar forgotten"
                        :style="{ width: overallForgottenPercentage + '%' }"
                        title="사라진 기억"
                    ></div>
                    <div v-if="overallTotalCards === 0" class="progress-bar no-data">학습 데이터 없음</div>
                </div>
                <p class="progress-labels">
                    <span v-if="overallPerfectPercentage > 0" class="label perfect">완벽 {{ overallPerfectPercentage }}%</span>
                    <span v-if="overallVaguePercentage > 0" class="label vague">희미 {{ overallVaguePercentage }}%</span>
                    <span v-if="overallForgottenPercentage > 0" class="label forgotten">사라짐 {{ overallForgottenPercentage }}%</span>
                </p>
            </section>

            <section class="problems-list-section">
                <h2>학습 문제 목록</h2>

                <div class="problem-lists-container">
                    <div class="problem-list-column">
                        <h3>현재 진행 중인 학습 ({{ ongoingProblems.length }}개)</h3>
                        <ul v-if="ongoingProblems.length > 0" class="problem-list">
                            <li v-for="problem in ongoingProblems" :key="problem.id" class="problem-item ongoing">
                                <div class="problem-content">
                                    <div class="problem-header">
                                        <span class="problem-title">{{ problem.title }}</span>
                                        <span v-if="problem.authorNickname" class="problem-author">작성자: {{ problem.authorNickname }}</span>
                                    </div>
                                    <div class="problem-meta">
                                        <span class="card-count-display">
                                            <img :src="cardIcon" alt="카드 아이콘" class="card-icon" />
                                            {{ problem.cardCount }}
                                        </span>
                                        <span class="icon-text like-toggle" :class="{ liked: problem.isLiked }" @click.stop="toggleLike(problem.id)">
                                            <img :src="problem.isLiked ? likeFilledIcon : likeOutlineIcon" alt="좋아요 아이콘" class="icon" />
                                            {{ problem.totalLikes || 0 }}
                                        </span>
                                        <span class="icon-text scrap-toggle" :class="{ scrapped: problem.isScrapped }" @click.stop="toggleScrap(problem.id)">
                                            <img :src="problem.isScrapped ? scrapFilledIcon : scrapOutlineIcon" alt="스크랩 아이콘" class="icon" />
                                            {{ problem.totalScraps || 0 }}
                                        </span>
                                    </div>
                                    <div v-if="problem.categories && problem.categories.length > 0" class="problem-tags">
                                        <span v-for="tag in problem.categories" :key="tag" class="tag">#{{ tag }}</span>
                                    </div>
                                </div>
                                <div class="problem-actions">
                                    <div :class="['problem-status-tag', problem.studyStatus]">
                                        {{ getStatusText(problem.studyStatus) }}
                                    </div>
                                    <button @click="goToStudy(problem.id)" class="solve-button">문제 풀기</button>
                                </div>
                            </li>
                        </ul>
                        <p v-else class="no-data-message">현재 진행 중인 학습이 없습니다.</p>
                    </div>

                    <div class="problem-list-column">
                        <h3>완료한 학습 ({{ completedProblems.length }}개)</h3>
                        <ul v-if="completedProblems.length > 0" class="problem-list">
                            <li v-for="problem in completedProblems" :key="problem.id" class="problem-item completed">
                                <div class="problem-content">
                                    <div class="problem-header">
                                        <span class="problem-title">{{ problem.title }}</span>
                                        <span v-if="problem.authorNickname" class="problem-author">작성자: {{ problem.authorNickname }}</span>
                                    </div>
                                    <div class="problem-meta">
                                        <span class="card-count-display">
                                            <img :src="cardIcon" alt="카드 아이콘" class="card-icon" />
                                            {{ problem.cardCount }}
                                        </span>
                                        <span class="icon-text like-toggle" :class="{ liked: problem.isLiked }" @click.stop="toggleLike(problem.id)">
                                            <img :src="problem.isLiked ? likeFilledIcon : likeOutlineIcon" alt="좋아요 아이콘" class="icon" />
                                            {{ problem.totalLikes || 0 }}
                                        </span>
                                        <span class="icon-text scrap-toggle" :class="{ scrapped: problem.isScrapped }" @click.stop="toggleScrap(problem.id)">
                                            <img :src="problem.isScrapped ? scrapFilledIcon : scrapOutlineIcon" alt="스크랩 아이콘" class="icon" />
                                            {{ problem.totalScraps || 0 }}
                                        </span>
                                    </div>
                                    <div v-if="problem.categories && problem.categories.length > 0" class="problem-tags">
                                        <span v-for="tag in problem.categories" :key="tag" class="tag">#{{ tag }}</span>
                                    </div>
                                </div>
                                <div class="problem-actions">
                                    <div :class="['problem-status-tag', problem.studyStatus]">
                                        {{ getStatusText(problem.studyStatus) }}
                                    </div>
                                    <button @click="goToStudy(problem.id)" class="solve-button">다시 풀기</button>
                                </div>
                            </li>
                        </ul>
                        <p v-else class="no-data-message">아직 완료한 학습이 없습니다.</p>
                    </div>
                </div>
            </section>
        </div>
    </div>
</template>

<style scoped>
/* 기존 MyStudyView.vue 스타일 유지 및 수정 */

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

/* Overall Study Summary Section */
.overall-summary-section {
    width: 100%;
    max-width: 800px;
    background-color: #ffffff;
    border-radius: 15px;
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    padding: 30px;
    margin-bottom: 40px;
    border: 1px solid #e0d0ff; /* Subtle border */
}

.overall-summary-section h2 {
    color: #5a2e87;
    font-size: 1.8rem;
    font-weight: 600;
    margin-bottom: 25px;
    border-bottom: 2px solid #a471ff;
    padding-bottom: 10px;
    text-align: center;
}

.summary-stats {
    display: flex;
    justify-content: space-around;
    flex-wrap: wrap;
    margin-bottom: 25px;
    gap: 15px;
}

.stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 10px 15px;
    border-radius: 10px;
    background-color: #f9f9f9;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    min-width: 120px;
    text-align: center;
}

.stat-item .label {
    font-size: 0.95rem;
    color: #777;
    margin-bottom: 5px;
    font-weight: 500;
}

.stat-item .value {
    font-size: 1.5rem;
    font-weight: bold;
}

.stat-item.perfect .value { color: #28a745; }
.stat-item.vague .value { color: #ffc107; }
.stat-item.forgotten .value { color: #dc3545; }
.stat-item.total .value { color: #5a2e87; }


.progress-bar-container {
    width: 100%;
    height: 30px; /* Height of the bar */
    background-color: #e9ecef; /* Background for the empty part */
    border-radius: 15px;
    overflow: hidden;
    display: flex;
    margin-bottom: 15px;
    box-shadow: inset 0 2px 5px rgba(0, 0, 0, 0.1);
}

.progress-bar {
    height: 100%;
    transition: width 0.5s ease-in-out;
    flex-shrink: 0; /* Prevent shrinking */
}

.progress-bar.perfect { background-color: #28a745; }
.progress-bar.vague { background-color: #ffc107; }
.progress-bar.forgotten { background-color: #dc3545; }
.progress-bar.no-data {
    background-color: #a471ff;
    width: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    color: white;
    font-weight: bold;
}

.progress-labels {
    display: flex;
    justify-content: space-around;
    font-size: 0.9rem;
    color: #555;
    margin-top: 10px;
}
.progress-labels .label {
    font-weight: 600;
}
.progress-labels .label.perfect { color: #28a745; }
.progress-labels .label.vague { color: #ffc107; }
.progress-labels .label.forgotten { color: #dc3545; }


/* Problems List Section */
.problems-list-section {
    width: 100%;
    max-width: 1200px; /* 가로 배치를 위해 최대 너비 증가 */
    background-color: #ffffff;
    border-radius: 15px;
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    padding: 30px;
    border: 1px solid #e0d0ff;
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
    justify-content: space-between;
    gap: 30px; /* 두 컬럼 사이 간격 */
    flex-wrap: wrap; /* 작은 화면에서 줄바꿈 */
}

.problem-list-column {
    flex: 1; /* 가용 공간을 균등하게 분배 */
    min-width: 350px; /* 컬럼 최소 너비 */
}

.problems-list-section h3 {
    color: #7a4cb8;
    font-size: 1.3rem;
    font-weight: 600;
    margin-top: 0; /* 상단 여백 제거 */
    margin-bottom: 20px;
    padding-left: 10px;
    border-left: 5px solid #a471ff;
}

.problem-list {
    list-style: none;
    padding: 0;
    margin: 0;
}

.problem-item {
    display: flex;
    flex-direction: column; /* 내용을 세로로 배치하고 액션 버튼을 아래로 */
    background-color: #f8f8f8;
    border: 1px solid #eee;
    border-radius: 10px;
    padding: 15px 20px;
    padding-top: 25px; /* 태그가 위로 올라가면서 내용과의 겹침을 방지 */
    margin-bottom: 15px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
    /* cursor: pointer; */ /* 문제를 클릭하면 상세 페이지로 이동하지 않고, 버튼으로 이동하므로 cursor 기본값으로 변경 */
    position: relative; /* 자식 요소의 absolute 위치 기준 */
    overflow: hidden;
}

.problem-item:hover {
    transform: translateY(-5px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.problem-content {
    flex-grow: 1;
    margin-bottom: 10px; /* 컨텐츠와 액션 버튼 사이 여백 */
}

.problem-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: 5px;
}

.problem-title { /* .problem-info 제거 */
    font-size: 1.2rem;
    font-weight: 600;
    color: #4a3f69;
    margin-right: 10px;
}

/* 작성자 닉네임 스타일 추가 */
.problem-author {
    font-size: 0.85rem;
    color: #888;
    font-style: italic;
    white-space: nowrap; /* 닉네임이 길어도 줄바꿈 방지 */
}

.problem-meta {
    display: flex;
    align-items: center;
    gap: 15px;
    font-size: 0.9rem;
    color: #777;
    margin-bottom: 8px;
}

.card-count-display {
    display: flex;
    align-items: center;
    font-weight: 500;
    color: #5a2e87;
}

.card-icon, .icon { /* .icon 클래스 추가 및 스타일 통일 */
    width: 18px; /* 카드 아이콘 크기 조절 */
    height: 18px;
    margin-right: 5px;
    vertical-align: middle;
}

.icon-text {
    display: flex;
    align-items: center;
    font-size: 0.85rem;
    cursor: pointer; /* 클릭 가능하도록 커서 추가 */
    transition: color 0.2s ease, transform 0.2s ease;
}
.icon-text:hover {
    transform: translateY(-2px);
}
/* 좋아요/스크랩 활성화/비활성화 시 색상 변화는 아이콘으로 대체되므로 텍스트 색상은 기본으로 유지 */
.icon-text.like-toggle { color: #a0a0a0; }
.icon-text.scrap-toggle { color: #a0a0a0; }


.problem-tags {
    margin-top: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
}

.tag {
    background-color: #e0d0ff;
    color: #5a2e87;
    padding: 3px 8px;
    border-radius: 5px;
    font-size: 0.8rem;
    font-weight: 500;
}

.problem-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    margin-top: auto; /* 하단 정렬 */
}

.problem-status-tag {
    position: absolute; /* 절대 위치 */
    top: 0px; /* 상단에 완전히 붙임 */
    right: 0px; /* 오른쪽에 완전히 붙임 */
    padding: 5px 15px;
    border-bottom-left-radius: 8px; /* 오른쪽 상단만 둥글게 */
    border-top-right-radius: 10px; /* problem-item의 radius와 맞춤 */
    font-size: 0.9rem;
    font-weight: bold;
    color: white;
    white-space: nowrap;
    z-index: 10;
}

.problem-status-tag.new,
.problem-status-tag.ongoing {
    background-color: #a471ff;
}

.problem-status-tag.completed {
    background-color: #28a745;
}

.solve-button {
    background-color: #7a4cb8; /* 버튼 색상 */
    color: white;
    padding: 8px 15px;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 600;
    transition: background-color 0.2s ease;
}

.solve-button:hover {
    background-color: #633b9e;
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

/* 반응형 디자인 */
@media (max-width: 768px) {
    .problem-lists-container {
        flex-direction: column; /* 세로로 다시 배치 */
        gap: 0;
    }
    .problem-list-column {
        min-width: unset; /* 최소 너비 제한 해제 */
        width: 100%; /* 전체 너비 사용 */
    }
    .problems-list-section h3 {
        margin-top: 30px; /* 컬럼 간 간격 */
    }
    .problem-status-tag { /* 작은 화면에서 위치 조정 */
        top: 0px; /* 상단에 완전히 붙임 */
        right: 0px; /* 오른쪽에 완전히 붙임 */
        padding: 3px 10px;
        font-size: 0.8rem;
        border-bottom-left-radius: 5px; /* 작은 화면에 맞게 조정 */
        border-top-right-radius: 10px; /* problem-item의 radius와 맞춤 */
    }
}
</style>