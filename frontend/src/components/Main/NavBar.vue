<template>
  <header class="navbar">
    <div class="logo" @click="goHome">
      <img src="@/assets/mogwi-character.png" alt="Mogwi Character" />
      <span>모귀</span>
    </div>
    <nav class="menu">
      <!-- router-link 사용 시 active 클래스 자동 적용 -->
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

// 로그인 여부 확인: Vuex 스토어의 실제 키값인 'store_userid'를 사용합니다.
const isLoggedIn = computed(() => !!store.state.store_userid)

// 로그아웃 처리
const onLogout = () => {
  // setUserInfo mutation을 사용하여 모든 관련 상태를 초기화합니다.
  store.commit('setUserInfo', {
    userid: null, // null 또는 빈 문자열로 설정하여 로그아웃 상태를 나타냅니다.
    userpass: null,
    usermail: null,
    username: null,
    created_at: null
  });
  localStorage.clear(); // 모든 localStorage 데이터를 비웁니다.
  router.push('/');
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

.menu a:hover,
.menu a.router-link-exact-active {
  color: #a471ff; /* 호버 시 색상과 동일하게 설정 */
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

@media (max-width: 768px) {
  .navbar {
    padding: 0.8rem 1rem;
  }
  .logo img {
    height: 30px;
  }
  .logo span {
    font-size: 18px;
  }
  .menu {
    gap: 1rem;
    font-size: 0.9rem;
  }
  .auth {
    gap: 1rem;
  }
  .login-btn, .register-btn {
    padding: 5px 10px;
    font-size: 0.9rem;
  }
}
</style>