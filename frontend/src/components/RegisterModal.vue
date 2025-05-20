<template>
  <div class="overlay" @click.self="close">
    <div class="modal">
      <div class="modal-header">
        <h2>회원가입</h2>
        <button class="close-btn" @click="close">×</button>
      </div>

      <!-- 이메일 인증 영역 -->
      <div v-if="!verified" class="step">
        <div class="email-row">
          <input v-model="usermail" type="email" placeholder="이메일" />
          <button @click="sendCode" class="sendCode-btn">인증코드 전송</button>
        </div>
        <p v-if="emailError" class="error-msg">{{ emailError }}</p>

        <div v-if="mailSent">
          <div class="verify-row">
            <input v-model="verifyInput" placeholder="인증코드 입력" class="code-input" />
            <span class="timer-text" v-if="!timeExpired">{{ minutes }}:{{ seconds.toString().padStart(2, '0')}}</span>
          </div>
          <button @click="checkCode" class="sendCode-btn">인증 확인</button>
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
import { ref, onUnmounted } from 'vue'
import axios from 'axios'

// eslint-disable-next-line
const emit = defineEmits(['close'])

const usermail = ref('')
const userid = ref('')
const userpass = ref('')
const username = ref('')
const verifyInput = ref('')
const verifyCode = ref('')
const verified = ref(false)
const mailSent = ref(false)
const errorMessage = ref('')

// Timer 관련
const minutes = ref(3)
const seconds = ref(0)
const timeExpired = ref(false)
let timer = null

// 이메일 유효성 검사
const validateEmail = () => {
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  emailError.value = emailPattern.test(usermail.value) ? '' : '이메일 형식이 올바르지 않습니다.'
}

const startTimer = () => {
  minutes.value = 3
  seconds.value = 0
  timer = setInterval(() => {
    if (seconds.value === 0) {
      if (minutes.value === 0) {
        clearInterval(timer)
        mailSent.value = false
        errorMessage.value = '인증 시간이 초과되었습니다. 다시 시도해주세요.'
      } else {
        minutes.value--
        seconds.value = 59
      }
    } else {
      seconds.value--
    }
  }, 1000)
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const close = () => {
  if (timer) clearInterval(timer)
  emit('close')
}

const sendCode = async () => {
  try {
    const res = await axios.post('/api/send-email-code', { usermail: usermail.value })
    if (res.data.status === 'OK') {
      verifyCode.value = res.data.code // 실제 서비스에서는 프론트엔드에서 제거
      mailSent.value = true
      errorMessage.value = ''
      startTimer()
      alert('이메일로 인증코드를 보냈습니다.')
    } else {
      errorMessage.value = '메일 전송 실패'
    }
  } catch (e) {
    console.error(e)
    errorMessage.value = '서버 오류가 발생했습니다.'
  }
}

const checkCode = async () => {
  try {
    const res = await axios.post('/api/verify-email-code', {
      usermail: usermail.value,
      code: verifyInput.value
    })
    if (res.data.status === 'OK') {
      verified.value = true
      errorMessage.value = ''
      clearInterval(timer)
      alert('이메일 인증 완료!')
    } else {
      errorMessage.value = '인증코드가 일치하지 않습니다.'
    }
  } catch (e) {
    console.error(e)
    errorMessage.value = '서버 오류가 발생했습니다.'
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

.step, form {
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
.sendCode-btn, .register-btn {
  background-color: #a471ff;
  color: white;
  border: none;
  padding: 10px;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.3s;
  width: 100%;
}
.sendCode-btn:hover, .register-btn:hover {
  background-color: #854fe6;
}
.error-msg {
  color: #e74c3c;
  font-size: 13px;
  line-height: 1;
  text-align: center;
}
.timer-text {
  font-size: 14px;
  color: #555;
  margin-bottom: 10px;
}
</style>
