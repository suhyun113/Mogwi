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

.sticky-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background-color: #2c003e;
}
</style>