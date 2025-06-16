<template>
  <div class="report-view">
    <section class="report-banner-section">
      <div class="report-banner">
        <div class="banner-content">
          <div class="text-content">
            <h1 class="banner-title">나의 학습 리포트</h1>
            <p class="banner-description">
              모귀와 함께한 학습 여정을 한눈에 확인하고,<br>
              더 효율적인 학습 계획을 세워보세요!
            </p>
          </div>
          <img src="@/assets/mogwi-character.png" alt="모귀 리포트 캐릭터" class="banner-mogwi-character">
        </div>
      </div>
    </section>

    <div v-if="loading" class="loading-message">데이터를 불러오는 중입니다...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else-if="!isLoggedIn" class="logged-out-prompt">
      <img src="@/assets/mogwi-character.png" alt="모귀 캐릭터" class="mogwi-character-small" />
      <p class="logged-out-message">
        <i class="fas fa-lock"></i> 학습 리포트는 로그인 후 이용 가능합니다.
      </p>
      <button @click="showLoginModal = true" class="login-button">로그인</button>
    </div>

    <div v-else class="report-container">
      <div class="tab-buttons">
        <button
          :class="{ 'tab-button': true, 'active': activeTab === 'daily' }"
          @click="activeTab = 'daily'"
        >
          날짜별 학습 기록
        </button>
        <button
          :class="{ 'tab-button': true, 'active': activeTab === 'weekly' }"
          @click="activeTab = 'weekly'"
        >
          주간 학습량
        </button>
      </div>

      <section v-if="activeTab === 'daily'" class="report-section calendar-section">
        <div class="calendar-and-detail-wrapper">
          <div class="daily-detail-wrapper">
            <h2 class="section-title-daily-detail">날짜별 학습 기록</h2>
            <img :src="dailyCharacterImage" alt="모귀 캐릭터" class="mogwi-daily-detail-character">
            <DailyStudyDetail :selectedDate="selectedDate" :dailyStudyData="dailyStudyData" />
          </div>
          <div class="calendar-wrapper">
            <StudyCalendar :studyDates="studyDates" @date-selected="handleDateSelected" />
          </div>
        </div>
      </section>

      <section v-if="activeTab === 'weekly'" class="report-section chart-section">
        <h2 class="section-title">주간 학습량</h2>
        <p class="section-description">지난 한 달간의 주별 학습 카드 수를 막대 그래프로 확인하세요.</p>
        <WeeklyBarChart :chartData="weeklyChartData" />
      </section>
    </div>

    <LoginModal v-if="showLoginModal" @close="showLoginModal = false" @open-register="openRegisterModal" />
    <RegisterModal v-if="showRegisterModal" @close="showRegisterModal = false" @open-login="openLoginModal" />
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useStore } from 'vuex';
import axios from 'axios';
import StudyCalendar from '@/components/Report/StudyCalendar.vue';
import DailyStudyDetail from '@/components/Report/DailyStudyDetail.vue';
import WeeklyBarChart from '@/components/Report/WeeklyBarChart.vue';
import LoginModal from '@/components/Login/LoginModal.vue';
import RegisterModal from '@/components/Register/RegisterModal.vue';

import mogwiCharacter from '@/assets/mogwi-character.png';
import mogwiSleep from '@/assets/mogwi-sleep.png';

export default {
  name: 'ReportView',
  components: {
    StudyCalendar,
    DailyStudyDetail,
    WeeklyBarChart,
    LoginModal,
    RegisterModal,
  },
  setup() {
    const store = useStore();
    const isLoggedIn = computed(() => !!store.state.store_userid);
    const currentUserId = computed(() => store.state.store_userid);

    const loading = ref(true);
    const error = ref(null);

    const showLoginModal = ref(false);
    const showRegisterModal = ref(false);

    const overallPerfectCount = ref(0);
    const overallVagueCount = ref(0);
    const overallForgottenCount = ref(0);
    const overallTotalCards = ref(0);

    const studyDates = ref({});
    const selectedDate = ref(null);
    const dailyStudyData = ref(null);

    const weeklyChartData = ref([]);

    // 탭 상태 관리: 'daily' 또는 'weekly'
    const activeTab = ref('daily'); // 기본값은 'daily'

    const dailyCharacterImage = computed(() => {
      if (dailyStudyData.value && (dailyStudyData.value.perfect > 0 || dailyStudyData.value.vague > 0 || dailyStudyData.value.forgotten > 0)) {
        return mogwiCharacter;
      } else {
        return mogwiSleep;
      }
    });

    const fetchReportData = async () => {
      if (!isLoggedIn.value) {
        loading.value = false;
        return;
      }

      loading.value = true;
      error.value = null;

      try {
        const summaryResponse = await axios.get(`/api/report/summary/${currentUserId.value}`);
        const summary = summaryResponse.data;
        overallPerfectCount.value = summary.perfect || 0;
        overallVagueCount.value = summary.vague || 0;
        overallForgottenCount.value = summary.forgotten || 0;
        overallTotalCards.value = summary.total || 0;

        const dailyResponse = await axios.get(`/api/report/daily-records/${currentUserId.value}`);
        const transformedStudyDates = {};
        dailyResponse.data.forEach(record => {
          transformedStudyDates[record.date] = {
            perfect: record.perfect || 0,
            vague: record.vague || 0,
            forgotten: record.forgotten || 0,
          };
        });
        studyDates.value = transformedStudyDates;

        const today = new Date().toISOString().slice(0, 10);
        if (studyDates.value[today]) {
          selectedDate.value = today;
          dailyStudyData.value = studyDates.value[today];
        } else {
          selectedDate.value = null;
          dailyStudyData.value = null;
        }

        const weeklyResponse = await axios.get(`/api/report/weekly-records/${currentUserId.value}`);
        weeklyChartData.value = weeklyResponse.data.map(item => ({
          label: item.weekStart.substring(5),
          perfect: item.perfect || 0,
          vague: item.vague || 0,
          forgotten: item.forgotten || 0,
          total: item.total || 0,
        }));

      } catch (err) {
        console.error('리포트 데이터 불러오기 실패:', err);
        error.value = '리포트 데이터를 불러오는 데 실패했습니다.';
      } finally {
        loading.value = false;
      }
    };

    const handleDateSelected = (date) => {
      selectedDate.value = date;
      dailyStudyData.value = studyDates.value[date] || { perfect: 0, vague: 0, forgotten: 0 };
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
      fetchReportData();
    });

    watch(isLoggedIn, (newVal, oldVal) => {
      if (newVal !== oldVal && newVal) {
        fetchReportData();
      } else if (!newVal) {
        overallPerfectCount.value = 0;
        overallVagueCount.value = 0;
        overallForgottenCount.value = 0;
        overallTotalCards.value = 0;
        studyDates.value = {};
        selectedDate.value = null;
        dailyStudyData.value = null;
        weeklyChartData.value = [];
        loading.value = false;
      }
    }, { immediate: true });

    return {
      loading,
      error,
      isLoggedIn,
      overallPerfectCount,
      overallVagueCount,
      overallForgottenCount,
      overallTotalCards,
      studyDates,
      selectedDate,
      dailyStudyData,
      weeklyChartData,
      handleDateSelected,
      showLoginModal,
      showRegisterModal,
      openLoginModal,
      openRegisterModal,
      activeTab, // activeTab 반환
      dailyCharacterImage, // computed 속성 반환
    };
  },
};
</script>

<style scoped>
.report-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 0;
  background-color: #fdf8f4;
  min-height: 100vh;
  width: 100%;
  box-sizing: border-box;
  font-family: 'Pretendard', sans-serif;
  overflow-x: hidden; /* 가로 스크롤 제거 */
}

/* Report Banner Styles */
.report-banner-section {
  width: 100%;
  margin-bottom: 40px;
}

.report-banner {
  width: 100%;
  padding: 60px 20px;
  background: linear-gradient(to right, #8c5dff, #a471ff);
  color: white;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-content {
  position: relative;
  z-index: 10;
  max-width: 1000px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  box-sizing: border-box;
}

.text-content {
  text-align: left;
  max-width: 50%;
}

.banner-title {
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 15px;
  text-shadow: 2px 2px 5px rgba(0, 0, 0, 0.2);
}

.banner-description {
  font-size: 1.3rem;
  line-height: 1.6;
  margin-bottom: 25px;
  font-weight: 400;
  opacity: 0.95;
}

.banner-mogwi-character {
  width: 220px;
  height: auto;
  opacity: 0.8;
  filter: drop-shadow(5px 5px 10px rgba(0,0,0,0.2));
  flex-shrink: 0;
}

@media (max-width: 992px) {
  .banner-content {
    padding: 0 20px;
  }
  .text-content {
    max-width: 60%;
  }
  .banner-title {
    font-size: 2.5rem;
  }
  .banner-description {
    font-size: 1.1rem;
  }
  .banner-mogwi-character {
    width: 180px;
  }
}

@media (max-width: 768px) {
  .report-banner {
    padding: 40px 15px;
    min-height: 250px;
  }
  .banner-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0;
  }
  .text-content {
    max-width: 100%;
    margin-bottom: 20px;
    text-align: center;
  }
  .banner-title {
    font-size: 2rem;
  }
  .banner-description {
    font-size: 1rem;
    margin-bottom: 15px;
  }
  .banner-mogwi-character {
    width: 150px;
  }
}

@media (max-width: 480px) {
  .report-banner {
    padding: 30px 10px;
    min-height: 200px;
  }
  .banner-title {
    font-size: 1.7rem;
  }
  .banner-description {
    font-size: 0.9rem;
  }
  .banner-mogwi-character {
    width: 120px;
  }
}

/* Report Container & Common Section Styles */
.report-container {
  padding: 40px;
  width: 100%;
  max-width: 1100px;
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.loading-message, .error-message {
  color: #6c757d;
  font-size: 1.1rem;
  margin-top: 30px;
  text-align: center;
}

/* Tab Buttons Styles */
.tab-buttons {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
  width: 100%;
  max-width: 350px; /* Adjusted max-width to reduce overall button width */
  margin: 0 auto 30px auto;
  border-radius: 30px; /* Pill shape */
  background-color: transparent; /* Remove the gray background container */
  padding: 0; /* Remove padding from the container */
  box-shadow: none; /* Remove container shadow */
}

.tab-button {
  flex: 1;
  padding: 10px 15px; /* Adjusted padding to reduce button width */
  border: none;
  background-color: transparent; /* Default transparent background */
  color: #666; /* Default text color (gray) */
  font-size: 1.1rem; /* Adjust font size */
  font-weight: 500; /* Adjust font weight */
  cursor: pointer;
  border-radius: 25px; /* Pill shape */
  transition: all 0.3s ease;
  white-space: nowrap;
}

.tab-button.active {
  background: linear-gradient(to right, #8c5dff, #a471ff); /* Purple gradient for active button */
  color: white; /* White text for active button */
  box-shadow: 0 4px 10px rgba(140, 93, 255, 0.3); /* Subtle shadow for active button matching the banner */
}

.tab-button:hover:not(.active) {
  background-color: #f0f0f0; /* Light hover background */
  color: #333; /* Slightly darker text on hover */
}

/* Report Section General Styles */
.report-section {
  padding: 30px;
}

.section-title {
  color: #5a2e87;
  font-size: 2rem;
  font-weight: 600;
  margin-bottom: 12px;
  text-align: center;
  position: relative;
  padding-bottom: 15px;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 3px;
  background: linear-gradient(to right, #8c5dff, #a471ff);
  border-radius: 3px;
}

.section-description {
  color: #666;
  font-size: 1.1rem;
  text-align: center;
  margin-bottom: 25px;
  line-height: 1.6;
}

.calendar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
}

.calendar-and-detail-wrapper {
  display: flex;
  flex-direction: row; /* 가로 배치 */
  gap: 20px; /* 각 섹션 사이 간격 */
  width: 100%;
  justify-content: center;
  align-items: stretch; /* 세로 길이를 같게 늘리기 */
}

.daily-detail-wrapper {
  flex: 1.2; /* 왼쪽 섹션을 더 넓게 */
  background-color: #8c5dff; /* 보라색 배경 */
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 25px; /* 패딩 통일 */
  color: white; /* 텍스트 색상 흰색으로 변경 */
  box-sizing: border-box;
  text-align: center;
  height: 100%; /* 세로 길이를 부모 컨테이너에 맞춤 */
}

.section-title-daily-detail {
  color: white; /* 제목 색상 흰색으로 변경 */
  font-size: 2rem;
  font-weight: 600;
  margin-bottom: 20px;
  position: relative;
  padding-bottom: 15px;
}

.mogwi-daily-detail-character {
  width: 150px; /* 캐릭터 이미지 크기 조정 */
  height: auto;
  margin-bottom: 20px;
  filter: drop-shadow(0 0 10px rgba(0,0,0,0.3));
}

/* DailyStudyDetail 컴포넌트 내부의 스타일을 조절해야 할 수 있습니다.
   여기서는 ReportView에서 DailyStudyDetail 컴포넌트 자체에는
   배경이나 패딩을 주지 않고, daily-detail-wrapper에서 전체 스타일을 제어합니다. */
.daily-detail-wrapper >>> .daily-study-detail {
  width: 100%; /* DailyStudyDetail이 부모 너비를 차지하도록 */
  background-color: transparent; /* DailyStudyDetail 자체 배경 투명 */
  padding: 0; /* DailyStudyDetail 자체 패딩 제거 */
  box-shadow: none; /* DailyStudyDetail 자체 그림자 제거 */
  border: none; /* DailyStudyDetail 자체 테두리 제거 */
}
/* 필요하다면 DailyStudyDetail 내부의 텍스트 색상 등 조정 */
.daily-detail-wrapper >>> .study-detail-item p,
.daily-detail-wrapper >>> .no-data-message {
  color: white; /* DailyStudyDetail 내부 텍스트 색상을 흰색으로 */
}


.calendar-wrapper {
  flex: 2; /* 캘린더 섹션을 더 넓게 */
  background-color: #ffffff; /* 흰색 배경 */
  display: flex;
  flex-direction: column;
  padding: 25px; /* 패딩 통일 */
  box-sizing: border-box;
}


.chart-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 25px;
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
}

/* Logged out prompt styles */
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

/* Media Queries for Responsiveness */
@media (max-width: 1200px) {
  .report-container {
    max-width: 900px;
  }
  .calendar-section, .chart-section {
    max-width: 900px;
  }
  .calendar-and-detail-wrapper {
    flex-direction: column; /* 작은 화면에서는 세로 배치 */
    gap: 20px; /* 세로 배치 시 간격 */
  }
  .calendar-wrapper, .daily-detail-wrapper {
    width: 100%;
    min-width: unset;
    padding: 20px; /* 모바일 패딩 조정 */
  }
  /* 모바일에서 DailyStudyDetail 섹션의 높이 고정 해제 */
  .daily-detail-wrapper {
    min-height: unset;
  }
}

@media (max-width: 768px) {
  .report-banner-section {
    margin-bottom: 25px;
  }
  .report-container {
    padding: 20px;
    gap: 20px;
    max-width: 100%;
  }
  .tab-buttons {
    margin: 0 auto 20px auto;
    padding: 0;
    max-width: 280px;
  }
  .tab-button {
    padding: 8px 10px;
    font-size: 0.9rem;
  }
  .section-title {
    font-size: 1.5rem;
  }
  .section-description {
    font-size: 0.9rem;
  }
  .report-section {
    padding: 20px;
    gap: 15px;
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
  .calendar-and-detail-wrapper {
    gap: 20px;
  }
  .daily-detail-wrapper {
    padding: 20px; /* 모바일에서 다시 패딩 적용 */
  }
  .section-title-daily-detail {
    font-size: 1.5rem;
  }
  .mogwi-daily-detail-character {
    width: 100px;
  }
}
</style>