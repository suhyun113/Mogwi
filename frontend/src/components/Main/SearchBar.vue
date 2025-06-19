<template>
  <div class="search-bar">
    <div class="search-input-wrapper">
      <input
        v-model="searchText"
        placeholder="문제 제목 검색"
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
  width: 100%;
  margin-bottom: 20px;
  box-sizing: border-box;
}

.search-input-wrapper {
  display: flex;
  /* gap: 12px; /* Keep or adjust as needed */
  gap: 15px; /* Increased gap slightly */
  margin-bottom: 18px; /* Increased margin-bottom slightly */
  box-sizing: border-box;
}
.search-input {
  flex: 1;
  /* Increase padding to make it taller and wider */
  padding: 15px 20px; /* Original: 12px 16px */
  border: 1px solid #ccc;
  border-radius: 8px;
  /* Increase font size */
  font-size: 18px; /* Original: 16px */
  outline: none;
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.05);
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  min-width: 0;
}
.search-input:focus {
  border-color: #a471ff;
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.05), 0 0 0 3px rgba(164, 113, 255, 0.2);
}
.search-btn {
  /* Increase padding to make it taller and wider */
  padding: 15px 28px; /* Original: 12px 24px */
  border: none;
  background-color: #a471ff;
  color: white;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.2s;
  /* Increase font size */
  font-size: 18px; /* Original: 16px */
  box-sizing: border-box;
}
.search-btn:hover {
  background-color: #854fe6;
  transform: translateY(-1px);
}
.category-buttons {
  display: flex;
  flex-wrap: wrap;
  /* gap: 10px; /* Keep or adjust as needed */
  gap: 12px; /* Increased gap slightly */
  box-sizing: border-box;
}
.category-button {
  border: none;
  /* Increase padding for larger buttons */
  padding: 10px 18px; /* Original: 8px 16px */
  border-radius: 20px;
  cursor: pointer;
  /* Increase font size */
  font-size: 16px; /* Original: 14px */
  background-color: #eee;
  color: #333;
  transition: all 0.2s ease;
  box-sizing: border-box;
}
.category-button.active {
  background-color: #a471ff;
  color: white;
  box-shadow: 0 2px 5px rgba(164, 113, 255, 0.3);
}
.category-button:hover:not(.active) {
  background-color: #e0e0e0;
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .search-input-wrapper {
    flex-direction: column;
    gap: 12px; /* Adjusted for mobile */
  }
  .search-btn {
    width: 100%;
    font-size: 16px; /* Adjusted for mobile */
    padding: 12px 20px; /* Adjusted for mobile */
  }
  .search-input {
    font-size: 16px; /* Adjusted for mobile */
    padding: 12px 16px; /* Adjusted for mobile */
  }
  .category-buttons {
    gap: 10px; /* Adjusted for mobile */
  }
  .category-button {
    font-size: 13px; /* Adjusted for mobile */
    padding: 7px 12px; /* Adjusted for mobile */
  }
}
</style>