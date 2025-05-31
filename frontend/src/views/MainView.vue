<template>
  <div class="main-view-wrapper">
    <div class="main-inner">
      <SearchBar @search="handleSearch" :categories="categories" />

      <div class="problem-list">
        <ProblemSummary
          v-for="problem in filteredProblems"
          :key="problem.id"
          :problem="problem"
          :isAuthenticated="isAuthenticated"
          @solve="handleSolve"
          @auth-required="handleAuthRequired"
          @update-like="increaseLike"
          @update-scrap="increaseScrap"
        />
      </div>

      <LoginPromptModal
        v-if="showLoginPrompt"
        @continue="showLoginPrompt = false"
        @open-login="openLoginModal"
      />

      <LoginModal
        v-if="showLoginModal"
        @close="showLoginModal = false"
      />
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import SearchBar from '@/components/SearchBar.vue'
import ProblemSummary from '@/components/ProblemSummary.vue'
import LoginPromptModal from '@/components/LoginPromptModal.vue'
import LoginModal from '@/components/LoginModal.vue'

export default {
  components: { SearchBar, ProblemSummary, LoginPromptModal, LoginModal },
  setup() {
    const router = useRouter()
    const problems = ref([
      { id: 1, title: '자료구조 마스터', categories: ['#컴퓨터', '#자료구조'], likes: 12, scraps: 8, cardCount: 5 },
      { id: 2, title: 'Vue 기초 다지기', categories: ['#프론트엔드', '#컴퓨터'], likes: 24, scraps: 15, cardCount: 10 },
      { id: 3, title: '기초 통계', categories: ['#수학', '#AI'], likes: 10, scraps: 5, cardCount: 6 }
    ])

    const isAuthenticated = ref(false)
    const showLoginPrompt = ref(false)
    const showLoginModal = ref(false)
    const selectedProblem = ref(null)
    const query = ref('')
    const selectedCategory = ref('#전체')
    const categories = ref(['#전체', '#수학', '#AI', '#컴퓨터', '#과학', '#역사', '#기타'])

    const handleSearch = ({ text, category }) => {
      query.value = text
      selectedCategory.value = category
    }

    const handleSolve = (problem) => {
      if (isAuthenticated.value) {
        router.push(`/study/${problem.id}`)
      } else {
        selectedProblem.value = problem
        showLoginPrompt.value = true
      }
    }

    const handleAuthRequired = () => {
      showLoginPrompt.value = true
    }

    const openLoginModal = () => {
      showLoginPrompt.value = false
      showLoginModal.value = true
    }

    const increaseLike = (problem) => {
      const target = problems.value.find(p => p.id === problem.id)
      if (target) target.likes++
    }

    const increaseScrap = (problem) => {
      const target = problems.value.find(p => p.id === problem.id)
      if (target) target.scraps++
    }

    const filteredProblems = computed(() => {
      return problems.value.filter(problem => {
        const matchText = problem.title.toLowerCase().includes(query.value.toLowerCase())
        const matchCategory =
          selectedCategory.value === '#전체' ||
          problem.categories.includes(selectedCategory.value)
        return matchText && matchCategory
      })
    })

    return {
      problems,
      categories,
      filteredProblems,
      isAuthenticated,
      showLoginPrompt,
      showLoginModal,
      selectedProblem,
      handleSearch,
      handleSolve,
      handleAuthRequired,
      openLoginModal,
      increaseLike,
      increaseScrap
    }
  }
}
</script>

<style scoped>
.main-view-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px;
}
.main-inner {
  width: 100%;
  max-width: 720px;
}
.problem-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}
</style>
