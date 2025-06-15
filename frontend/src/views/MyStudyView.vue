<script setup>
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import axios from 'axios';

// assets/icons/card.png 경로를 import 합니다.
import cardIcon from '@/assets/icons/card.png';

const store = useStore();
const router = useRouter();

const username = computed(() => store.state.store_username);
const currentUserId = computed(() => store.state.store_userid);

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
  allUserProblems.value.filter(p => p.studyStatus === 'ongoing' || p.studyStatus === 'new') // 'new' 상태도 진행 중으로 간주
);
const completedProblems = computed(() =>
  allUserProblems.value.filter(p => p.studyStatus === 'completed')
);

const fetchMyStudyData = async () => {
  loading.value = true;
  error.value = null;

  if (!currentUserId.value) {
      loading.value = false;
      return;
  }

  try {
    const summaryResponse = await axios.get(`/api/mystudy/summary/${currentUserId.value}`);
    const summary = summaryResponse.data;
    overallPerfectCount.value = summary.perfect || 0;
    overallVagueCount.value = summary.vague || 0;
    overallForgottenCount.value = summary.forgotten || 0;
    overallTotalCards.value = summary.total || 0;

    const problemsResponse = await axios.get(`/api/mystudy/problems/detail/${currentUserId.value}`);
    // 백엔드에서 is_liked, is_scrapped, tags, author_nickname 등의 데이터를 받지 않으므로,
    // 기본값으로 설정하거나 해당 필드를 표시하지 않도록 합니다.
    // 여기서는 일단 백엔드에서 받은 데이터만 사용하고, 없는 필드는 표시하지 않습니다.
    allUserProblems.value = problemsResponse.data.map(problem => ({
        ...problem,
        // 아래 필드들은 백엔드에서 현재 제공되지 않습니다.
        // 백엔드에서 제공된다면 아래 주석을 풀고 사용하세요.
        // isLiked: problem.isLiked, // 예시
        // isScrapped: problem.isScrapped, // 예시
        // authorNickname: problem.authorNickname, // 예시
        // tags: problem.tags || [] // 예시: 태그가 없을 경우 빈 배열
    }));

  } catch (err) {
    console.error("나의 학습 데이터 불러오기 실패:", err);
    error.value = "학습 데이터를 불러오는 데 실패했습니다. 다시 시도해주세요.";
  } finally {
    loading.value = false;
  }
};

const goToStudy = (problemId) => {
  router.push({ name: 'SolveView', params: { problemId: problemId } });
};

// 백엔드에 좋아요/스크랩 상태 토글 요청
const toggleProblemStatus = async (problemId, field) => {
  try {
    const response = await axios.post(`/api/mystudy/problems/${problemId}/toggle-status`, {
      userId: currentUserId.value,
      field: field
    });
    if (response.data.status === 'OK') {
      // 프론트엔드 상태 업데이트
      const problemIndex = allUserProblems.value.findIndex(p => p.id === problemId);
      if (problemIndex !== -1) {
        if (field === 'isLiked') {
          allUserProblems.value[problemIndex].isLiked = response.data.newStatus;
        } else if (field === 'isScrapped') {
          allUserProblems.value[problemIndex].isScrapped = response.data.newStatus;
        }
      }
    }
  } catch (err) {
    console.error(`문제 ${field} 상태 토글 실패:`, err);
    alert('상태 변경에 실패했습니다.');
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

const isLoggedIn = computed(() => !!currentUserId.value);
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
                    <span v-if="problem.authorNickname" class="problem-author">by {{ problem.authorNickname }}</span>
                  </div>
                  <div class="problem-meta">
                    <span class="card-count-display">
                      <img :src="cardIcon" alt="카드 아이콘" class="card-icon" />
                      {{ problem.totalCards }}개 카드
                    </span>
                    <span v-if="problem.isLiked" class="icon-text liked">👍 좋아요</span>
                    <span v-if="problem.isScrapped" class="icon-text scrapped">⭐ 스크랩</span>
                  </div>
                  <div v-if="problem.tags && problem.tags.length > 0" class="problem-tags">
                    <span v-for="tag in problem.tags" :key="tag" class="tag">{{ tag }}</span>
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
                    <span v-if="problem.authorNickname" class="problem-author">by {{ problem.authorNickname }}</span>
                  </div>
                  <div class="problem-meta">
                    <span class="card-count-display">
                      <img :src="cardIcon" alt="카드 아이콘" class="card-icon" />
                      {{ problem.totalCards }}개 카드
                    </span>
                    <span v-if="problem.isLiked" class="icon-text liked">👍 좋아요</span>
                    <span v-if="problem.isScrapped" class="icon-text scrapped">⭐ 스크랩</span>
                  </div>
                  <div v-if="problem.tags && problem.tags.length > 0" class="problem-tags">
                    <span v-for="tag in problem.tags" :key="tag" class="tag">{{ tag }}</span>
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
  margin-bottom: 15px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.problem-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

/* 문제 아이콘 제거 (요청에 따라) */
/* .problem-icon {
  display: none;
} */

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

.problem-info .problem-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #4a3f69;
  margin-right: 10px;
}

.problem-author {
  font-size: 0.85rem;
  color: #888;
  font-style: italic;
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

.card-icon {
  width: 18px; /* 카드 아이콘 크기 조절 */
  height: 18px;
  margin-right: 5px;
  vertical-align: middle;
}

.icon-text {
  display: flex;
  align-items: center;
  font-size: 0.85rem;
}
.icon-text.liked { color: #28a745; } /* 좋아요 색상 */
.icon-text.scrapped { color: #ffc107; } /* 스크랩 색상 */

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
  /* position: absolute; 원래 위치 고정 */
  /* top: 15px; */
  /* right: 20px; */
  padding: 5px 15px;
  border-radius: 8px; /* 사각형 모서리 변경 */
  font-size: 0.9rem; /* 폰트 크기 조정 */
  font-weight: bold;
  color: white;
  white-space: nowrap; /* 줄바꿈 방지 */
}

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
}
</style>