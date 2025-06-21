<template>
  <div class="study-view">
    <div v-if="!problem" class="loading">문제를 불러오는 중입니다...</div>
    <div v-else class="card-box">
      <ProblemCard
        :problem="problem"
        @like="toggleLike"
        @scrap="toggleScrap"
      />
      <StartButton @click="handleStudyStartClick" />
    </div>

    <StudyStartModal
      v-if="showStudyStartModal"
      :problemId="problem ? problem.id : null"
      @go-preview="handleGoPreviewFromStudyModal"
      @go-study="handleGoStudyFromStudyModal"
    />
  </div>
</template>

<script>
import axios from 'axios';
import StartButton from '@/components/StartButton.vue';
import ProblemCard from '@/components/Study/ProblemCard.vue';
import StudyStartModal from '@/components/Study/StudyStartModal.vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';

export default {
  components: { StartButton, ProblemCard, StudyStartModal },
  data() {
    return {
      problem: null,
      problemStatus: null,
      showStudyStartModal: false
    };
  },
  setup() {
    const store = useStore();
    const router = useRouter();
    const currentUserId = store.state.store_userid;
    return { currentUserId, router, store };
  },
  created() {
    const id = this.$route.params.id;
    this.fetchProblemDetails(id);
  },
  methods: {
    async fetchProblemDetails(id) {
      try {
        const response = await axios.get(`/api/problem/${id}`, {
          params: {
            currentUserId: this.$store.state.store_userid
          }
        });
        console.log('API 응답 데이터 (문제 상세):', response.data);
        this.problem = response.data;
        if (this.problem && this.problem.userProblemStatus) {
            this.problemStatus = this.problem.userProblemStatus.problemStatus;
        } else {
            this.problemStatus = 'new';
        }
        console.log('초기 문제 상태:', this.problemStatus);

      } catch (err) {
        console.error("문제 불러오기 실패:", err);
      }
    },

    async handleStudyStartClick() {
      const problemId = this.problem.id;

      try {
        const response = await axios.post('/api/solve/start-study', {
          userId: this.currentUserId,
          problemId: problemId
        });

        // 서버에서 받은 상태가 없을 수도 있으니 대비
        const receivedStatus = response.data.problemStatus || '';

        this.problemStatus = receivedStatus; // 화면용 상태 업데이트
        console.log('API 응답 데이터 (start-study problemStatus):', receivedStatus);

        if (receivedStatus === '') {
          this.showStudyStartModal = true;
          console.log('problemStatus가 빈 문자열이므로 StudyStartModal 표시.');
        } else {
          // 그 외의 모든 상태(new, ongoing, completed)는 바로 문제 풀이 페이지로 이동
          console.log(`problemStatus가 "${receivedStatus}"이므로 바로 문제 풀이 페이지로 이동.`);
          this.router.push(`/study/${problemId}/solve`);
        }

      } catch (error) {
        console.error('문제 학습 시작 처리 중 예외 발생:', error);
        alert('문제 학습을 시작할 수 없습니다. 서버 오류 또는 네트워크 문제일 수 있습니다.');
      }
    },

    // StudyStartModal에서 '문제 구경하기' 클릭 시 (수정된 부분)
    handleGoPreviewFromStudyModal() {
      this.showStudyStartModal = false; // 모달 닫기
      console.log('StudyView: "문제 구경하기" 클릭. 모달을 닫고 MainView로 이동.');
      this.router.push('/'); // MainView로 라우팅
    },

    // StudyStartModal에서 '문제 바로 풀기' 클릭 시
    async handleGoStudyFromStudyModal(problemIdFromModal) {
      this.showStudyStartModal = false;
      try {
        const response = await axios.post('/api/solve/set-ongoing', {
          userId: this.currentUserId,
          problemId: problemIdFromModal
        });

        if (response.status === 200) {
          console.log(`StudyView: 문제 ${problemIdFromModal} 학습 상태를 'ongoing'으로 업데이트 완료.`);
          this.problemStatus = 'ongoing';
          this.router.push(`/study/${problemIdFromModal}/solve`);
        } else {
          console.error('StudyView: 문제 상태 업데이트 실패:', response.data);
          alert('문제 학습을 시작하는 데 실패했습니다.');
        }
      } catch (error) {
        console.error('StudyView: 문제 상태 업데이트 중 오류:', error);
        alert('문제 학습을 시작하는 데 문제가 발생했습니다.');
      }
    },

    toggleLike() {
      if (!this.problem) return;
      const newLiked = !this.problem.liked;
      this.problem.liked = newLiked;
      this.problem.likes += newLiked ? 1 : -1;

      axios.post(`/api/like/${this.problem.id}`, {
        userId: this.$store.state.store_userid,
        liked: newLiked
      }).catch(err => {
        console.error("좋아요 반영 실패:", err);
        this.problem.liked = !newLiked;
        this.problem.likes += newLiked ? -1 : 1;
      });
    },
    toggleScrap() {
      if (!this.problem) return;
      const newScrapped = !this.problem.scrapped;
      this.problem.scrapped = newScrapped;
      this.problem.scraps += newScrapped ? 1 : -1;

      axios.post(`/api/scrap/${this.problem.id}`, {
        userId: this.$store.state.store_userid,
        scrapped: newScrapped
      }).catch(err => {
        console.error("스크랩 반영 실패:", err);
        this.problem.scrapped = !newScrapped;
        this.problem.scraps += newScrapped ? -1 : 1;
      });
    }
  },
  watch: {
    '$route.params.id': {
      handler(newId, oldId) {
        if (newId && newId !== oldId) {
          this.fetchProblemDetails(newId);
        }
      },
      immediate: true
    }
  }
};
</script>

<style scoped>
/* (스타일은 변경 없음) */
.study-view {
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #fdf8f4;
  overflow: hidden;
}
.loading {
  color: #9ca3af;
  font-size: 1.25rem;
}
.card-box {
  width: 280px;
  text-align: center;
}
</style>