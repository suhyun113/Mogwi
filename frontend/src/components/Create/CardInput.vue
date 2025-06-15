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
      <label>이미지 (선택)</label>
      <div
        class="image-upload-area"
        :class="{ 'has-image': card.image_url, 'drag-over': isDragging }"
        @dragover.prevent="handleDragOver"
        @dragleave="handleDragLeave"
        @drop.prevent="handleDrop"
      >
        <input
          type="file"
          :id="`image-input-${card.id}`"
          @change="handleImageFileChange"
          accept="image/*"
          ref="fileInput"
          hidden
        />
        <div v-if="!card.image_url" class="upload-placeholder">
          <span class="icon-upload" aria-hidden="true">&#x2191;</span>
          <p>
            <label :for="`image-input-${card.id}`" class="upload-label">
              클릭하여 파일 선택
            </label>
            또는 이미지를 여기에 드래그하세요.
          </p>
        </div>
        <div v-else class="image-preview-wrapper">
          <img :src="card.image_url" alt="Image Preview" class="preview-img" @error="handleImageError"/>
          <p v-if="imageLoadError" class="image-error-msg">이미지를 불러올 수 없습니다. 파일 형식을 확인하거나 다른 이미지를 시도해주세요.</p>
          <div class="image-actions">
            <button @click="clearImage" class="action-btn clear-image-btn" title="이미지 제거">
              <img src="@/assets/icons/delete.png" alt="Delete" class="btn-icon" />
              <span>제거</span>
            </button>
            <label :for="`image-input-${card.id}`" class="action-btn change-image-btn" title="이미지 변경">
              <img src="@/assets/icons/edit.png" alt="Change" class="btn-icon" />
              <span> 변경</span>
            </label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch } from 'vue';

export default {
  props: {
    card: Object,
    index: Number,
  },
  emits: [
    'update:question',
    'update:answer',
    'update:image_url',
    'update:image_file',
    'remove-card',
  ],
  setup(props, { emit }) {
    const imageLoadError = ref(false);
    const isDragging = ref(false); // For drag and drop visual feedback
    const fileInput = ref(null); // Reference to the hidden file input

    const processFile = (file) => {
      if (file && file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (e) => {
          emit('update:image_url', e.target.result);
          imageLoadError.value = false;
        };
        reader.onerror = () => {
          imageLoadError.value = true;
          emit('update:image_url', null);
          emit('update:image_file', null);
        };
        reader.readAsDataURL(file);

        emit('update:image_file', file);
      } else if (file) {
        imageLoadError.value = true;
        alert('이미지 파일만 업로드할 수 있습니다.');
        emit('update:image_url', null);
        emit('update:image_file', null);
        if (fileInput.value) {
          fileInput.value.value = '';
        }
      } else {
        emit('update:image_url', null);
        emit('update:image_file', null);
        imageLoadError.value = false;
        if (fileInput.value) {
          fileInput.value.value = '';
        }
      }
    };

    const handleImageFileChange = (event) => {
      processFile(event.target.files[0]);
    };

    const handleDragOver = () => {
      isDragging.value = true;
    };

    const handleDragLeave = () => {
      isDragging.value = false;
    };

    const handleDrop = (event) => {
      isDragging.value = false;
      const files = event.dataTransfer.files;
      if (files.length > 0) {
        processFile(files[0]);
      }
    };

    const handleImageError = () => {
      imageLoadError.value = true;
    };

    const clearImage = () => {
      emit('update:image_url', null);
      emit('update:image_file', null);
      imageLoadError.value = false;
      if (fileInput.value) {
        fileInput.value.value = '';
      }
    };

    watch(() => props.card.image_url, (newUrl) => {
      if (newUrl && imageLoadError.value) {
        imageLoadError.value = false;
      }
    });

    return {
      imageLoadError,
      isDragging,
      fileInput,
      handleImageFileChange,
      handleDragOver,
      handleDragLeave,
      handleDrop,
      handleImageError,
      clearImage,
    };
  },
};
</script>

<style scoped>
/* Existing styles */
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

/* --- NEW/MODIFIED STYLES FOR IMAGE UPLOAD UI --- */

.image-upload-area {
  border: 2px dashed #a471ff; /* Primary color dashed border */
  border-radius: 8px;
  background-color: #fcfcfc;
  padding: 25px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s, background-color 0.3s;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 120px; /* Minimum height for the drop area */
  position: relative; /* For absolute positioning of actions */
}

.image-upload-area:hover,
.image-upload-area.drag-over {
  border-color: #6a3e9c; /* Darker on hover/drag */
  background-color: #f0eafc; /* Lighter background on hover/drag */
}

.image-upload-area.has-image {
  border: 1px solid #e0e0e0; /* Solid border when image is present */
  padding: 10px; /* Less padding when image is present */
  min-height: unset; /* Let image content define height */
}

.upload-placeholder {
  color: #888;
  font-size: 0.95rem;
  line-height: 1.5;
}

.icon-upload {
  font-size: 3rem; /* 아이콘 크기 키우기 */
  color: #a471ff; /* 업로드 아이콘 색상 */
  margin-bottom: 10px;
  display: block; /* 블록 요소로 만들어 margin-bottom 적용 */
  font-weight: 300; /* 얇은 폰트 웨이트로 가벼운 느낌 */
}

.upload-label {
  color: #6a3e9c; /* Primary color for clickable text */
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  transition: color 0.2s;
}

.upload-label:hover {
  color: #a471ff; /* Lighter primary color on hover */
}

.image-preview-wrapper {
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview-img {
  max-width: 100%;
  height: auto;
  border-radius: 5px;
  max-height: 200px; /* Limit preview height */
  object-fit: contain;
  border: 1px solid #eee;
}

.image-error-msg {
  color: #e74c3c;
  font-size: 0.85rem;
  margin-top: 10px;
}

.image-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  width: 40%; /* Further reduce container width from 60% to 40% */
  justify-content: center;
  margin-left: auto;
  margin-right: auto;
}

/* Common style for action buttons */
.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 5px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.2s;
  border: none;
  color: #333;
  font-weight: 500;
  gap: 8px; /* Set gap to 8px for spacing between icon and text */
  flex-grow: 1;
  height: 36px; /* Fixed height for consistent button size */
  box-sizing: border-box; /* Include padding and border in the element's total width and height */
  max-width: 90px; /* Limit button width as previously requested */
}

.action-btn .btn-icon {
  width: 18px; /* Icon size */
  height: 18px;
  vertical-align: middle;
}

/* Specific styles for clear and change buttons with softer colors */
.clear-image-btn {
  background-color: #ffe0e0;
  color: #c0392b;
  border: 1px solid #ffc8c8;
  height: 36px; /* Ensure height consistency */
  gap: 5px; /* Reduce gap specifically for clear button */
}

.clear-image-btn:hover {
  background-color: #ffcccc;
  transform: translateY(-1px);
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}

.change-image-btn {
  background-color: #e0e6ff;
  color: #4a5c9e;
  border: 1px solid #c8d3ff;
  height: 36px; /* Ensure height consistency */
}

.change-image-btn:hover {
  background-color: #ccd6ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}

/* Remove default file input appearance */
.form-input-file {
  display: none;
}
</style>