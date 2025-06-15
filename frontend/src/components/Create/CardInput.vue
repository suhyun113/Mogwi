<template>
  <div class="card-input-container">
    <div class="card-header">
      <h3 class="card-title">카드 {{ index + 1 }}</h3>
      <button @click="$emit('remove-card')" class="remove-card-btn" title="카드 삭제">
        <img src="@/assets/icons/delete.png" alt="Delete" class="delete-icon" />
      </button>
    </div>
    <div class="form-group">
      <label :for="`question-${card.id}`">질문 <span class="required">*</span></label>
      <textarea
        :id="`question-${card.id}`"
        :value="card.question"
        @input="$emit('update:question', $event.target.value)"
        placeholder="질문을 입력하세요."
        class="form-textarea"
      ></textarea>
    </div>
    <div class="form-group">
      <label :for="`answer-${card.id}`">정답 <span class="required">*</span></label>
      <textarea
        :id="`answer-${card.id}`"
        :value="card.answer"
        @input="$emit('update:answer', $event.target.value)"
        placeholder="정답을 입력하세요."
        class="form-textarea"
      ></textarea>
    </div>
    <div class="form-group">
      <label :for="`image-${card.id}`">이미지 URL (선택)</label>
      <input
        type="url"
        :id="`image-${card.id}`"
        :value="card.image_url"
        @input="$emit('update:image_url', $event.target.value)"
        placeholder="이미지 URL을 입력하세요 (예: https://example.com/image.jpg)"
        class="form-input"
      />
      <div v-if="card.image_url" class="image-preview">
        <img :src="card.image_url" alt="Image Preview" class="preview-img" @error="handleImageError"/>
        <p v-if="imageLoadError" class="image-error-msg">이미지를 불러올 수 없습니다. URL을 확인해주세요.</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch } from 'vue';

export default {
  // props 정의
  props: {
    card: Object,
    index: Number,
  },
  // emits 정의 (명시적으로 정의하는 것이 좋습니다)
  emits: [
    'update:question',
    'update:answer',
    'update:image_url',
    'remove-card',
  ],
  setup(props) { // setup 함수에서 props를 인자로 받습니다. emit은 $emit으로 직접 호출합니다.
    const imageLoadError = ref(false);

    const handleImageError = () => {
      imageLoadError.value = true;
    };

    // Reset imageLoadError when image_url changes
    watch(() => props.card.image_url, (newUrl) => {
      if (newUrl) {
        imageLoadError.value = false;
      }
    });

    // 템플릿에 노출할 데이터와 함수 반환
    return {
      imageLoadError,
      handleImageError,
    };
  },
};
</script>

<style scoped>
/* 기존 스타일은 동일하게 유지됩니다. */
.card-input-container {
  background-color: #ffffff;
  border: 1px solid #dcdcdc;
  border-radius: 10px;
  padding: 25px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  transition: border-color 0.3s;
}

.card-input-container:focus-within {
  border-color: #a471ff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eee;
}

.card-title {
  font-size: 1.4rem;
  color: #6a3e9c;
  font-weight: 600;
  margin: 0;
}

.remove-card-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px;
  transition: transform 0.2s;
  opacity: 0.7;
}

.remove-card-btn:hover {
  transform: scale(1.1);
  opacity: 1;
}

.delete-icon {
  width: 28px;
  height: 28px;
  vertical-align: middle;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 1rem;
  color: #444;
  font-weight: 500;
  margin-bottom: 6px;
}

.form-group .required {
  color: #e74c3c;
  margin-left: 4px;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 0.95rem;
  color: #555;
  box-sizing: border-box;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.form-input::placeholder, .form-textarea::placeholder {
  color: #bbb;
}

.form-input:focus, .form-textarea:focus {
  border-color: #a471ff;
  box-shadow: 0 0 0 3px rgba(164, 113, 255, 0.15);
  outline: none;
}

.form-textarea {
  min-height: 80px;
  resize: vertical;
}

.image-preview {
  margin-top: 15px;
  text-align: center;
  border: 1px dashed #ccc;
  padding: 10px;
  border-radius: 8px;
  background-color: #fcfcfc;
}

.preview-img {
  max-width: 100%;
  height: auto;
  border-radius: 5px;
  max-height: 200px;
  object-fit: contain;
  border: 1px solid #eee;
}

.image-error-msg {
  color: #e74c3c;
  font-size: 0.85rem;
  margin-top: 10px;
}
</style>