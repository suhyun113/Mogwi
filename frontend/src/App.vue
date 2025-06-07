<template>
  <div id="app">
    <header class="sticky-header">
      <NavBar @open-login="showLogin = true" @open-register="showRegister = true" />
    </header>
    <main>
      <router-view />
      <LoginModal v-if="showLogin" @close="showLogin = false" @open-register="openRegisterFromLogin"/>
      <RegisterModal v-if="showRegister" @close="showRegister = false" />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import NavBar from '@/components/NavBar.vue'
import LoginModal from '@/components/LoginModal.vue'
import RegisterModal from '@/components/RegisterModal.vue'

const showLogin = ref(false)
const showRegister = ref(false)

const openRegisterFromLogin = () => {
  showLogin.value = false
  showRegister.value = true
}
</script>

<style>
body {
  margin: 0;
  background-color: #fdf8f4;
  font-family: 'Pretendard', sans-serif;
}

#app {
  display: flex; /* Flexbox 레이아웃 적용 */
  flex-direction: column; /* 세로 방향으로 아이템 배치 */
  min-height: 100vh; /* 뷰포트 전체 높이를 최소 높이로 설정 */
}

main {
  flex-grow: 1; /* 남은 공간을 모두 차지하도록 설정 */
  overflow-y: auto; /* main 요소 내에서만 세로 스크롤 허용 */
  -webkit-overflow-scrolling: touch; /* iOS 부드러운 스크롤 */
  box-sizing: border-box; /* 패딩, 보더가 너비/높이에 포함되도록 */
  position: relative; /* StudyView 내부의 절대 위치 지정을 위한 기준점 */
}

.sticky-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background-color: #2c003e;
  /* height를 명시적으로 주지 않아도 내용에 따라 높이가 결정됨 */
  /* 필요하다면 여기에 padding이나 height 추가 */
}
</style>