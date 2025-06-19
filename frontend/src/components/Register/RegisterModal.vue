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
          <input v-model="usermail" type="email" placeholder="이메일" class="email-input"/>
          <button @click="sendCode" class="sendCode-btn" :disabled="sendDisabled">인증코드 전송</button>
        </div>
        <!-- 이메일 형식 오류 또는 중복 이메일-->
        <p v-if="emailError" class="error-msg">{{ emailError }}</p>
        
        <!-- 인증코드 입력 영역은 mailSent가 true일 때만 표시-->
        <div v-if="mailSent" class="verify-section">
          <div class="code-input-wrapper">
            <input v-model="verifyInput" placeholder="인증코드 입력" class="code-input" />
            <span class="timer-inside" v-if="!timeExpired">{{ minutes }}:{{ seconds.toString().padStart(2, '0') }}</span>
          </div>
          <button @click="checkCode" class="checkCode-btn">인증 확인</button>
          <p v-if="errorMessage && mailSent" class="error-msg">{{ errorMessage }}</p>
        </div>
      </div>

      <!-- 인증 후 회원가입 영역 -->
      <form v-if="verified" @submit.prevent="onRegister">
        <input v-model="userid" placeholder="아이디" class="register-input"/>
        <input v-model="userpass" type="password" placeholder="비밀번호" class="register-input"/>
        <input v-model="username" placeholder="닉네임" class="register-input"/>
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
const created_at = ref(new Date().toISOString().slice(0, 19).replace('T', ' '))
const verifyInput = ref('')
const verifyCode = ref('')
const verified = ref(false)
const mailSent = ref(false)
const errorMessage = ref('')
const emailError = ref('')
const sendDisabled = ref(false)

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

// 타이머머 시작
const startTimer = () => {
  minutes.value = 3
  seconds.value = 0
  timeExpired.value = false
  timer = setInterval(() => {
    if (seconds.value === 0) {
      if (minutes.value === 0) {
        clearInterval(timer)
        timeExpired.value = true
        mailSent.value = false
        errorMessage.value = '인증 시간이 초과되었습니다. 다시 시도해주세요.'
        sendDisabled.value = false
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

// 인증 코드 전송
const sendCode = async () => {
  validateEmail()
  if (emailError.value) {
    mailSent.value = false
    return
  }

  try {
    sendDisabled.value = true
    const res = await axios.post('/api/send-email-code', { usermail: usermail.value })
    if (res.data.status === 'DUPLICATE') {
      mailSent.value = false
      emailError.value = '이미 가입된 이메일입니다.'
      sendDisabled.value = false
      return
    }
    
    if (res.data.status === 'OK') {
      mailSent.value = true
      emailError.value = ''
      errorMessage.value = ''
      verifyCode.value = res.data.code // 실제 서비스에서는 제거
      startTimer()
      alert('이메일로 인증코드를 보냈습니다.')
    } else {
      errorMessage.value = res.data.message || '메일 전송 실패'
      mailSent.value = false
      sendDisabled.value = false
    }
  } catch (e) {
    console.error(e)
    errorMessage.value = '서버 오류가 발생했습니다.'
    mailSent.value = false
    sendDisabled.value = false
  }
}

// 인증 코드 확인
const checkCode = async () => {
  if (timeExpired.value) {
    errorMessage.value = '인증 시간이 만료되었습니다. 다시 시도해주세요.'
    mailSent.value = false
    return
  }

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
    } else if (res.data.status === 'EXPIRED') {
      errorMessage.value = '인증 시간이 만료되었습니다. 다시 인증해주세요.'
      mailSent.value = false
      timeExpired.value = true
    } 
    else {
      errorMessage.value = '인증코드가 일치하지 않습니다.'
    }
  } catch (e) {
    console.error(e)
    errorMessage.value = '서버 오류가 발생했습니다.'
  }
}

// 회원가입
const onRegister = async () => {
  if (!userid.value || !userpass.value || !username.value) {
    errorMessage.value = '모든 정보를 입력해주세요.'
    return
  }

  try {
    const res = await axios.post('/api/auth/register', {
      userid: userid.value,
      userpass: userpass.value,
      username: username.value,
      usermail: usermail.value,
      created_at: created_at.value
    })

    if (res.data.status === 'OK') {
      alert('회원가입 완료')
      emit('close')
    } else if (res.data.status === 'DUPLICATE') {
      errorMessage.value = '이미 존재하는 아이디입니다.'
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
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal {
  background: white;
  padding: 2rem;
  width: 320px;
  border-radius: 0;
  position: relative;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
  animation: popup 0.2s ease-out;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
@keyframes popup {
  0% {
    transform: scale(0.9);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
.close-btn {
  font-size: 28px;
  background: none;
  border: none;
  cursor: pointer;
}
.step,
form {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.email-input, 
.code-input, 
.register-input {
  width: 100%;
  padding: 10px;
  margin-bottom: 12px;
  border-radius: 8px;
  border: 1.5px solid #ccc;
  box-sizing: border-box;
}

.email-input:hover, .email-input:focus,
.code-input:hover, .code-input:focus,
.register-input:hover, .register-input:focus {
  border-color: #a471ff;
  border-width:1.5px;
  outline: none;
}
.sendCode-btn, 
.checkCode-btn, 
.register-btn {
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
.sendCode-btn:disabled {
  background-color: #d1c4e9 !important;
  cursor: not-allowed !important;
  opacity: 0.6;
}
/* 비활성화 상태에서 hover에도 변화 없게 */
.sendCode-btn:disabled:hover {
  background-color: #d1c4e9 !important;
  cursor: not-allowed !important;
  opacity: 0.6;
}
/* 활성화된 상태일 때만 hover 스타일 적용 */
.sendCode-btn:not(:disabled):hover,
.checkCode-btn:hover,
.register-btn:hover {
  background-color: #854fe6;
}

.error-msg {
  color: #e74c3c;
  font-size: 13px;
  line-height: 1;
  text-align: center;
}
.verify-section {
  width: 100%;
  margin-top: 16px;
}
.code-input-wrapper {
  width: 100%;
  position: relative;
}
.timer-inside {
  position: absolute;
  right: 12px;
  top: 35%;
  transform: translateY(-50%);
  font-size: 13px;
  color: #555;
  pointer-events: none;
}
</style>