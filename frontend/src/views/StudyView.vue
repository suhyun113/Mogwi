<template>
  <div class="study-view">
    <div v-if="!problem" class="loading">문제를 불러오는 중입니다...</div>
    <div v-else class="study-main">
      <div class="card-box">
        <ProblemCard
          :problem="problem"
          @like="toggleLike"
          @scrap="toggleScrap"
        />
        <StartButton @click="startStudy" />
      </div>
    </div>
    <StudyStartModal
      v-if="showStudyStartModal"
      @go-preview="showStudyStartModal = false"
      @go-study="goToSolveView"
    />
  </div>
</template>

<script>
import axios from 'axios';
import StartButton from '@/components/StartButton.vue';
import ProblemCard from '@/components/ProblemCard.vue';
import StudyStartModal from '@/components/StudyStartModal.vue';

export default {
  components: { StartButton, ProblemCard, StudyStartModal },
  data() {
    return {
      problem: null,
      showStudyStartModal: false
    };
  },
  created() {
    const id = this.$route.params.id;
    const store = this.$store;
    const currentUserId = store.state.store_userid;
    
    axios.get(`/api/problems/${id}`, {
      params: {
        currentUserId: currentUserId
      }
    })
      .then(res => {
        console.log('API 응답 데이터:', res.data);
        this.problem = res.data;
      })
      .catch(err => {
        console.error("문제 불러오기 실패:", err);
      });
  },
  methods: {
    startStudy() {
      this.showStudyStartModal = true;
    },
    goToSolveView() {
      this.showStudyStartModal = false;
      this.$router.push(`/study/${this.$route.params.id}/solve`);
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
  }
};
</script>

<style scoped>
.study-view {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #fdf8f4;
}
.loading {
  color: #9ca3af;
  font-size: 1.25rem;
}
.study-main {
  width: 100%;
  display: flex;
  justify-content: center;
}
.card-box {
  width: 280px;
  text-align: center;
}
</style>
