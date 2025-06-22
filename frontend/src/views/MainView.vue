<template>
  <div class="main-view-wrapper">
    <section class="banner-section">
      <CustomCarousel :slides="bannerSlides">
        <template #slide-0>
          <BannerOne bgColor="#A471FF" />
        </template>
        <template #slide-1>
          <BannerTwo bgColor="#FFC107" />
        </template>
        <template #slide-2>
          <BannerThree bgColor="#4CAF50" />
        </template>
      </CustomCarousel>
    </section>

    <div class="search-guidance">
      <p>아래의 검색창에 문제를 검색해보세요.</p>
    </div>

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
import SearchBar from '@/components/Main/SearchBar.vue'
import ProblemSummary from '@/components/Main/ProblemSummary.vue'
import LoginPromptModal from '@/components/Login/LoginPromptModal.vue'
import LoginModal from '@/components/Login/LoginModal.vue'
import StudyStartModal from '@/components/Study/StudyStartModal.vue'
import CustomCarousel from '@/components/Main/CustomCarousel.vue'; // CustomCarousel 임포트
import BannerOne from '@/components/Main/BannerOne.vue';     // BannerOne 임포트
import BannerTwo from '@/components/Main/BannerTwo.vue';     // BannerTwo 임포트
import BannerThree from '@/components/Main/BannerThree.vue'; // BannerThree 임포트

export default {
  components: {
    SearchBar,
    ProblemSummary,
    LoginPromptModal,
    LoginModal,
    StudyStartModal,
    CustomCarousel, // 컴포넌트 등록
    BannerOne,      // 컴포넌트 등록
    BannerTwo,      // 컴포넌트 등록
    BannerThree,    // 컴포넌트 등록
  },
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
    const categories = ref([])

    // 각 배너의 데이터를 정의합니다. (CustomCarousel의 slides prop에 전달)
    // 실제 콘텐츠는 각 BannerX.vue 컴포넌트가 담당하므로, 여기서는 슬라이드 수만 맞춰줍니다.
    const bannerSlides = ref([
      { id: 1, component: 'BannerOne' },
      { id: 2, component: 'BannerTwo' },
      { id: 3, component: 'BannerThree' },
    ]);


    const fetchCategories = async () => {
      try {
        const response = await axios.get('/api/categories')
        const sortedCategories = response.data
          .sort((a, b) => a.id - b.id)
          .map(cat => cat.tag_name)
        categories.value = ['#전체', ...sortedCategories]
      } catch (error) {
        console.error('카테고리 불러오기 실패:', error)
      }
    }

    const fetchProblems = async () => {
      try {
        // 로그인 여부에 따라 파라미터 분기
        const params = {
          onlyPublic: true,
          query: query.value,
        };
        if (selectedCategory.value && selectedCategory.value !== '#전체') {
          params.category = selectedCategory.value;
        }
        // 로그인 상태일 때만 currentUserId 파라미터를 추가
        if (isAuthenticated.value && currentUserId.value) {
          params.currentUserId = currentUserId.value;
        }
        const response = await axios.get('/api/problem/detail', { params });
        problems.value = response.data.sort((a, b) => a.id - b.id);
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
        router.push(`/study/${selectedProblemId.value}`);
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

    const handleUpdateLike = (updatedProblemData) => {
      const target = problems.value.find(p => p.id === updatedProblemData.id);
      if (target) {
        // Ensure the current user is not the author and is authenticated
        if (isAuthenticated.value && target.authorId !== currentUserId.value) {
          // Update the correct properties based on the emitted data
          target.isLiked = updatedProblemData.isLiked;
          target.totalLikes = updatedProblemData.totalLikes;
        } else if (!isAuthenticated.value) {
          alert("로그인 후 이용할 수 있습니다.");
        }
      }
    }

    const handleUpdateScrap = (updatedProblemData) => {
      const target = problems.value.find(p => p.id === updatedProblemData.id);
      if (target) {
        // Ensure the current user is not the author and is authenticated
        if (isAuthenticated.value && target.authorId !== currentUserId.value) {
          // Update the correct properties based on the emitted data
          target.isScrapped = updatedProblemData.isScrapped;
          target.totalScraps = updatedProblemData.totalScraps;
        } else if (!isAuthenticated.value) {
          alert("로그인 후 이용할 수 있습니다.");
        }
      }
    }

    const filteredProblems = computed(() => {
      return problems.value.filter(problem => {
        const matchText = problem.title.toLowerCase().includes(query.value.toLowerCase())
        const matchCategory =
          selectedCategory.value === '#전체' ||
          problem.categories.some(cat => cat.tag_name === selectedCategory.value)
        return matchText && matchCategory
      })
    })

    onMounted(() => {
      fetchCategories()
      fetchProblems()
    })

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
      handleUpdateScrap,
      bannerSlides, // bannerSlides를 반환
    }
  }
}
</script>

<style scoped>
.main-view-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 0;
  background-color: #f8f0ff;
  min-height: 100vh;
}

.banner-section {
  width: 100%;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
  background-color: #f0e6ff;
  min0height: 700px;
}

.main-inner {
  width: 100%;
  max-width: 900px;
  position: relative;
  padding: 0 16px;
  box-sizing: border-box;
  margin-top: 70px;
  margin: 0 auto;
}

.search-guidance {
  width: 100%;
  max-width: 900px; /* main-inner와 동일한 최대 너비 */
  text-align: center; /* 텍스트 가운데 정렬 */
  margin-top: 60px; 
  color: #555; /* 부드러운 회색 톤 */
  font-size: 1.25rem; /* 적당히 눈에 띄는 크기 */
  font-weight: 500;
  line-height: 1.5;
  padding: 0 16px; /* 좌우 여백 */
  box-sizing: border-box;
}

.search-guidance p {
    margin: 0; /* p 태그의 기본 margin 제거 */
}

.sticky-search {
  position: sticky;
  top: 74px;
  z-index: 999;
  background-color: transparent;
  padding: 12px 0;
  margin-top: 60px;
  margin-bottom: 10px;
  width: 100%;
  box-shadow: none;
}

.problem-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 25px;
  margin-bottom: 50px;
  padding: 0;
}

@media (max-width: 768px) {
  .banner-section {
    height: 55vh;
  }
  .main-inner {
    padding: 0 10px;
    max-width: 100%;
    margin: 40px auto;
  }
  .sticky-search {
    top: 60px;
    padding: 10px 0;
    margin: 0;
    width: 100%;
  }
  .sticky-search > * {
    padding: 0;
    max-width: 100%;
  }
  .problem-list {
    gap: 15px;
    margin-top: 20px;
    padding: 0;
  }
}
</style>