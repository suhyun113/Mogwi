<template>
  <header class="navbar">
    <div class="logo" @click="goHome">
      <img src="@/assets/mogwi-character.png" alt="Mogwi Character" />
      <span>MOGWI</span>
    </div>
    <nav class="menu">
      <router-link to="/mystudy">나의 학습</router-link>
      <router-link to="/create">문제 만들기</router-link>
      <router-link to="/report">학습 리포트</router-link>
      <router-link to="/mypage">마이페이지</router-link>
    </nav>
    <div class="auth">
      <template v-if="!isLoggedIn">
        <button class="login-btn" @click="$emit('open-login')">로그인</button>
        <button class="register-btn" @click="$emit('open-register')">회원가입</button>
      </template>
      <template v-else>
        <button class="login-btn" @click="onLogout">로그아웃</button>
      </template>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

const store = useStore()
const router = useRouter()

const goHome = () => {
  router.push('/')
}

// 로그인 여부 확인
const isLoggedIn = computed(() => !!store.state.store_userid)

// 로그아웃 처리
const onLogout = () => {
  store.commit('setUserInfo', {
    userid: '',
    userpass: '',
    usermail: '',
    username: '',
    created_at: ''
  })
  localStorage.clear()
  router.push('/')
}
</script>


<style scoped>
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #1f1235;
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

.logo span {
  font-weight: bold;
  font-size: 20px;
  color: #ffe066;
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
  color: #a471ff;
}

.auth {
  display: flex;
  gap: 1.5rem;
}

.login-btn {
  background: none;
  border: 1px solid #a471ff;
  color: #a471ff;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.login-btn:hover {
  background-color: #a471ff;
  color: white;
}

.register-btn {
  background: none;
  border: 1px solid #a471ff;
  color: #a471ff;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.register-btn:hover {
  background-color: #a471ff;
  color: white;
}
</style>
