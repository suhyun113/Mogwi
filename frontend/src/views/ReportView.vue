<template>
  <div class="report-view">
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
      <h1 class="page-title">나의 학습 리포트</h1>

      <section class="report-section calendar-section">
        <h2 class="section-title">날짜별 학습 기록</h2>
        <p class="section-description">달력에서 날짜를 선택하여 해당 날짜의 학습 기록을 확인하세요.</p>
        <StudyCalendar :studyDates="studyDates" @date-selected="handleDateSelected" />
        <DailyStudyDetail :selectedDate="selectedDate" :dailyStudyData="dailyStudyData" />
      </section>

      <section class="report-section chart-section">
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
// ReportSummary 컴포넌트 import가 제거되었습니다.
import StudyCalendar from '@/components/Report/StudyCalendar.vue';
import DailyStudyDetail from '@/components/Report/DailyStudyDetail.vue';
import WeeklyBarChart from '@/components/Report/WeeklyBarChart.vue';
import LoginModal from '@/components/Login/LoginModal.vue';
import RegisterModal from '@/components/Register/RegisterModal.vue';

export default {
  name: 'ReportView',
  components: {
    // ReportSummary 컴포넌트가 제거되었습니다.
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

    // Modals
    const showLoginModal = ref(false);
    const showRegisterModal = ref(false);

    // Overall Study Summary Data - 이 데이터들은 이제 ReportSummary 컴포넌트가 없으므로 사용되지 않을 수 있습니다.
    // 하지만 API 호출 자체는 남아있고, 혹시 모를 다른 곳에서의 사용을 위해 일단 유지합니다.
    const overallPerfectCount = ref(0);
    const overallVagueCount = ref(0);
    const overallForgottenCount = ref(0);
    const overallTotalCards = ref(0);

    // Calendar Data
    const studyDates = ref({}); // { 'YYYY-MM-DD': { perfect: N, vague: M, forgotten: O }, ... }
    const selectedDate = ref(null); // The date selected in the calendar
    const dailyStudyData = ref(null); // Data for the currently selected date

    // Weekly Chart Data
    const weeklyChartData = ref([]); // [{ date: 'MM-DD', perfect: N, vague: M, forgotten: O }, ...]

    const fetchReportData = async () => {
      if (!isLoggedIn.value) {
        loading.value = false;
        return;
      }

      loading.value = true;
      error.value = null;

      try {
        // Fetch overall summary (API 호출은 유지)
        const summaryResponse = await axios.get(`/api/report/summary/${currentUserId.value}`);
        const summary = summaryResponse.data;
        overallPerfectCount.value = summary.perfect || 0;
        overallVagueCount.value = summary.vague || 0;
        overallForgottenCount.value = summary.forgotten || 0;
        overallTotalCards.value = summary.total || 0;

        // Fetch daily study records for the calendar
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

        // Fetch weekly chart data
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
      overallPerfectCount, // 여전히 계산되지만, 화면에 표시되지 않습니다.
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
    };
  },
};
</script>

<style scoped>
.report-view {
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

.report-container {
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
  gap: 30px; /* Space between sections */
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

.report-section {
  background-color: #fcf9fc;
  border: 1px solid #ede1ff;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.section-title {
  color: #5a2e87;
  font-size: 1.8rem;
  font-weight: 600;
  margin-bottom: 10px;
  text-align: center;
}

.section-description {
  color: #888;
  font-size: 0.95rem;
  text-align: center;
  margin-bottom: 20px;
}

/* summary-section 스타일은 ReportSummary가 제거되었으므로 더 이상 필요하지 않을 수 있습니다.
   하지만 혹시 다른 곳에서 이 클래스를 사용하거나 재사용 가능성을 고려하여 일단 유지합니다.
   만약 확실히 필요없다면 이 부분도 제거 가능합니다. */
.summary-section {
  background-color: #f0e6ff;
  padding: 30px;
}

.calendar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.chart-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
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

@media (max-width: 768px) {
  .report-container {
    padding: 20px;
    gap: 20px;
  }
  .page-title {
    font-size: 2rem;
    margin-bottom: 20px;
    padding-bottom: 15px;
  }
  .section-title {
    font-size: 1.5rem;
  }
  .section-description {
    font-size: 0.9rem;
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