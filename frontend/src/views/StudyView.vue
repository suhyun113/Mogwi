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
            this.problemStatus = '';
        }
        console.log('초기 문제 상태:', this.problemStatus);

      } catch (err) {
        console.error("문제 불러오기 실패:", err);
      }
    },

    async handleStudyStartClick() {
      if (this.isProcessing) return;  // 중복 실행 방지
      this.isProcessing = true;

      console.log('학습 시작 버튼 클릭됨');

      try {
        const problemId = this.problem.id;
        const response = await axios.post('/api/solve/start-study', {
          userId: this.currentUserId,
          problemId: problemId
        });

        const receivedStatus = response.data.problemStatus || '';
        this.problemStatus = receivedStatus;
        console.log('API 응답 데이터 (start-study problemStatus):', receivedStatus);

        if (receivedStatus === '') {
          this.showStudyStartModal = true;
          console.log('problemStatus가 "new"이므로 StudyStartModal 표시.');
        } else {
          console.log(`problemStatus가 "${receivedStatus}"이므로 바로 문제 풀이 페이지로 이동.`);
          this.router.push(`/study/${problemId}/solve`);
        }

      } catch (error) {
        console.error('문제 학습 시작 처리 중 예외 발생:', error);
        alert('문제 학습을 시작할 수 없습니다. 서버 오류 또는 네트워크 문제일 수 있습니다.');
      } finally {
        this.isProcessing = false;
      }
    },
    // StudyStartModal에서 '문제 구경하기' 클릭 시
    handleGoPreviewFromStudyModal() {
      this.showStudyStartModal = false; // 모달 닫기
      console.log('StudyView: "문제 구경하기" 클릭. 모달을 닫고 MainView로 이동. (문제는 이미 new 상태로 저장됨)');
      this.router.push('/'); // MainView로 라우팅
      // 문제의 DB 상태는 이미 'new'로 되어 있으므로 별도 API 호출 필요 없음
    },

    // StudyStartModal에서 '문제 바로 풀기' 클릭 시
    // 이 버튼을 클릭해도 'new' 상태를 유지하고 싶다면 'set-ongoing' API 호출을 제거합니다.
    async handleGoStudyFromStudyModal(problemIdFromModal) {
      this.showStudyStartModal = false; // 모달 닫기
      console.log(`StudyView: '문제 바로 풀기' 클릭. 문제 ${problemIdFromModal} 풀이 페이지로 이동. (new 상태 유지)`);
      // 요청에 따라 'set-ongoing' API 호출을 제거하고 바로 라우팅
      this.router.push(`/study/${problemIdFromModal}/solve`);
      // 만약 프론트엔드 상태도 'new'로 명시하고 싶다면:
      this.problemStatus = 'new';
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