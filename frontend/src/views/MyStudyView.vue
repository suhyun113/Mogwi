<template>
  <div class="mystudy">
    <h1>{{ username }}님의 학습 페이지</h1>
    <p>로그인 후 이동한 화면입니다.</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useStore } from 'vuex'

const store = useStore()
const username = computed(() => store.state.store_username)
</script>
