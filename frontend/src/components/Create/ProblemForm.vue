<template>
  <div class="problem-form-container">
    <h2 class="section-title">문제 기본 정보</h2>
    <p class="section-description">문제 제목과 카테고리를 설정하고 공개 여부를 결정합니다.</p>
    <div class="form-group">
      <label for="title">문제 제목 <span class="required">*</span></label>
      <input
        type="text"
        id="title"
        :value="title"
        @input="$emit('update:title', $event.target.value)"
        placeholder="예) Vue.js 기초 개념 마스터"
        class="form-input"
      />
    </div>

    <div class="form-group">
      <label>문제 공개 여부 <span class="required">*</span></label>
      <div class="radio-group">
        <label>
          <input
            type="radio"
            :checked="isPublic === true"
            @change="$emit('update:isPublic', true)"
          />
          공개
        </label>
        <label>
          <input
            type="radio"
            :checked="isPublic === false"
            @change="$emit('update:isPublic', false)"
          />
          비공개
        </label>
      </div>
    </div>

    <div class="form-group tag-selection-group">
      <label>태그 선택 (최대 3개) <span class="required">*</span></label>
      <div class="tags-container">
        <button
          v-for="category in allCategories"
          :key="category.id"
          @click="toggleTag(category.id)"
          :class="['tag-button', { 'selected': localSelectedTags.includes(category.id) }]"
          :disabled="localSelectedTags.length >= 3 && !localSelectedTags.includes(category.id)"
          :style="{
            backgroundColor: localSelectedTags.includes(category.id) ? getColor(category) : '#e0e0e0',
            borderColor: localSelectedTags.includes(category.id) ? getColor(category) : '#e0e0e0',
            color: localSelectedTags.includes(category.id) ? 'white' : '#555'
          }"
        >
          {{ formatTagName(category.tag_name) }}
        </button>
      </div>
      <p v-if="tagError" class="error-text">{{ tagError }}</p>
    </div>

    <div class="form-group">
      <label for="description">문제 설명 (선택)</label>
      <textarea
        id="description"
        :value="description"
        @input="$emit('update:description', $event.target.value)"
        placeholder="이 문제집에 대한 설명을 작성해주세요. (예: 이 문제집은 Vue.js의 기본적인 컴포넌트 개념을 다룹니다.)"
        class="form-textarea"
      ></textarea>
    </div>
  </div>
</template>

<script>
import { ref, watch, computed } from 'vue';

export default {
  // props 정의
  props: {
    title: String,
    isPublic: Boolean,
    selectedTags: Array,
    description: String,
    allCategories: Array,
  },
  // emits 정의 (명시적으로 정의하는 것이 좋습니다)
  emits: [
    'update:title',
    'update:isPublic',
    'update:selectedTags',
    'update:description',
  ],
  setup(props, { emit }) {
    const tagError = ref('');

    const localSelectedTags = computed(() => {
      if (!Array.isArray(props.selectedTags)) return [];
      return props.selectedTags.map(Number);
    });

    // Helper function to format tag names for display (ensuring single #)
    const formatTagName = (tagName) => {
      const cleanedTagName = tagName.startsWith('#') ? tagName.substring(1) : tagName;
      return `#${cleanedTagName}`;
    };

    const getColor = (category) => {
      return category?.color_code || '#ccc';
    };

    const toggleTag = (tagId) => {
      const currentTags = [...localSelectedTags.value];
      const index = currentTags.indexOf(tagId);

      if (index > -1) {
        // 이미 선택된 태그 -> 제거
        currentTags.splice(index, 1);
      } else {
        // 선택되지 않은 태그 -> 추가 (최대 3개 제한)
        if (currentTags.length < 3) {
          currentTags.push(tagId);
        } else {
          tagError.value = '태그는 최대 3개까지 선택할 수 있습니다.';
          return;
        }
      }
      emit('update:selectedTags', currentTags);
      tagError.value = ''; // 에러 메시지 초기화
    };

    // 태그 선택 개수가 변경될 때마다 에러 메시지 초기화
    watch(() => props.selectedTags.length, (newLength) => {
      if (newLength < 3) {
        tagError.value = '';
      }
    });

    // setup 함수에서 정의된 내용은 return 객체에 포함시켜야 템플릿에서 사용 가능
    return {
      tagError,
      toggleTag,
      getColor,
      formatTagName,
      localSelectedTags,
    };
  },
};
</script>

<style scoped>
/* 기존 스타일은 동일하게 유지됩니다. */
.problem-form-container {
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.section-title {
  color: #4a1e77;
  font-size: 1.8rem;
  font-weight: 600;
  margin-bottom: 10px;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
}

.section-description {
  color: #777;
  font-size: 0.95rem;
  margin-bottom: 20px;
  line-height: 1.4;
}

.form-group {
  margin-bottom: 25px;
}

.form-group label {
  display: block;
  font-size: 1.1rem;
  color: #333;
  font-weight: 500;
  margin-bottom: 8px;
}

.form-group .required {
  color: #e74c3c;
  margin-left: 4px;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  color: #555;
  box-sizing: border-box;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.form-input::placeholder, .form-textarea::placeholder {
  color: #aaa;
}

.form-input:focus, .form-textarea:focus {
  border-color: #a471ff;
  box-shadow: 0 0 0 3px rgba(164, 113, 255, 0.2);
  outline: none;
}

.form-textarea {
  min-height: 100px;
  resize: vertical;
}

.radio-group {
  display: flex;
  gap: 20px;
}

.radio-group label {
  display: flex;
  align-items: center;
  font-size: 1rem;
  cursor: pointer;
  color: #555;
}

.radio-group input[type="radio"] {
  appearance: none;
  width: 18px;
  height: 18px;
  border: 2px solid #a471ff;
  border-radius: 50%;
  margin-right: 8px;
  position: relative;
  cursor: pointer;
  outline: none;
  flex-shrink: 0;
}

.radio-group input[type="radio"]:checked {
  background-color: #a471ff;
  border-color: #a471ff;
}

.radio-group input[type="radio"]:checked::before {
  content: '';
  display: block;
  width: 8px;
  height: 8px;
  background-color: white;
  border-radius: 50%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-button {
  border-radius: 14px;
  padding: 4px 10px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  /* 색상 관련 스타일은 인라인에서 직접 관리 */
  background: none;
  border: none;
  color: inherit; /* 텍스트 색상도 인라인에서 제어 */
}

.tag-button.selected {
  /* 선택 시 스타일은 인라인에서 관리 */
}

.tag-button:hover:not(.disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.tag-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-text {
  color: #e74c3c;
  font-size: 0.85rem;
  margin-top: 5px;
}
</style>