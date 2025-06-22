<template>
  <div class="overlay" @click.self="closeModal">
    <div class="modal">
      <div class="modal-header">
        <h2>정보 수정</h2>
        <button class="close-btn" @click="closeModal">×</button>
      </div>
      <form @submit.prevent="saveProfile">
        <div class="input-group">
          <label for="email" class="input-label">이메일</label>
          <input type="email" id="email" v-model="form.email" placeholder="이메일 주소" disabled class="disabled-input"/>
        </div>

        <div class="input-group">
          <label for="current-password" class="input-label">현재 비밀번호</label>
          <input type="password" id="current-password" v-model="form.currentPassword" placeholder="현재 비밀번호 입력" />
        </div>

        <div class="input-group">
          <label for="new-password" class="input-label">새 비밀번호 (변경 시)</label>
          <input type="password" id="new-password" v-model="form.newPassword" placeholder="새 비밀번호 (변경 시 입력)" />
        </div>

        <div class="input-group">
          <label for="confirm-new-password" class="input-label">새 비밀번호 확인</label>
          <input type="password" id="confirm-new-password" v-model="form.confirmNewPassword" placeholder="새 비밀번호 확인" />
        </div>

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
import { reactive, ref, watch, onMounted } from 'vue';
import axios from 'axios';

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
    userId: { // userId prop은 여전히 필요합니다.
      type: String,
      required: true,
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

    watch(() => props.initialEmail, (newEmail) => {
      form.email = newEmail;
    }, { immediate: true });
    watch(() => props.initialNickname, (newNickname) => {
      form.nickname = newNickname;
    }, { immediate: true });

    // userId를 사용하여 사용자 정보를 불러오는 함수 (GET /api/user/{userId})
    const fetchUserProfile = async () => {
      if (!props.userId) { // userId가 없으면 조회하지 않음
          errorMessage.value = '사용자 ID 정보가 없어 사용자 정보를 불러올 수 없습니다.';
          return;
      }
      try {
        // GET /api/user/{userId} 엔드포인트 호출
        const response = await axios.get(`/api/user/${props.userId}`);
        if (response.data.status === 'OK' && response.data.user) {
          form.email = response.data.user.usermail; // 'usermail' 키로 이메일 받음
          form.nickname = response.data.user.username; // 'username' 키로 닉네임 받음
          // 비밀번호 필드는 초기화
          form.currentPassword = '';
          form.newPassword = '';
          form.confirmNewPassword = '';
        } else {
          errorMessage.value = response.data.message || '사용자 정보를 불러오는데 실패했습니다.';
        }
      } catch (error) {
        console.error('사용자 정보 불러오기 오류:', error);
        if (error.response && error.response.data && error.response.data.message) {
            errorMessage.value = error.response.data.message;
        } else {
            errorMessage.value = '사용자 정보를 불러오는 중 서버 오류가 발생했습니다.';
        }
      }
    };

    onMounted(() => {
      fetchUserProfile(); // 모달이 마운트될 때 사용자 정보 조회
    });

    const closeModal = () => {
      emit('close');
    };

    const saveProfile = async () => {
      errorMessage.value = '';

      // 닉네임 유효성 검사 (백엔드와 동일하게 필수 입력)
      if (!form.nickname.trim()) {
        errorMessage.value = '닉네임은 필수 입력입니다.';
        return;
      }

      const isPasswordChangeAttempt =
        form.newPassword.trim() !== '' ||
        form.currentPassword.trim() !== '' ||
        form.confirmNewPassword.trim() !== '';

      if (isPasswordChangeAttempt) {
        if (!form.newPassword.trim()) {
            errorMessage.value = '새 비밀번호를 변경하려면 새 비밀번호 필드를 입력해야 합니다.';
            return;
        }
        if (form.newPassword.length < 6) {
            errorMessage.value = '새 비밀번호는 최소 6자 이상이어야 합니다.';
            return;
        }
        if (form.newPassword !== form.confirmNewPassword) {
          errorMessage.value = '새 비밀번호와 비밀번호 확인이 일치하지 않습니다.';
          return;
        }
        if (!form.currentPassword.trim()) {
          errorMessage.value = '비밀번호를 변경하려면 현재 비밀번호를 입력해야 합니다.';
          return;
        }
      } else {
        // 새 비밀번호를 입력하지 않을 때 현재 비밀번호나 확인 필드가 채워져 있다면 오류
        if (form.currentPassword.trim() !== '' || form.confirmNewPassword.trim() !== '') {
            errorMessage.value = '새 비밀번호를 입력하지 않으려면, 현재 비밀번호 및 확인 필드를 비워두세요.';
            return;
        }
      }

      try {
        const payload = {
          username: form.nickname, // 닉네임은 항상 전송
        };

        if (form.newPassword.trim() !== '') { // 새 비밀번호가 있을 때만 현재 비밀번호와 새 비밀번호를 payload에 추가
            payload.currentPassword = form.currentPassword;
            payload.newPassword = form.newPassword;
        }

        // PUT /api/user/{userId}/profile 엔드포인트 호출
        const response = await axios.put(`/api/user/${props.userId}/profile`, payload);

        if (response.data.status === 'OK' || response.data.status === 'NO_CHANGE') {
          // 백엔드에서 받은 메시지를 직접 alert (닉네임 변경, 비밀번호 변경, 둘 다 변경 등)
          alert(response.data.message);
          // 프로필 업데이트 성공 시 부모 컴포넌트에 알림 및 모달 닫기
          emit('update-profile', { nickname: form.nickname, email: form.email });
          closeModal();
        } else {
            // 'INVALID', 'NOT_FOUND', 'ERROR' 등 다른 상태는 errorMessage로 표시
            errorMessage.value = response.data.message || '프로필 업데이트 실패.';
        }

      } catch (error) {
        console.error('프로필 업데이트 오류:', error);
        if (error.response) {
          // HTTP 상태 코드가 400, 404, 500이거나 data.message가 있는 경우
          if (error.response.data && error.response.data.message) {
            errorMessage.value = error.response.data.message;
          } else if (error.response.status === 400) {
            errorMessage.value = '잘못된 요청입니다. 입력값을 확인해주세요.';
          } else if (error.response.status === 404) {
             errorMessage.value = '사용자 정보를 찾을 수 없습니다.';
          } else if (error.response.status === 500) {
            errorMessage.value = '서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
          } else {
            errorMessage.value = `오류 발생: ${error.response.status} ${error.response.statusText}`;
          }
        } else if (error.request) {
          // 요청이 전송되었지만 응답을 받지 못한 경우 (네트워크 문제)
          errorMessage.value = '서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.';
        } else {
          // 요청 설정 중 오류가 발생한 경우
          errorMessage.value = '요청을 보내는 중에 오류가 발생했습니다.';
        }
      }
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