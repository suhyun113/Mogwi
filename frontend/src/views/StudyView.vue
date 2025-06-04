<template>
  <div class="study-view">
    <div v-if="!problem" class="loading">문제를 불러오는 중입니다...</div>
    <div v-else class="study-main">
      <div class="card-count">총 {{ problem.cards.length }}개</div>
      <ProblemCard :problem="problem" @like="toggleLike" @scrap="toggleScrap" />
      <StartButton @click="startStudy" />
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import ProblemCard from '@/components/ProblemCard.vue';
import StartButton from '@/components/StartButton.vue';

export default {
  components: { ProblemCard, StartButton },
  data() {
    return {
      started: false,
      problem: null,
    };
  },
  created() {
    const id = this.$route.params.id;
    axios.get(`/api/problems/${id}`)
      .then(res => {
        this.problem = res.data;
      })
      .catch(err => {
        console.error("문제 불러오기 실패:", err);
      });
  },
  methods: {
    startStudy() {
      this.started = true;
    },
    toggleLike() {
      if (!this.problem) return;
      this.problem.liked = !this.problem.liked;
      this.problem.likes += this.problem.liked ? 1 : -1;
    },
    toggleScrap() {
      if (!this.problem) return;
      this.problem.scrapped = !this.problem.scrapped;
      this.problem.scraps += this.problem.scrapped ? 1 : -1;
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
  position: relative;
  width: 360px;
  text-align: center;
}
.card-count {
  position: absolute;
  top: -1.5rem;
  right: 0;
  font-size: 0.875rem;
  color: #6b7280;
}
</style>
