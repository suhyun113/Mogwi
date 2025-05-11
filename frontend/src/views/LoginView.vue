<template>
  <div class="login-page">
    <div class="login-box">
      <h2>로그인</h2>
      <form @submit.prevent="onLogin">
        <input v-model="email" placeholder="아이디" />
        <input v-model="password" type="password" placeholder="비밀번호" />
        <button type="submit">로그인</button>
      </form>
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'

const email = ref('')
const password = ref('')
const errorMsg = ref('')
const router = useRouter()
const userStore = useUserStore()

async function onLogin() {
  if (email.value === 'test' && password.value === '1234') {
    userStore.login('token', { email: email.value })
    router.push('/mystudy')
  } else {
    errorMsg.value = '아이디 또는 비밀번호가 잘못되었습니다.'
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  background: linear-gradient(135deg, #e4daff, #fdf8f4);
}

.login-box {
  background-color: white;
  padding: 3rem 2rem;
  border-radius: 1.5rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  text-align: center;
}

.login-box h2 {
  margin-bottom: 2rem;
  color: #9b7edc;
}

input {
  width: 100%;
  padding: 12px;
  margin-bottom: 1rem;
  border: 1px solid #ccc;
  border-radius: 12px;
  font-size: 15px;
}

button {
  width: 100%;
  background-color: #9b7edc;
  color: #fff;
  padding: 12px;
  border: none;
  border-radius: 12px;
  font-weight: bold;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

button:hover {
  background-color: #7a5fc2;
}

.error {
  color: #c00;
  font-size: 14px;
  margin-top: 0.5rem;
}
</style>
