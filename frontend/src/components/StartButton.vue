<template>
  <button class="start-button" @click="handleClick">시작</button>
</template>

<script>
export default {
  methods: {
    handleClick() {
      this.$emit('click');
    }
  }
}
</script>

<style scoped>
.start-button {
  background-color: #a471ff;
  color: white;
  border: none;
  border-radius: 0.5rem;
  padding: 0.375rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
  width: fit-content;
  margin: 1rem auto 0;
}

.start-button:hover {
  background-color: #8b5cf6;
}
</style>
