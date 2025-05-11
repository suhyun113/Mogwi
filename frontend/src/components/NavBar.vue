<template>
  <header class="navbar">
    <div class="logo" @click="goHome">
      <img src="@/assets/logo.png" alt="Mogwi Logo" />
      <span>MOGWI</span>
    </div>
    <nav class="menu">
      <router-link to="/mystudy">나의 학습</router-link>
      <router-link to="/create">문제 만들기</router-link>
      <router-link to="/report">학습 리포트</router-link>
      <router-link to="/mypage">마이페이지</router-link>
    </nav>
    <div class="auth">
      <router-link to="/login">로그인</router-link>
      <router-link to="/register">회원가입</router-link>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #0f0f1c;
  padding: 1rem 2rem;
  color: white;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.logo img {
  height: 36px;
  margin-right: 10px;
}

.menu {
  display: flex;
  gap: 2rem;
}

.menu a, .auth a {
  color: #eaeaff;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.menu a:hover, .auth a:hover {
  color: #9b7edc;
}

.auth {
  display: flex;
  gap: 1.5rem;
}
</style>
