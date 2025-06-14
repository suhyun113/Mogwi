<template>
  <div class="card">
    <div class="card-count">
      <img src="@/assets/icons/card.png" alt="card" class="card-icon" />
      <span>{{ problem.cardCount }}개</span>
    </div>
    <div class="title">{{ problem.title }}</div>
    <div class="author">작성자: {{ problem.author }}</div>
    
    <div class="tags">
      <span v-for="tag in problemCategories" :key="tag" class="tag" :style="{ backgroundColor: getTagColor(tag) }">
        {{ tag }}
      </span>
    </div>

    <div class="divider"></div>

    <div class="description-container">
      <template v-if="problem.description">
        <div class="description">{{ problem.description }}</div>
        <img src="@/assets/mogwi-character.png" alt="mogwi" class="mogwi-image" style="opacity: 0.15" />
      </template>
      <template v-else>
        <div class="empty-description">
          <p class="encourage-message">문제를 풀어보세요!</p>
          <img src="@/assets/mogwi-character.png" alt="mogwi" class="mogwi-image-small" />
        </div>
      </template>
    </div>

    <div class="divider"></div>

    <div class="icons">
      <div class="icon" @click="$emit('like')">
        <img :src="likeIcon" alt="like" />
        <span>{{ problemLikes }}</span>
      </div>
      <div class="icon" @click="$emit('scrap')">
        <img :src="scrapIcon" alt="scrap" />
        <span>{{ problemScraps }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import HeartFilled from '@/assets/icons/like_filled.png';
import HeartOutline from '@/assets/icons/like_outline.png';
import ScrapFilled from '@/assets/icons/scrap_filled.png';
import ScrapOutline from '@/assets/icons/scrap_outline.png';
import CardIcon from '@/assets/icons/card.png';

export default {
  props: {
    problem: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      cardIcon: CardIcon
    };
  },
  watch: {
    problem: {
      handler(newVal) {
        console.log('ProblemCard received problem data:', newVal);
      },
      immediate: true,
      deep: true
    }
  },
  computed: {
    problemCategories() {
      if (!this.problem.categories) return [];
      return Array.isArray(this.problem.categories) 
        ? this.problem.categories 
        : [this.problem.categories];
    },
    problemLikes() {
      return parseInt(this.problem.likes || 0);
    },
    problemScraps() {
      return parseInt(this.problem.scraps || 0);
    },
    likeIcon() {
      return this.problem.liked ? HeartFilled : HeartOutline;
    },
    scrapIcon() {
      return this.problem.scrapped ? ScrapFilled : ScrapOutline;
    }
  },
  methods: {
    getTagColor(tag) {
      const colors = {
        '#수학': '#ffd54f',
        '#AI': '#81c784',
        '#컴퓨터': '#64b5f6',
        '#과학': '#4dd0e1',
        '#역사': '#a1887f',
        '#기타': '#e0e0e0',
        '#프론트엔드': '#ba68c8',
        '#자료구조': '#f06292',
        '#전체': '#b0bec5'
      };
      return colors[tag] || '#e0e0e0';
    }
  }
};
</script>

<style scoped>
.card {
  background: white;
  border: 3px solid #a471ff;
  border-radius: 1rem;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;
  aspect-ratio: 2.5/3.5;
  position: relative;
}

.card-count {
  position: absolute;
  top: 1rem;
  right: 1rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: #6b7280;
  font-size: 0.875rem;
}

.card-icon {
  width: 1rem;
  height: 1rem;
}

.title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #000000;
  line-height: 1.4;
  margin-top: 0.5rem;
  text-align: center;
  width: 100%;
  margin-bottom: 0.25rem;
}

.author {
  font-size: 0.875rem;
  color: #6b7280;
  text-align: center;
  margin: 0;
}

.tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: 0.5rem 0;
  padding-bottom: 0.5rem;
}

.tag {
  font-size: 0.75rem;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-weight: 500;
}

.divider {
  width: 100%;
  height: 0.5px;
  background: #e5e7eb;
  margin: 0;
}

.description-container {
  flex: 1;
  min-height: 0;
  padding: 0.5rem 0;
  overflow: hidden;
  position: relative;
}

.description {
  font-size: 0.875rem;
  color: #4b5563;
  line-height: 1.5;
  text-align: center;
  height: 100%;
  overflow-y: auto;
  position: relative;
  z-index: 1;
}

.description::-webkit-scrollbar {
  width: 4px;
}

.description::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.description::-webkit-scrollbar-thumb {
  background: #a471ff;
  border-radius: 2px;
}

.icons {
  display: flex;
  justify-content: center;
  gap: 2rem;
  padding-top: 0.75rem;
}

.icon {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
}

.icon img {
  width: 1.25rem;
  height: 1.25rem;
}

.icon span {
  color: #6b7280;
  font-size: 0.875rem;
}

.mogwi-image {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 120px;
  height: auto;
  z-index: 0;
}

.empty-description {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 1rem;
  position: relative;
}

.mogwi-image-small {
  width: 80px;
  height: auto;
}

.encourage-message {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
  margin: 0;
  background: #f3f4f6;
  padding: 0.75rem 1rem;
  border-radius: 1rem;
  position: relative;
  max-width: 200px;
  text-align: center;
}

.encourage-message::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid #f3f4f6;
}
</style>
