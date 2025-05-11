<template>
  <div class="overlay" @click.self="close">
    <div class="modal">
      <button class="close-btn" @click="close">×</button>
      <h2>로그인</h2>
      <form @submit.prevent="onLogin">
        <input v-model="email" placeholder="이메일" />
        <input v-model="password" type="password" placeholder="비밀번호" />
        <div class="remember">
          <input type="checkbox" v-model="remember" id="remember" />
          <label for="remember">로그인 상태 유지</label>
        </div>
        <button class="login-btn">로그인</button>
      </form>
      <div class="links">
        <router-link to="/register">회원가입</router-link>
        <span> | </span>
        <a href="#">아이디·비밀번호 찾기</a>
      </div>
      <hr />
      <button class="guest-btn">비회원 주문배송 조회</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
// eslint-disable-next-line no-undef
const emit = defineEmits(['close'])

const email = ref('')
const password = ref('')
const remember = ref(false)

const close = () => emit('close')

const onLogin = () => {
  if (email.value && password.value) {
    alert('로그인 성공 (임시)')
    close()
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
  width: 350px;
  border-radius: 12px;
  position: relative;
  box-shadow: 0 10px 30px rgba(0,0,0,0.25);
  animation: popup 0.2s ease-out;
}
@keyframes popup {
  0% { transform: scale(0.9); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
.close-btn {
  position: absolute;
  top: 10px; right: 10px;
  font-size: 20px;
  background: none; border: none;
  cursor: pointer;
}
input {
  width: 100%;
  padding: 10px;
  margin-bottom: 12px;
  border-radius: 8px;
  border: 1px solid #ccc;
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
  box-shadow: 0 0 0 2px #ffe066;
}
.login-btn:hover {
  background: #854fe6;
}
.remember {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.links {
  margin-top: 1rem;
  font-size: 14px;
  text-align: center;
}
hr {
  margin: 1.5rem 0;
}
.guest-btn {
  width: 100%;
  background: #fff4c1;
  padding: 10px;
  border: none;
  border-radius: 8px;
  color: #4a3f69;
  font-weight: bold;
}
</style>