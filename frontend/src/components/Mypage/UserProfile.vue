<template>
  <section class="mypage-section user-profile-section">
    <div class="profile-header">
      <h2 class="section-title">{{ nickname }} 님 안녕하세요.</h2>
      <button @click="$emit('edit-info')" class="edit-info-button">
        <img src="@/assets/icons/edit.png" alt="수정" class="edit-icon" /> 수정
      </button>
    </div>
  </section>
</template>

<script>
import { ref, watch } from 'vue';

export default {
  name: 'UserProfile',
  props: {
    nickname: {
      type: String,
      required: true,
    },
  },
  emits: ['update-nickname', 'edit-info'],
  setup(props, { emit }) {
    const isEditingNickname = ref(false);
    const newNickname = ref('');

    // props.nickname이 변경될 때마다 newNickname을 초기화 (외부에서 닉네임이 변경될 경우 대비)
    watch(() => props.nickname, (val) => {
      newNickname.value = val;
    }, { immediate: true });

    const startEditingNickname = () => {
      newNickname.value = props.nickname; // 현재 닉네임을 편집 필드에 로드
      isEditingNickname.value = true;
    };

    const saveNickname = () => {
      if (newNickname.value.trim() && newNickname.value !== props.nickname) {
        emit('update-nickname', newNickname.value.trim());
      }
      isEditingNickname.value = false;
    };

    const cancelEditingNickname = () => {
      isEditingNickname.value = false;
      newNickname.value = props.nickname; // 취소 시 원래 닉네임으로 되돌림
    };

    return {
      isEditingNickname,
      newNickname,
      startEditingNickname,
      saveNickname,
      cancelEditingNickname,
    };
  },
};
</script>

<style scoped>
.user-profile-section {
  background-color: #fff;
  border: 1px solid #e5d8f7;
  border-radius: 10px;
  padding: 50px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 30px;
  max-width: 1400px;
  width: 100%;
  box-sizing: border-box;
  margin-left: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 100px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  color: #5a2e87;
  font-size: 1.8rem;
  font-weight: 600;
  margin: 0;
}

.edit-info-button {
  background-color: #f3eaff;
  color: #6a2dbd;
  border: 1.5px solid #e0d0ff;
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, box-shadow 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.2;
  box-shadow: 0 2px 8px rgba(106, 45, 189, 0.06);
  outline: none;
}

.edit-info-button:hover, .edit-info-button:focus {
  background-color: #e6dbff;
  color: #4a1e77;
  box-shadow: 0 4px 16px rgba(106, 45, 189, 0.10);
  border-color: #cbb6f7;
}

.edit-icon {
  width: 18px;
  height: 18px;
  display: inline-block;
  vertical-align: middle;
}

.profile-content {
  text-align: left;
}

.profile-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  font-size: 1.1rem;
  color: #4a1e77;
  background-color: #fff;
  padding: 10px 15px;
  border-radius: 8px;
  border: 1px solid #e9dffc;
}

.profile-item:last-child {
  margin-bottom: 0;
}

.label {
  font-weight: 600;
  margin-right: 10px;
  color: #6a3d9a;
  min-width: 80px;
}

.value {
  flex-grow: 1;
  font-weight: 500;
}

.nickname-display {
  justify-content: space-between;
}

.edit-nickname-button {
  background: none;
  border: none;
  color: #8c5dff;
  font-size: 1.1rem;
  cursor: pointer;
  margin-left: 10px;
  padding: 5px;
  transition: color 0.2s ease, transform 0.2s ease;
}

.edit-nickname-button:hover {
  color: #6a3d9a;
  transform: scale(1.1);
}

.nickname-edit {
  justify-content: flex-start;
  gap: 10px;
  flex-wrap: wrap; /* 모바일에서 버튼이 줄바꿈되도록 */
}

.nickname-input {
  flex-grow: 1;
  padding: 8px 12px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 1rem;
  max-width: 250px; /* 입력 필드 최대 너비 제한 */
}

.save-nickname-button, .cancel-nickname-button {
  padding: 8px 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease;
  font-size: 0.95rem;
  font-weight: 500;
}

.save-nickname-button {
  background-color: #6a3d9a;
  color: white;
  border: none;
}

.save-nickname-button:hover {
  background-color: #4a1e77;
  transform: translateY(-1px);
}

.cancel-nickname-button {
  background-color: #f0f0f0;
  color: #555;
  border: 1px solid #ddd;
}

.cancel-nickname-button:hover {
  background-color: #e0e0e0;
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .user-profile-section {
    padding: 20px;
  }
  .section-title {
    font-size: 1.5rem;
  }
  .edit-info-button {
    padding: 6px 12px;
    font-size: 0.85rem;
  }
  .profile-item {
    font-size: 1rem;
    padding: 8px 12px;
    flex-wrap: wrap;
  }
  .label {
    min-width: 60px;
    margin-bottom: 5px; /* 모바일에서 줄바꿈 시 간격 */
  }
  .nickname-edit {
    flex-direction: column;
    align-items: flex-start;
  }
  .nickname-input {
    width: 100%;
    max-width: none;
    margin-bottom: 10px;
  }
  .save-nickname-button, .cancel-nickname-button {
    width: 100%;
    margin-bottom: 5px;
  }
}
</style>