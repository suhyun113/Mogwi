<template>
  <div class="main-view-wrapper">
    <div class="main-inner">
      <div class="sticky-search">
        <SearchBar @search="handleSearch" :categories="categories" />
      </div>

      <div class="problem-list">
        <ProblemSummary
          v-for="problem in filteredProblems"
          :key="problem.id"
          :problem="problem"
          :isAuthenticated="isAuthenticated"
          :currentUserId="currentUserId"
          @solve="handleSolve"
          @auth-required="handleAuthRequired"
          @update-like="handleUpdateLike"
          @update-scrap="handleUpdateScrap"
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

      <StudyStartModal
        v-if="showStudyStartModal"
        @go-preview="showStudyStartModal = false"
        @go-study="goToStudy"
      />

    </div>
  </div>
</template>


<script>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import axios from 'axios'
import SearchBar from '@/components/SearchBar.vue'
import ProblemSummary from '@/components/ProblemSummary.vue'
import LoginPromptModal from '@/components/LoginPromptModal.vue'
import LoginModal from '@/components/LoginModal.vue'
import StudyStartModal from '@/components/StudyStartModal.vue'

export default {
  components: { SearchBar, ProblemSummary, LoginPromptModal, LoginModal, StudyStartModal },
  emits: ['open-login'],
  setup() {
    const store = useStore()
    const router = useRouter()
    const problems = ref([])

    const isAuthenticated = computed(() => !!store.state.store_userid)
    const currentUserId = computed(() => store.state.store_userid)

    const showLoginPrompt = ref(false)
    const showLoginModal = ref(false)
    const selectedProblem = ref(null)
    const showStudyStartModal = ref(false)
    const selectedProblemId = ref(null)
    const query = ref('')
    const selectedCategory = ref('#전체')
    const categories = ref(['#전체', '#수학', '#AI', '#컴퓨터', '#과학', '#역사', '#기타'])

    // 문제 불러오기기
    const fetchProblems = async () => {
      try {
        const response = await axios.get('/api/problems', {
          params: {
            query: query.value,
            category: selectedCategory.value,
            currentUserId: currentUserId.value
          }
        })
        problems.value = response.data
      } catch (error) {
        console.error('문제 불러오기 실패:', error)
      }
    }

    const handleSearch = ({ text, category }) => {
      query.value = text
      selectedCategory.value = category
      fetchProblems()
    }

    const handleSolve = (problem) => {
      if (isAuthenticated.value) {
        selectedProblemId.value = problem.id
        showStudyStartModal.value = true
      } else {
        selectedProblem.value = problem
        showLoginPrompt.value = true
      }
    }

    const goToStudy = () => {
      showStudyStartModal.value = false
      router.push(`/study/${selectedProblemId.value}`)
    }

    const handleAuthRequired = () => {
      showLoginPrompt.value = true
    }

    const openLoginModal = () => {
      showLoginPrompt.value = false
      showLoginModal.value = true
    }

    const handleUpdateLike = (problem) => {
      const target = problems.value.find(p => p.id === problem.id)
      if (target && target.authorId !== currentUserId.value) {
        target.liked = !target.liked
        target.likes += target.liked ? 1 : -1
      }
    }

    const handleUpdateScrap = (problem) => {
      const target = problems.value.find(p => p.id === problem.id)
      if (target && target.authorId !== currentUserId.value) {
        target.scrapped = !target.scrapped
        target.scraps += target.scrapped ? 1 : -1
      }
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

    onMounted(fetchProblems)

    return {
      problems,
      categories,
      filteredProblems,
      isAuthenticated,
      currentUserId,
      showLoginPrompt,
      showLoginModal,
      selectedProblem,
      handleSearch,
      handleSolve,
      goToStudy,
      showStudyStartModal,
      handleAuthRequired,
      openLoginModal,
      handleUpdateLike,
      handleUpdateScrap
    }
  }
}
</script>

<style scoped>
.main-view-wrapper {
  display: flex;
  justify-content: center;
}

.main-inner {
  width: 100%;
  max-width: 720px;
}

.sticky-search {
  position: sticky;
  top: 64px; /* 헤더 높이 고려 */
  background-color: #fdf8f4;
  z-index: 999;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.problem-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}
</style>
