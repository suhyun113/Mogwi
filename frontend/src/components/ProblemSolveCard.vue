<template>
  <div class="card" :class="getCardStatusClass(selectedStatus)">
    <div class="card-content with-image">
      <h3 class="card-question">{{ problem.question }}</h3>

      <div class="image-wrapper">
        <template v-if="!problem.imageUrl">
          <div class="speech-bubble">정답을 맞춰봐!</div>
          <img :src="require('@/assets/mogwi-character.png')" class="problem-image" alt="모귀 캐릭터" />
        </template>
        <template v-else>
          <img :src="problem.imageUrl" class="problem-image" alt="문제 이미지" />
        </template>
      </div>

      <select
        v-model="selectedStatus"
        class="status-dropdown"
        :disabled="!hasSubmitted"
        @change="onStatusChange"
      >
        <option value="new" disabled>새로운 기억</option>
        <option value="perfect">완벽한 기억</option>
        <option value="vague">희미한 기억</option>
        <option value="forgotten">사라진 기억</option>
      </select>
    </div>
  </div>
</template>

<script>
export default {
  // 컴포넌트 이름 (선택 사항이지만 권장됨)
  name: 'ProblemSolveCard', 
  
  // props 선언 (Composition API의 defineProps와 동일)
  props: {
    problem: { type: Object, required: true },
    showAnswer: { type: Boolean, default: false },
    hasSubmitted: { type: Boolean, default: false }
  },

  // 컴포넌트의 반응형 데이터 (Composition API의 ref와 동일)
  data() {
    return {
      selectedStatus: 'new'
    };
  },

  // 컴포넌트가 마운트될 때 실행되는 로직 (Composition API의 onMounted와 동일)
  mounted() {
    this.selectedStatus = this.problem.cardStatus || 'new';
  },

  // props나 data의 변화를 감지하여 실행되는 로직 (Composition API의 watch와 동일)
  watch: {
    'problem.cardStatus': {
      handler(newStatus) {
        if (newStatus) {
          this.selectedStatus = newStatus;
        }
      },
      // 컴포넌트가 생성될 때 즉시 실행
      immediate: true 
    }
  },

  // 계산된 속성
  computed: {
    // 여기에 필요한 computed 속성을 추가할 수 있습니다.
  },

  // 메서드 선언 (Composition API에서 일반 함수 선언과 동일)
  methods: {
    getCardStatusClass(status) {
      if (status === 'perfect') return 'card-perfect';
      if (status === 'vague') return 'card-vague';
      if (status === 'forgotten') return 'card-forgotten';
      if (status === 'new') return 'card-new';
      return '';
    },
    // 이벤트를 발생시키는 메서드 (Composition API의 emit과 동일)
    onStatusChange() {
      // 'status-changed' 이벤트를 발생시키고 현재 selectedStatus 값을 전달
      this.$emit('status-changed', this.selectedStatus);
    }
    // 'toggle-show-answer' 이벤트는 부모 컴포넌트에서 직접 호출할 수 있으므로,
    // 필요하다면 이곳에 메서드를 추가하거나 부모에서 직접 처리할 수 있습니다.
    // 예를 들어, 카드 내부에 "정답 보기" 버튼이 있다면
    // toggleShowAnswer() {
    //   this.$emit('toggle-show-answer');
    // }
  }
};
</script>

<style scoped>
.card {
  background: white;
  border: 3px solid #a471ff;
  border-radius: 0.75rem;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  padding: 1rem;
  position: relative;
  width: 100%;
  box-sizing: border-box;
  min-height: 400px;
  transition: border-color 0.3s ease;
}

.card-perfect { border-color: #28a745; }
.card-vague { border-color: #ffc107; }
.card-forgotten { border-color: #dc3545; }
.card-new { border-color: #a471ff; }

.card-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: space-between;
  margin-top: 20px;
}

.card-question {
  font-size: 1.4rem;
  font-weight: bold;
  color: #333;
  line-height: 1.4;
  word-break: keep-all;
  margin-bottom: 10px;
  padding: 5px;
}

.image-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: auto;
}

.problem-image {
  width: 90px;
  height: auto;
  border-radius: 8px;
  object-fit: contain;
  margin: 8px 0;
}

.speech-bubble {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
  background: #f3f4f6;
  padding: 0.75rem 1rem;
  border-radius: 1rem;
  position: relative;
  max-width: 200px;
  text-align: center;
  margin-bottom: 6px;
}

.speech-bubble::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid #f3f4f6;
}

.status-dropdown {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 0.9rem;
  font-weight: bold;
  border-radius: 0.75rem;
  padding: 5px 12px;
  background-color: #f0e6ff;
  color: #5a2e87;
  border: none;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  cursor: pointer;
}
</style>