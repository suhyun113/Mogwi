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
      <label :for="`image-${card.id}`">이미지 (선택)</label>
      <input
        type="file"
        :id="`image-${card.id}`"
        @change="handleImageFileChange"
        accept="image/*"
        class="form-input-file"
      />
      <button v-if="card.image_url" @click="clearImage" class="clear-image-btn">
        이미지 제거
      </button>

      <div v-if="card.image_url" class="image-preview">
        <img :src="card.image_url" alt="Image Preview" class="preview-img" @error="handleImageError"/>
        <p v-if="imageLoadError" class="image-error-msg">이미지를 불러올 수 없습니다. 파일 형식을 확인하거나 다른 이미지를 시도해주세요.</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch } from 'vue';

export default {
  props: {
    card: Object, // card should now also accept a local file preview URL or the final remote URL
    index: Number,
  },
  emits: [
    'update:question',
    'update:answer',
    'update:image_url', // This will now receive a Data URL for preview, or null for clear
    'update:image_file', // NEW: Emit the File object for actual upload
    'remove-card',
  ],
  setup(props, { emit }) {
    const imageLoadError = ref(false);

    const handleImageFileChange = (event) => {
      const file = event.target.files[0];
      if (file) {
        // Read file for immediate local preview (Data URL)
        const reader = new FileReader();
        reader.onload = (e) => {
          emit('update:image_url', e.target.result); // For immediate preview
          imageLoadError.value = false; // Reset error on new file selection
        };
        reader.onerror = () => {
          imageLoadError.value = true;
          emit('update:image_url', null); // Clear URL on error
          emit('update:image_file', null); // Clear file on error
        };
        reader.readAsDataURL(file);

        // Emit the File object to the parent for actual upload later
        emit('update:image_file', file);
      } else {
        emit('update:image_url', null); // Clear preview
        emit('update:image_file', null); // Clear file
        imageLoadError.value = false; // Reset error
      }
    };

    const handleImageError = () => {
      imageLoadError.value = true;
    };

    const clearImage = () => {
      // Clear the displayed image and the internal file reference
      emit('update:image_url', null);
      emit('update:image_file', null);
      imageLoadError.value = false;
      // Optionally, clear the file input visually
      const fileInput = document.getElementById(`image-${props.card.id}`);
      if (fileInput) {
        fileInput.value = '';
      }
    };

    // Reset imageLoadError when the card's image_url prop changes from parent
    watch(() => props.card.image_url, (newUrl) => {
      // Only reset if a new non-null URL is provided, indicating a successful upload from parent
      if (newUrl && imageLoadError.value) {
        imageLoadError.value = false;
      }
    });

    return {
      imageLoadError,
      handleImageFileChange,
      handleImageError,
      clearImage,
    };
  },
};
</script>

<style scoped>
/* 기존 스타일 유지 */
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

.form-input-file { /* New style for file input */
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 0.95rem;
  color: #555;
  box-sizing: border-box;
  transition: border-color 0.3s, box-shadow 0.3s;
  background-color: #f8f8f8;
  cursor: pointer;
}

.form-input-file:focus {
  border-color: #a471ff;
  box-shadow: 0 0 0 3px rgba(164, 113, 255, 0.15);
  outline: none;
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

.clear-image-btn {
  background-color: #f4e8f9;
  color: #5a2e87;
  border: 1px solid #a471ff;
  padding: 8px 15px;
  border-radius: 5px;
  font-size: 0.9rem;
  cursor: pointer;
  margin-top: 10px;
  transition: background-color 0.2s;
}

.clear-image-btn:hover {
  background-color: #e6d7f0;
}
</style>