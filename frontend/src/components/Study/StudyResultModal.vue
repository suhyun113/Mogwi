<template>
  <div v-if="isVisible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h2>학습 결과</h2>
      <p class="modal-message">오늘 학습한 카드들의 기억 상태입니다.</p>

      <div class="result-breakdown">
        <div class="result-item perfect">
          <span class="label">완벽한 기억:</span>
          <span class="value">{{ perfectCount }}개 ({{ perfectPercentage }}%)</span>
        </div>
        <div class="result-item vague">
          <span class="label">희미한 기억:</span>
          <span class="value">{{ vagueCount }}개 ({{ vaguePercentage }}%)</span>
        </div>
        <div class="result-item forgotten">
          <span class="label">사라진 기억:</span>
          <span class="value">{{ forgottenCount }}개 ({{ forgottenPercentage }}%)</span>
        </div>
        <div class="total-cards">
          총 학습 카드: {{ totalCards }}개
        </div>
      </div>

      <div class="modal-buttons"> <button @click="restartStudy" class="modal-button secondary">다시 학습</button>
        <button @click="goToMyStudy" class="modal-button primary">나의 학습</button>
      </div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'; // Import useRouter for navigation

export default {
  props: {
    isVisible: {
      type: Boolean,
      default: false
    },
    perfectCount: {
      type: Number,
      default: 0
    },
    vagueCount: {
      type: Number,
      default: 0
    },
    forgottenCount: {
      type: Number,
      default: 0
    },
    totalCards: {
      type: Number,
      default: 0
    },
    // You'll need to pass the problemId to the modal for navigation
    problemId: {
      type: [String, Number],
      required: true // Ensure problemId is always provided
    }
  },
  emits: ['close'],
  setup() { // Use setup for useRouter
    const router = useRouter();
    return { router }; // Return router to be accessible in methods
  },
  computed: {
    perfectPercentage() {
      return this.totalCards > 0 ? ((this.perfectCount / this.totalCards) * 100).toFixed(1) : 0;
    },
    vaguePercentage() {
      return this.totalCards > 0 ? ((this.vagueCount / this.totalCards) * 100).toFixed(1) : 0;
    },
    forgottenPercentage() {
      return this.totalCards > 0 ? ((this.forgottenCount / this.totalCards) * 100).toFixed(1) : 0;
    }
  },
  methods: {
    closeModal() {
      this.$emit('close');
    },
    restartStudy() {
      this.$emit('close'); // Close the modal first
      // Navigate back to the StudyView for the current problem
      // This will cause the StudyView to re-fetch cards, essentially restarting
      this.router.push({ name: 'StudyDetail', params: { id: this.problemId } });
    },
    goToMyStudy() {
      this.$emit('close'); // Close the modal first
      // Navigate to MyStudyView. You'll need to define this route in your router/index.js
      // and create the MyStudyView component.
      // Assuming MyStudyView might need the problemId to show specific stats.
      this.router.push({ name: 'MyStudy', params: { id: this.problemId } });
    }
  }
};
</script>

<style scoped>
/* (Existing styles remain the same) */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.modal-content {
  background: white;
  padding: 30px 40px;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  text-align: center;
  max-width: 400px;
  width: 90%;
  box-sizing: border-box;
  animation: fadeInScale 0.3s ease-out forwards;
  position: relative;
}

h2 {
  color: #5a2e87;
  margin-bottom: 20px;
  font-size: 1.8rem;
  font-weight: bold;
}

.modal-message {
  color: #666;
  margin-bottom: 30px;
  font-size: 1.1rem;
}

.result-breakdown {
  margin-bottom: 30px;
  text-align: left;
}

.result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  margin-bottom: 8px;
  border-radius: 8px;
  font-size: 1.05rem;
  font-weight: 500;
  border: 1px solid #eee;
}

.result-item.perfect {
  background-color: #e6ffe6;
  border-color: #a3e8a3;
  color: #28a745;
}
.result-item.vague {
  background-color: #fffde6;
  border-color: #ffe08a;
  color: #ffc107;
}
.result-item.forgotten {
  background-color: #ffe6e6;
  border-color: #ffb3b3;
  color: #dc3545;
}

.result-item .label {
  font-weight: 600;
}

.result-item .value {
  font-weight: bold;
}

.total-cards {
  margin-top: 20px;
  font-size: 1.2rem;
  font-weight: bold;
  color: #333;
  padding-top: 15px;
  border-top: 1px dashed #ddd;
}

.modal-buttons {
  display: flex;
  justify-content: space-between;
  gap: 15px;
  margin-top: 20px;
  width: 100%;
}

.modal-button {
  flex: 1;
  border: none;
  padding: 12px 15px;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  /* Removed 'transform 0.2s ease' from transition */
  transition: background-color 0.2s ease;
  font-weight: 600;
}

.modal-button.primary {
  background-color: #a471ff;
  color: white;
}

.modal-button.primary:hover {
  background-color: #8b5cf6;
  /* Removed transform property */
}

.modal-button.secondary {
  background-color: #f0f0f0;
  color: #555;
  border: 1px solid #ddd;
}

.modal-button.secondary:hover {
  background-color: #e0e0e0;
  /* Removed transform property */
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>