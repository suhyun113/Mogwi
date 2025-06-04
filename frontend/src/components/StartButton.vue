<template>
  <button class="start-btn" @click="$emit('click')">시작</button>
</template>

<script>
export default {}
</script>

<style scoped>
.start-btn {
  margin-top: 1rem;
  background-color: #a855f7;
  color: white;
  padding: 0.5rem 2rem;
  border-radius: 9999px;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(168, 85, 247, 0.4);
  transition: background-color 0.2s ease;
}
.start-btn:hover {
  background-color: #9333ea;
}
</style>
