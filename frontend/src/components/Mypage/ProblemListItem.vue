<template>
  <div class="problem-list-item">
    <div class="problem-info">
      <span class="problem-title">{{ problem.title }}</span>
      <span v-if="problem.is_public !== undefined" :class="['problem-status', { 'public': problem.is_public, 'private': !problem.is_public }]">
        {{ problem.is_public ? '공개' : '비공개' }}
      </span>
      <span v-if="problem.topic" class="problem-topic">{{ problem.topic }}</span>
    </div>
    <div class="problem-actions">
      <button class="like-btn" @click.stop="toggleLike">
        <img :src="isLiked ? require('@/assets/icons/like_filled.png') : require('@/assets/icons/like_outline.png')" alt="좋아요" />
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProblemListItem',
  props: {
    problem: {
      type: Object,
      required: true,
    },
    isLiked: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['toggle-like'],
  methods: {
    toggleLike() {
      this.$emit('toggle-like', this.problem);
    },
  },
};
</script>

<style scoped>
.problem-list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border: 1.5px solid #e3d0ff;
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(138, 43, 226, 0.04);
  transition: box-shadow 0.2s, transform 0.2s;
}
.problem-list-item:hover {
  box-shadow: 0 4px 16px rgba(138, 43, 226, 0.10);
  transform: translateY(-2px);
}
.problem-info {
  display: flex;
  align-items: center;
  gap: 14px;
}
.problem-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #4a1e77;
  margin-right: 10px;
}
.problem-status {
  display: inline-block;
  font-size: 0.95rem;
  font-weight: 600;
  border-radius: 12px;
  padding: 4px 14px;
  margin-left: 6px;
}
.problem-status.public {
  background: #f3eaff;
  color: #6a2dbd;
}
.problem-status.private {
  background: #ffe6e6;
  color: #d32f2f;
}
.problem-topic {
  display: inline-block;
  background: #e6f0ff;
  color: #3b6bb2;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 500;
  padding: 4px 14px;
  margin-left: 6px;
}
.problem-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.like-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
}
.like-btn img {
  width: 24px;
  height: 24px;
}
</style>