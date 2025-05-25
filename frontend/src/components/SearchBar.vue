<template>
  <div class="search-bar">
    <div class="search-input-wrapper">
      <input
        v-model="searchText"
        placeholder="문제 제목을 입력하세요."
        class="search-input"
      />
      <button class="search-btn" @click="emitSearch">검색</button>
    </div>
    <div class="category-buttons">
      <button
        v-for="cat in categories"
        :key="cat"
        @click="selectCategory(cat)"
        :class="['category-button', { active: selectedCategory === cat }]"
      >
        {{ cat }}
      </button>
    </div>
  </div>
</template>

<script>
export default {
  props: ['categories'],
  emits: ['search'],
  data() {
    return {
      searchText: '',
      selectedCategory: '#전체'
    }
  },
  mounted() {
    // 초기 검색 emit
    this.emitSearch()
  },
  methods: {
    emitSearch() {
      this.$emit('search', {
        text: this.searchText,
        category: this.selectedCategory
      })
    },
    selectCategory(cat) {
      this.selectedCategory = cat
      this.emitSearch()
    }
  }
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 16px;
}
.search-input-wrapper {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.search-input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
}
.search-btn {
  padding: 8px 16px;
  border: none;
  background-color: #a471ff;
  color: white;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}
.search-btn:hover {
  background-color: #854fe6;
}
.category-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.category-button {
  border: none;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  font-size: 13px;
  background-color: #eee;
  color: #333;
  transition: all 0.2s ease;
}
.category-button.active {
  background-color: #a471ff;
  color: white;
}
</style>
