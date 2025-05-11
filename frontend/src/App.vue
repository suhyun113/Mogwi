<template>
  <div id="app">
    <NavBar />
    <router-view />
  </div>
</template>

<script setup>
import NavBar from '@/components/NavBar.vue'
</script>

<style>
body {
  margin: 0;
  background-color: #fdf8f4;
  font-family: 'Pretendard', sans-serif;
}
</style>
