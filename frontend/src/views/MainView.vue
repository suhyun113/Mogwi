<template>
  <div class="main-container">
    <SearchBar @search="handleSearch" />
    <div class="problem-list">
      <ProblemSummary
        v-for="problem in problems"
        :key="problem.id"
        :problem="problem"
        @click="handleClick(problem)"
      />
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import SearchBar from '@/components/SearchBar.vue'
import ProblemSummary from '@/components/ProblemSummary.vue'

export default {
  components: {
    SearchBar,
    ProblemSummary
  },
  setup() {
    const problems = ref([
      {
        id: 1,
        title: '자료구조 마스터',
        category: '#자료구조',
        likes: 12,
        scraps: 8,
        cardCount: 5
      },
      {
        id: 2,
        title: 'Vue 기초 다지기',
        category: '#프론트엔드',
        likes: 24,
        scraps: 15,
        cardCount: 10
      }
    ])
    const selectedProblem = ref(null)
    const query = ref('')

    const handleSearch = (q) => {
      query.value = q
      // 여기서는 필터링 없이 그대로 사용
    }

    const handleClick = (problem) => {
      selectedProblem.value = problem
      alert('문제 상세 보기 기능은 로그인 후 제공됩니다.')
    }

    onMounted(() => {
      // 실제 API 연동 시 fetchProblems로 대체
    })

    return {
      problems,
      selectedProblem,
      query,
      handleSearch,
      handleClick
    }
  }
}
</script>

<style scoped>
.main-container {
  padding: 24px;
}
.problem-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}
</style>
