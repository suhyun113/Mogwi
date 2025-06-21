<template>
  <div class="problem-item">
    <div class="item-header">
      <div class="left-info">
        <h3 class="title">{{ problem.title }}</h3>
        <p class="author">작성자: {{ problem.author_name }}</p>
        <span v-if="problem.topic" class="tag">#{{ problem.topic }}</span>
      </div>
      <div class="right-info">
        <span class="status-label">진행 중</span>
        <button class="solve-button" @click="goToProblem">문제 풀기</button>
      </div>
    </div>

    <div class="item-footer">
      <div class="icons">
        <i class="fas fa-heart like-icon"></i> {{ problem.likes }}
        <i class="fas fa-bookmark scrap-icon"></i> {{ problem.scraps }}
      </div>
      <div class="progress">
        <span class="green">{{ problem.solved }}</span> /
        <span class="orange">{{ problem.review }}</span> /
        <span class="red">{{ problem.unsolved }}</span>
        <span class="card-count">{{ problem.card_count }}카드</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    problem: Object,
  },
  methods: {
    goToProblem() {
      this.$emit("go-to-problem", this.problem.id);
    },
  },
};
</script>

<style scoped>
.problem-item {
  border: 1px solid #e5d9ff;
  border-radius: 12px;
  padding: 20px;
  background-color: #ffffff;
  box-shadow: 0 0 0 1px #f0eaff;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.item-header {
  display: flex;
  justify-content: space-between;
}

.title {
  font-size: 1.2rem;
  font-weight: 700;
  color: #5a2ca0;
  margin: 0;
}

.author {
  font-size: 0.9rem;
  color: #777;
  margin: 4px 0;
}

.tag {
  display: inline-block;
  background-color: #f0e6ff;
  color: #7a4bb7;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 0.8rem;
}

.status-label {
  background-color: #ffd965;
  padding: 3px 8px;
  border-radius: 8px;
  font-weight: bold;
  color: #6a4c00;
  font-size: 0.85rem;
  margin-right: 10px;
}

.solve-button {
  background-color: #b48fff;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.9rem;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.icons {
  display: flex;
  gap: 12px;
  align-items: center;
  color: #9b59b6;
  font-weight: bold;
}

.progress {
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 3px;
}

.green { color: green; }
.orange { color: orange; }
.red { color: red; }

.card-count {
  margin-left: 6px;
  color: #888;
}
</style>
