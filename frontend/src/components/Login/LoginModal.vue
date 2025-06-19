<template>
  <div class="overlay" @click.self="close">
    <div class="modal">
      <div class="modal-header">
        <h2>로그인</h2>
        <button class="close-btn" @click="close">×</button>
      </div>
      <form @submit.prevent="onLogin">
        <input v-model="userid" placeholder="아이디" />
        <input v-model="userpass" type="password" placeholder="비밀번호" />
        <div class="remember-container">
          <input type="checkbox" v-model="remember" id="remember-checkbox" class="remember-checkbox" />
          <label for="remember" class="remember-label">로그인 상태 유지</label>
        </div>
        <button class="login-btn">로그인</button>
        <div class="error-space">
          <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>
        </div>
      </form>
      <div class="links">
        <a href="#" class="dimmed-link" @click="$emit('open-register')">회원가입</a>
        <span class="dimmed-sep"> | </span>
        <a href="#" class="dimmed-link">아이디·비밀번호 찾기</a>
      </div>
      <div class="separator">
        <span>또는</span>
      </div>
      <button class="study-tip-btn">비회원 학습 체험하기</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import axios from 'axios'

// eslint-disable-next-line
const emit = defineEmits(['close'])
const store = useStore()
const router = useRouter()

const userid = ref('')
const userpass = ref('')
const remember = ref(false)
const errorMessage = ref('')

const close = () => emit('close')

function onLogin() {
  if (!userid.value && !userpass.value) {
    errorMessage.value = '아이디와 비밀번호를 입력해주세요.'
    return
  } else if (!userid.value) {
    errorMessage.value = '아이디를 입력해주세요.'
    return
  } else if (!userpass.value) {
    errorMessage.value = '비밀번호를 입력해주세요.'
    return
  }

  axios.post('/api/auth/login', {
    userid: userid.value,
    userpass: userpass.value
  })
  .then(response => {
    if (response.data.status === 'OK') {
      const userInfo = response.data.user
      store.commit('setUserInfo', userInfo)
      alert('로그인 성공')
      errorMessage.value = ''
      close()
      router.push('/mystudy')
    } else if (response.data.status === 'NOT') {
      errorMessage.value = '아이디 또는 비밀번호가 올바르지 않습니다.'
    } else {
      errorMessage.value = '서버 오류가 발생했습니다.'
    }
  })
  .catch(err => {
    console.error(err)
    errorMessage.value = '서버 오류가 발생했습니다.'
  })
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
  border: 1.5px solid #ccc;
  box-sizing: border-box;
}
input:hover, input:focus {
  border-color: #a471ff;
  border-width: 1.5px;
  outline: none;
}
.login-btn {
  width: 100%;
  background: #a471ff;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px;
  font-weight: bold;
  cursor: pointer;
}
.login-btn:hover {
  background: #854fe6;
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
.remember-container {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  width: 100%;
  margin-bottom: 1rem;
  font-size: 14px;
  color: #333;
}
.remember-checkbox {
  appearance: none;
  width: 14px;
  height: 14px;
  border: 1px solid #aaa;
  border-radius: 3px;
  background-color: white;
  position: relative;
  cursor: pointer;
  display: inline-block;
  vertical-align: middle;
}
.remember-checkbox:checked {
  background-color: #a471ff;
  border-color: #a471ff;
}
.remember-checkbox:checked::after {
  content: '✔';
  color: white;
  font-size: 10px;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  line-height: 1;
}
.remember-label {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
}
.links {
  margin-top: 1rem;
  font-size: 14px;
  text-align: center;
}
.dimmed-link {
  color: #888;
  text-decoration: none;
}
.dimmed-link:hover {
  color: #555;
}
.dimmed-sep {
  color: #aaa;
}
.separator {
  display: flex;
  align-items: center;
  text-align: center;
  margin: 1.5rem 0;
}
.separator::before, .separator::after {
  content: '';
  flex: 1;
  border-top: 1px solid #eee;
}
.separator::before {
  margin-right: .75em;
}
.separator::after {
  margin-left: .75em;
}
.separator span {
  color: #999;
  font-size: 14px;
  white-space: nowrap;
}
.study-tip-btn {
  width: 100%;
  background: #fff4c1;
  padding: 10px;
  border: none;
  border-radius: 8px;
  color: #4a3f69;
  font-weight: bold;
  cursor: pointer;
}
.study-tip-btn:hover {
  background: #ffe066;
}
</style>
