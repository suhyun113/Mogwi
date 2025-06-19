<template>
  <section class="mypage-section user-profile-section">
    <div class="profile-header">
      <h2 class="section-title">{{ nickname }} 님 안녕하세요.</h2>
      <!-- 정보 수정 버튼 클릭 시 모달 열기 -->
      <button @click="$emit('edit-info')" class="edit-info-button">
        <img src="@/assets/icons/edit.png" alt="수정" class="edit-icon" /> 수정
      </button>
    </div>

    <!-- 정보 수정 모달 컴포넌트 -->
    <EditProfileModal
      v-if="showEditProfileModal"
      :initial-nickname="nickname"
      :initial-email="userEmail"
      @close="closeEditProfileModal"
      @update-profile="handleProfileUpdate"
    />
  </section>
</template>

<script>
import { ref, watch } from 'vue';
import EditProfileModal from './EditProfileModal.vue'; // 새로운 모달 컴포넌트 임포트

export default {
  name: 'UserProfile',
  components: {
    EditProfileModal, // 컴포넌트 등록
  },
  props: {
    nickname: {
      type: String,
      required: true,
    },
    // UserProfile에서 이메일 정보를 받아서 표시할 수 있도록 prop 추가
    userEmail: {
      type: String,
      default: null,
    }
  },
  emits: ['update-nickname', 'edit-info'], // edit-info는 이제 직접 모달을 띄우므로 필요 없을 수 있음 (선택적)
  setup(props, { emit }) {
    const isEditingNickname = ref(false);
    const newNickname = ref('');
    const showEditProfileModal = ref(false); // 정보 수정 모달 표시 여부

    // props.nickname이 변경될 때마다 newNickname을 초기화 (외부에서 닉네임이 변경될 경우 대비)
    watch(() => props.nickname, (val) => {
      newNickname.value = val;
    }, { immediate: true });

    // 닉네임 편집 시작
    const startEditingNickname = () => {
      newNickname.value = props.nickname;
      isEditingNickname.value = true;
    };

    // 닉네임 저장
    const saveNickname = () => {
      if (newNickname.value.trim() && newNickname.value !== props.nickname) {
        emit('update-nickname', newNickname.value.trim());
      }
      isEditingNickname.value = false;
    };

    // 닉네임 편집 취소
    const cancelEditingNickname = () => {
      isEditingNickname.value = false;
      newNickname.value = props.nickname;
    };

    // 정보 수정 모달 열기
    const openEditProfileModal = () => {
      showEditProfileModal.value = true;
      // 기존 emit('edit-info')는 이제 모달이 직접 팝업되므로 필요 없을 수 있습니다.
      // 필요하다면 부모에게 알림용으로 유지할 수 있습니다.
      emit('edit-info'); // MypageView에 alert 메시지를 띄우기 위해 유지
    };

    // 정보 수정 모달 닫기
    const closeEditProfileModal = () => {
      showEditProfileModal.value = false;
    };

    // 모달에서 프로필 업데이트 이벤트 수신
    const handleProfileUpdate = (updatedData) => {
      // 여기서는 UserProfile 컴포넌트의 닉네임만 업데이트
      // 실제로는 MypageView로 이벤트를 다시 올려서 전체 데이터를 새로고침하거나
      // Vuex 스토어에서 사용자 정보를 업데이트해야 합니다.
      if (updatedData.nickname) {
        emit('update-nickname', updatedData.nickname); // 닉네임 업데이트 이벤트 발생
      }
      // 이메일이나 다른 정보는 MypageView에서 다시 fetchMypageData()를 통해 갱신
      closeEditProfileModal();
    };

    return {
      isEditingNickname,
      newNickname,
      showEditProfileModal,
      startEditingNickname,
      saveNickname,
      cancelEditingNickname,
      openEditProfileModal,
      closeEditProfileModal,
      handleProfileUpdate,
    };
  },
};
</script>

<style scoped>
/* 기존 스타일 유지 (MypageView.vue의 스타일과 연동) */
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
  /* margin-left는 MypageView의 main-content 패딩으로 충분하므로 제거 */
  margin-left: 0; 
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
  padding-top: 10px; /* 닉네임/이메일 정보 섹션 상단 여백 */
}

.profile-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  font-size: 1.1rem;
  color: #4a1e77;
  background-color: #fcfaff; /* 더 밝은 배경색으로 구분 */
  padding: 12px 18px; /* 패딩 조정 */
  border-radius: 8px;
  border: 1px solid #e9dffc;
}

.profile-item:last-child {
  margin-bottom: 0;
}

.label {
  font-weight: 600;
  margin-right: 15px; /* 라벨과 값 사이 간격 증가 */
  color: #6a3d9a;
  min-width: 80px;
}

.value {
  flex-grow: 1;
  font-weight: 500;
  color: #333;
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
  flex-wrap: wrap;
  flex-grow: 1; /* 입력 필드와 버튼이 공간을 채우도록 */
}

.nickname-input {
  flex-grow: 1;
  padding: 8px 12px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 1rem;
  max-width: 250px;
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
    margin-bottom: 5px;
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
