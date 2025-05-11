<template>
  <div id="app">
    <header>
      <NavBar @open-login="showLogin = true" />
    </header>
    <main>
      <router-view />
      <LoginModal v-if="showLogin" @close="showLogin = false" />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import NavBar from '@/components/NavBar.vue'
import LoginModal from '@/components/LoginModal.vue'

const showLogin = ref(false)
</script>

<style>
body {
  margin: 0;
  background-color: #fdf8f4;
  font-family: 'Pretendard', sans-serif;
}
</style>