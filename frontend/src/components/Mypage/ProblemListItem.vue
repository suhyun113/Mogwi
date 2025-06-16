<template>
  <div class="problem-list-item">
    <div class="problem-info">
      <span class="problem-title">{{ problem.title }}</span>
      <span class="problem-id">(ID: {{ problem.id }})</span>
      <span v-if="problem.is_public !== undefined" :class="['problem-status', { 'public': problem.is_public, 'private': !problem.is_public }]">
        {{ problem.is_public ? '공개' : '비공개' }}
      </span>
      <span class="problem-topic" v-if="problem.topic">
        <i class="fas fa-tag"></i> {{ problem.topic }}
      </span>
    </div>
    <div class="problem-actions">
      <slot name="actions"></slot>
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
      validator: (value) => {
        return (
          'id' in value &&
          'title' in value
          // 'is_public' in value // MyProblemSection에서만 필요하므로 필수는 아님
          // 'topic' in value // 선택적 속성
        );
      },
    },
  },
};
</script>

<style scoped>
.problem-list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  border: 1px solid #e0d0ff;
  border-radius: 8px;
  padding: 15px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.problem-list-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.problem-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap; /* 모바일에서 줄바꿈 */
  gap: 10px;
}

.problem-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #4a1e77;
  white-space: nowrap; /* 제목이 너무 길어도 줄바꿈 안되도록 */
  overflow: hidden;
  text-overflow: ellipsis; /* 넘칠 경우 ... */
  max-width: 60%; /* 제목 최대 너비 */
}

.problem-id {
  font-size: 0.85rem;
  color: #888;
}

.problem-status {
  font-size: 0.85rem;
  padding: 4px 8px;
  border-radius: 5px;
  font-weight: 500;
}

.problem-status.public {
  background-color: #e8f5e9; /* Light green */
  color: #2e7d32; /* Darker green */
}

.problem-status.private {
  background-color: #fbe9e7; /* Light red */
  color: #d32f2f; /* Darker red */
}

.problem-topic {
  font-size: 0.85rem;
  color: #8c5dff;
  background-color: #f0e6ff;
  padding: 4px 8px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.problem-actions {
  display: flex;
  gap: 5px; /* 액션 버튼 간격 */
  flex-shrink: 0; /* 공간이 부족해도 줄어들지 않도록 */
}

@media (max-width: 768px) {
  .problem-list-item {
    flex-direction: column;
    align-items: flex-start;
    padding: 12px 15px;
  }
  .problem-info {
    width: 100%;
    margin-bottom: 10px;
    gap: 8px;
  }
  .problem-title {
    font-size: 1rem;
    max-width: 100%;
  }
  .problem-id, .problem-status, .problem-topic {
    font-size: 0.8rem;
  }
  .problem-actions {
    width: 100%;
    justify-content: flex-end;
    gap: 8px;
  }
}
</style>