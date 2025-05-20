<template>
  <div class="overlay" @click.self="close">
    <div class="modal">
      <div class="modal-header">
        <h2>회원가입</h2>
        <button class="close-btn" @click="close">×</button>
      </div>

      <!-- 이메일 인증 영역 -->
      <div v-if="!verified" class="step">
        <input v-model="usermail" type="email" placeholder="이메일" />
        <button @click="sendCode" class="sendCode-btn">인증코드 전송</button>

        <div v-if="mailSent">
          <input v-model="verifyInput" placeholder="인증코드 입력" />
          <button @click="checkCode">인증 확인</button>
          <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>
        </div>
      </div>

      <!-- 인증 후 회원가입 영역 -->
      <form v-if="verified" @submit.prevent="onRegister">
        <input v-model="userid" placeholder="아이디" />
        <input v-model="userpass" type="password" placeholder="비밀번호" />
        <input v-model="username" placeholder="닉네임" />
        <button class="register-btn">가입하기</button>
        <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

// eslint-disable-next-line
const emit = defineEmits(['close', 'switch-to-login'])

const usermail = ref('')
const userid = ref('')
const userpass = ref('')
const username = ref('')
const verifyInput = ref('')
const verifyCode = ref('')
const verified = ref(false)
const mailSent = ref(false)
const errorMessage = ref('')

const close = () => emit('close')

const sendCode = async () => {
  try {
    const res = await axios.post('/api/send-email-code', { usermail: usermail.value })
    if (res.data.status === 'OK') {
      verifyCode.value = res.data.code // 실제 서비스에서는 이 값은 서버에 저장하고, 프론트엔드에는 안 보냄
      mailSent.value = true
      errorMessage.value = ''
      alert('이메일로 인증코드를 보냈습니다.')
    } else {
      errorMessage.value = '메일 전송 실패'
    }
  } catch (e) {
    console.error(e)
    errorMessage.value = '서버 오류가 발생했습니다.'
  }
}

const checkCode = () => {
  if (verifyInput.value === verifyCode.value) {
    verified.value = true
    errorMessage.value = ''
    alert('이메일 인증 완료!')
  } else {
    errorMessage.value = '인증코드가 일치하지 않습니다.'
  }
}

const onRegister = async () => {
  if (!userid.value || !userpass.value || !username.value) {
    errorMessage.value = '모든 정보를 입력해주세요.'
    return
  }

  try {
    const res = await axios.post('/api/register', {
      userid: userid.value,
      userpass: userpass.value,
      username: username.value,
      usermail: usermail.value
    })

    if (res.data.status === 'OK') {
      alert('회원가입 완료')
      emit('close')
    } else {
      errorMessage.value = '회원가입 실패'
    }
  } catch (e) {
    console.error(e)
    errorMessage.value = '서버 오류가 발생했습니다.'
  }
}
</script>

<style scoped>
.overlay {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background-color: rgba(0,0,0,0.5);
  display: flex; justify-content: center; align-items: center;
  z-index: 1000;
}
.modal {
  background: white;
  padding: 2rem;
  width: 320px;
  border-radius: 0;
  position: relative;
  box-shadow: 0 10px 30px rgba(0,0,0,0.25);
  animation: popup 0.2s ease-out;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

@keyframes popup {
  0% { transform: scale(0.9); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.close-btn {
  font-size: 28px;
  background: none; border: none;
  cursor: pointer;
}

form {
  display: flex;
  flex-direction: column;
  align-items: center;
}

input {
  width: 100%;
  padding: 10px;
  margin-bottom: 12px;
  border-radius: 8px;
  border: 1px solid #ccc;
  box-sizing: border-box;
}

.agree-container {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  margin-bottom: 1rem;
}

.sendCode-btn {
  background-color: #a471ff;
  color: white;
  border: none;
  padding: 8px;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.3s;
}

.register-btn {
  background-color: #a471ff;
  color: white;
  border: none;
  padding: 10px;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.3s;
}

.register-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.register-btn:hover:enabled {
  background-color: #854fe6;
}

.error-space {
  height: 18px;
  margin-top: 4px;
}

.error-msg {
  color: #e74c3c;
  font-size: 13px;
  line-height: 1;
}

.bottom-link {
  text-align: center;
  font-size: 14px;
  margin-top: 1rem;
  color: #777;
}

.login-link {
  color: #a471ff;
  cursor: pointer;
  font-weight: 500;
}
</style>
