<template>
  <div class="card">
    <div class="title">{{ problem.title }}</div>
    <div class="author">작성자: {{ problem.author }}</div>

    <div class="tags">
      <span v-for="tag in problem.categories" :key="tag" class="tag">#{{ tag }}</span>
    </div>

    <div class="icons">
      <div class="icon" @click="$emit('like')">
        <img :src="likeIcon" alt="like" />
        <span>{{ problem.likes }}</span>
      </div>
      <div class="icon" @click="$emit('scrap')">
        <img :src="scrapIcon" alt="scrap" />
        <span>{{ problem.scraps }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import HeartFilled from '@/assets/icons/like_filled.png';
import HeartOutline from '@/assets/icons/like_outline.png';
import ScrapFilled from '@/assets/icons/scrap_filled.png';
import ScrapOutline from '@/assets/icons/scrap_outline.png';

export default {
  props: {
    problem: Object
  },
  computed: {
    likeIcon() {
      return this.problem.liked ? HeartFilled : HeartOutline;
    },
    scrapIcon() {
      return this.problem.scrapped ? ScrapFilled : ScrapOutline;
    }
  }
};
</script>

<style scoped>
.card {
  background: white;
  border: 3px solid #7c3aed;
  border-radius: 1rem;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #6d28d9;
}
.author {
  font-size: 0.875rem;
  color: #6b7280;
}
.tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.tag {
  font-size: 0.75rem;
  background: #e5e7eb;
  color: #4b5563;
  padding: 0.25rem 0.5rem;
  border-radius: 9999px;
}
.icons {
  display: flex;
  justify-content: center;
  gap: 2rem;
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
</style>
