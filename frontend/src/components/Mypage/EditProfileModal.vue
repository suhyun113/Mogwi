<template>
  <div class="overlay" @click.self="closeModal">
    <div class="modal">
      <div class="modal-header">
        <h2>정보 수정</h2>
        <button class="close-btn" @click="closeModal">×</button>
      </div>
      <form @submit.prevent="saveProfile">
        <!-- 프로필 이미지 영역 제거 -->
        <!-- <div class="profile-image-container">
          <img src="@/assets/icons/default-profile.png" alt="프로필 이미지" class="profile-image" />
          <div class="camera-icon">
            <i class="fas fa-camera"></i>
          </div>
        </div> -->

        <!-- 이메일 입력 필드 -->
        <div class="input-group">
          <label for="email" class="input-label">이메일</label>
          <input type="email" id="email" v-model="form.email" placeholder="이메일 주소" disabled class="disabled-input"/>
        </div>

        <!-- 현재 비밀번호 입력 필드 -->
        <div class="input-group">
          <label for="current-password" class="input-label">현재 비밀번호</label>
          <input type="password" id="current-password" v-model="form.currentPassword" placeholder="현재 비밀번호 입력" />
        </div>

        <!-- 새 비밀번호 입력 필드 -->
        <div class="input-group">
          <label for="new-password" class="input-label">새 비밀번호 (변경 시)</label>
          <input type="password" id="new-password" v-model="form.newPassword" placeholder="새 비밀번호 (변경 시 입력)" />
        </div>

        <!-- 새 비밀번호 확인 필드 -->
        <div class="input-group">
          <label for="confirm-new-password" class="input-label">새 비밀번호 확인</label>
          <input type="password" id="confirm-new-password" v-model="form.confirmNewPassword" placeholder="새 비밀번호 확인" />
        </div>

        <!-- 닉네임/이름 입력 필드 -->
        <div class="input-group">
          <label for="nickname" class="input-label required-label">이름</label>
          <input type="text" id="nickname" v-model="form.nickname" placeholder="닉네임" />
        </div>

        <div class="error-space">
          <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>
        </div>

        <button type="submit" class="confirm-button">확인</button>
      </form>
    </div>
  </div>
</template>

<script>
import { ref, reactive, watch } from 'vue';

export default {
  name: 'EditProfileModal',
  props: {
    initialNickname: {
      type: String,
      default: '',
    },
    initialEmail: {
      type: String,
      default: '',
    },
  },
  emits: ['close', 'update-profile'],
  setup(props, { emit }) {
    const form = reactive({
      email: props.initialEmail,
      currentPassword: '',
      newPassword: '',
      confirmNewPassword: '',
      nickname: props.initialNickname,
    });

    const errorMessage = ref('');

    // props.initialEmail 또는 initialNickname이 변경될 때마다 폼 데이터 업데이트
    watch(() => props.initialEmail, (newEmail) => {
      form.email = newEmail;
    });
    watch(() => props.initialNickname, (newNickname) => {
      form.nickname = newNickname;
    });

    const closeModal = () => {
      emit('close');
    };

    const saveProfile = () => {
      errorMessage.value = '';

      // 닉네임 필수 검사
      if (!form.nickname.trim()) {
        errorMessage.value = '닉네임은 필수 입력입니다.';
        return;
      }

      // 새 비밀번호 입력 시 확인 로직
      if (form.newPassword) {
        if (form.newPassword.length < 6) { // 최소 길이
            errorMessage.value = '새 비밀번호는 최소 6자 이상이어야 합니다.';
            return;
        }
        if (form.newPassword !== form.confirmNewPassword) {
          errorMessage.value = '새 비밀번호와 비밀번호 확인이 일치하지 않습니다.';
          return;
        }
        if (!form.currentPassword) {
          errorMessage.value = '비밀번호를 변경하려면 현재 비밀번호를 입력해야 합니다.';
          return;
        }
      } else if (form.currentPassword || form.confirmNewPassword) {
          // 새 비밀번호가 없는데 현재 비밀번호나 확인이 채워져 있으면 오류
          errorMessage.value = '새 비밀번호를 입력하지 않으려면, 현재 비밀번호 및 확인 필드를 비워두세요.';
          return;
      }

      // TODO: 실제 API 호출 로직 구현
      // axios.put(`/api/user/profile`, form)
      //   .then(response => {
      //     if (response.data.status === 'success') {
      //       emit('update-profile', { nickname: form.nickname, email: form.email }); // 부모 컴포넌트에 업데이트 알림
      //       closeModal();
      //     } else {
      //       errorMessage.value = response.data.message || '프로필 업데이트 실패.';
      //     }
      //   })
      //   .catch(error => {
      //     console.error('프로필 업데이트 오류:', error);
      //     errorMessage.value = '서버 오류가 발생했습니다. 다시 시도해주세요.';
      //   });

      // 임시 성공 처리 (실제 API 호출 대체)
      console.log('Profile update data:', form);
      // 실제 업데이트 로직 대신 임시 성공 메시지
      emit('update-profile', { nickname: form.nickname, email: form.email }); // 부모에게 업데이트 알림
      closeModal();
    };

    return {
      form,
      errorMessage,
      closeModal,
      saveProfile,
    };
  },
};
</script>

<style scoped>
/* LoginModal.vue 스타일 참고하여 변경 */
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  font-family: 'Inter', 'Pretendard', sans-serif;
}

.modal {
  background: white;
  padding: 2.5rem 2rem; /* 패딩 조정 */
  width: 100%;
  max-width: 340px; /* 너비 조정 */
  border-radius: 0; /* 각진 모서리 */
  position: relative;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
  animation: popup 0.2s ease-out;
  display: flex;
  flex-direction: column;
  align-items: center; /* 중앙 정렬 */
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 1.5rem; /* 이미지 제거 후 간격 조정 */
}

.modal-header h2 {
  font-size: 1.8rem; /* 제목 폰트 크기 조정 */
  font-weight: 700;
  color: #333;
  margin: 0;
}

.close-btn {
  font-size: 2rem; /* 닫기 버튼 크기 조정 */
  background: none;
  border: none;
  cursor: pointer;
  color: #999; /* 색상 조정 */
  transition: color 0.2s ease;
}

.close-btn:hover {
  color: #666;
}

@keyframes popup {
  0% { transform: scale(0.9); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

form {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

/* 프로필 이미지 관련 스타일 제거 */
/* .profile-image-container { ... } */
/* .profile-image { ... } */
/* .camera-icon { ... } */


/* 입력 그룹 */
.input-group {
    width: 100%;
    margin-bottom: 1rem; /* 각 입력 필드 그룹 아래 간격 */
    text-align: left; /* 라벨 왼쪽 정렬 */
}

.input-label {
    display: block; /* 라벨을 블록 요소로 */
    font-size: 0.95rem;
    color: #555;
    margin-bottom: 0.5rem; /* 라벨과 입력 필드 사이 간격 */
    font-weight: 500;
}

.required-label::after {
    content: ' •'; /* 필수 입력 표시 (점) */
    color: #8a2be2; /* 보라색 점 */
    font-weight: bold;
    margin-left: 4px;
}


input {
  width: 100%;
  padding: 12px 15px; /* 패딩 조정 */
  border-radius: 8px; /* 둥근 모서리 */
  border: 1.5px solid #e0d0ff; /* 연한 보라색 테두리 */
  box-sizing: border-box;
  font-size: 1rem;
  color: #333;
}

input:hover, input:focus {
  border-color: #a471ff; /* 호버/포커스 시 보라색 */
  border-width: 1.5px;
  outline: none;
  box-shadow: 0 0 0 3px rgba(164, 113, 255, 0.2); /* 포커스 시 그림자 */
}

.disabled-input {
  background-color: #f8f9fa; /* 비활성화된 입력 필드 배경 */
  color: #888; /* 비활성화된 텍스트 색상 */
  cursor: not-allowed;
}

.error-space {
  height: 20px; /* 오류 메시지 공간 확보 */
  margin-top: 5px;
  margin-bottom: 15px;
  width: 100%;
  text-align: center;
}
.error-msg {
  color: #e74c3c;
  font-size: 0.9rem;
  line-height: 1;
}

.confirm-button {
  width: 100%;
  background-image: linear-gradient(to right, #8a2be2 0%, #a471ff 100%); /* 보라색 그라데이션 */
  color: white;
  border: none;
  border-radius: 8px;
  padding: 12px; /* 패딩 조정 */
  font-size: 1.1rem; /* 폰트 크기 조정 */
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(138, 43, 226, 0.3); /* 보라색 그림자 */
  margin-top: 1rem; /* 상단 여백 추가 */
}

.confirm-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(138, 43, 226, 0.4);
}

/* --- 모바일 반응형 --- */
@media (max-width: 576px) {
  .modal {
    padding: 1.5rem 1rem;
    max-width: 300px;
  }
  .modal-header h2 {
    font-size: 1.5rem;
  }
  .close-btn {
    font-size: 1.8rem;
  }
  /* 프로필 이미지 관련 스타일 제거 */
  /* .profile-image-container { ... } */
  /* .camera-icon { ... } */
  .input-group {
    margin-bottom: 0.8rem;
  }
  .input-label {
    font-size: 0.85rem;
    margin-bottom: 0.4rem;
  }
  input {
    padding: 10px 12px;
    font-size: 0.9rem;
  }
  .error-space {
    height: 18px;
    margin-top: 3px;
    margin-bottom: 10px;
  }
  .error-msg {
    font-size: 0.8rem;
  }
  .confirm-button {
    padding: 10px;
    font-size: 1rem;
  }
}
</style>
