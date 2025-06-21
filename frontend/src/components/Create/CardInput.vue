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
        @click="triggerFileInput" @dragover.prevent="handleDragOver"
        @dragleave="handleDragLeave"
        @drop.prevent="handleDrop"
      >
        <input
          type="file"
          :id="`image-input-${card.id}`"
          @change="handleImageFileChange"
          accept="image/*"
          ref="fileInput" hidden
        />
        <div v-if="!card.image_url" class="upload-placeholder">
          <span class="icon-upload" aria-hidden="true">&#x2191;</span>
          <p>
            <label class="upload-label">
              클릭하여 파일 선택
            </label>
            또는 이미지를 여기에 드래그하세요.
          </p>
        </div>
        <div v-else class="image-preview-wrapper">
          <img :src="getFullImageUrl(card.image_url)" alt="Image Preview" class="preview-img" @error="handleImageError"/>
          <p v-if="imageLoadError" class="image-error-msg">이미지를 불러올 수 없습니다. 파일 형식을 확인하거나 다른 이미지를 시도해주세요.</p>
          <div class="image-actions">
            <button @click.stop="clearImage" class="action-btn clear-image-btn" title="이미지 제거">
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
    const fileInput = ref(null); // template의 ref="fileInput"과 연결됨

    const getFullImageUrl = (relativeUrl) => {
      if (!relativeUrl) return '';
      // Check if it's already a full URL (http/https) or a Data URL (Base64)
      if (relativeUrl.startsWith('http') || relativeUrl.startsWith('data:')) return relativeUrl;
      // Otherwise, prepend the backend address
      return `http://localhost:8000${relativeUrl}`; // Adjust to your actual backend address
    };

    const processFile = (file) => {
      if (file && file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (e) => {
          emit('update:image_url', e.target.result); // Emits a Base64 URL for immediate preview
          imageLoadError.value = false;
        };
        reader.onerror = () => {
          imageLoadError.value = true;
          emit('update:image_url', null);
          emit('update:image_file', null);
        };
        reader.readAsDataURL(file); // Read the file as a Data URL

        emit('update:image_file', file); // Emit the actual File object for later upload
      } else if (file) {
        imageLoadError.value = true;
        alert('이미지 파일만 업로드할 수 있습니다.');
        emit('update:image_url', null);
        emit('update:image_file', null);
        if (fileInput.value) {
          fileInput.value.value = ''; // Clear the file input
        }
      } else {
        // No file provided or file cleared
        emit('update:image_url', null);
        emit('update:image_file', null);
        imageLoadError.value = false;
        if (fileInput.value) {
          fileInput.value.value = '';
        }
      }
    };

    const handleImageFileChange = (event) => {
      processFile(event.target.files?.[0]);
    };

    const handleDragOver = () => {
      isDragging.value = true;
    };

    const handleDragLeave = () => {
      isDragging.value = false;
    };

    const handleDrop = (event) => {
      isDragging.value = false;
      const files = event.dataTransfer?.files;
      if (files?.length > 0) {
        processFile(files?.[0]);
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
        fileInput.value.value = ''; // Clear the file input
      }
    };

    // 이미지 업로드 영역을 클릭했을 때 hidden input을 클릭하는 함수
    const triggerFileInput = () => {
      // 이미지가 없을 때만 파일 선택 창을 띄웁니다.
      // 이미지가 있으면, 해당 영역 클릭은 이미지 액션 버튼(제거, 변경)으로 처리됩니다.
      if (!props.card.image_url && fileInput.value) {
        fileInput.value.click();
      }
    };

    watch(() => props.card.image_url, (newUrl) => {
      if (newUrl && imageLoadError.value) {
        imageLoadError.value = false; // Reset error when a new image URL is set
      }
    });

    return {
      imageLoadError,
      isDragging,
      fileInput, // template에서 참조할 수 있도록 반환
      handleImageFileChange,
      handleDragOver,
      handleDragLeave,
      handleDrop,
      handleImageError,
      clearImage,
      getFullImageUrl,
      triggerFileInput // template에서 사용할 수 있도록 반환
    };
  },
};
</script>

<style scoped>
/* 기존 스타일은 변경 사항이 없습니다. */
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

/* --- 이미지 업로드 UI 관련 스타일 --- */

.image-upload-area {
  border: 2px dashed #a471ff;
  border-radius: 8px;
  background-color: #fcfcfc;
  padding: 25px;
  text-align: center;
  cursor: pointer; /* 마우스 커서를 포인터로 변경하여 클릭 가능함을 시각적으로 알림 */
  transition: border-color 0.3s, background-color 0.3s;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 120px;
  position: relative;
}

.image-upload-area:hover,
.image-upload-area.drag-over {
  border-color: #6a3e9c;
  background-color: #f0eafc;
}

.image-upload-area.has-image {
  border: 1px solid #e0e0e0;
  padding: 10px;
  min-height: unset;
}

.upload-placeholder {
  color: #888;
  font-size: 0.95rem;
  line-height: 1.5;
}

.icon-upload {
  font-size: 3rem;
  color: #a471ff;
  margin-bottom: 10px;
  display: block;
  font-weight: 300;
}

.upload-label {
  color: #6a3e9c;
  font-weight: 600;
  /* label 자체의 cursor는 필요 없습니다. 부모 div가 처리합니다. */
  /* cursor: pointer; */
  text-decoration: underline;
  transition: color 0.2s;
}

.upload-label:hover {
  color: #a471ff;
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
  max-height: 200px;
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
  width: 40%;
  justify-content: center;
  margin-left: auto;
  margin-right: auto;
}

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
  gap: 8px;
  flex-grow: 1;
  height: 36px;
  box-sizing: border-box;
  max-width: 90px;
}

.action-btn .btn-icon {
  width: 18px;
  height: 18px;
  vertical-align: middle;
}

.clear-image-btn {
  background-color: #ffe0e0;
  color: #c0392b;
  border: 1px solid #ffc8c8;
  height: 36px;
  gap: 5px;
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
  height: 36px;
}

.change-image-btn:hover {
  background-color: #ccd6ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}

.form-input-file {
  display: none;
}
</style>