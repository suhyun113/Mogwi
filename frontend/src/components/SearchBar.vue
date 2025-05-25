<template>
  <div class="search-bar">
    <input
      v-model="keyword"
      @keyup.enter="emitSearch"
      type="text"
      placeholder="문제를 검색하세요"
    />
    <button @click="emitSearch">검색</button>
  </div>
</template>

<script>
export default {
  data() {
    return {
      keyword: ''
    }
  },
  methods: {
    emitSearch() {
      this.$emit('search', this.keyword)
    }
  }
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 6px;
}
button {
  padding: 8px 16px;
  background-color: #a471ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
}
button:hover {
  background-color: #854fe6;
}
</style>
