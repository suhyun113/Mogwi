<template>
  <Carousel :items-to-show="1" :wrap-around="true" :autoplay="3000" :pause-autoplay-on-hover="true">
    <Slide v-for="(slide, index) in slides" :key="index">
      <slot :name="`slide-${index}`" :slideData="slide"></slot>
    </Slide>

    <template #addons>
      <Navigation />
      <Pagination />
    </template>
  </Carousel>
</template>

<script>
import { defineComponent } from 'vue';
import { Carousel, Slide, Pagination, Navigation } from 'vue3-carousel';
import 'vue3-carousel/dist/carousel.css';

export default defineComponent({
  name: 'CustomCarousel',
  components: {
    Carousel,
    Slide,
    Pagination,
    Navigation,
  },
  props: {
    slides: {
      type: Array,
      required: true,
      // 예: [{ id: 1, type: 'banner1', data: { /* ... */ } }]
    },
  },
});
</script>

<style scoped>
/* 기존 ImageCarousel.vue의 스타일과 유사하게 유지하거나 필요에 따라 수정 */
.carousel__item {
  width: 100%;
  height: 700px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  /* 테두리 둥근 값 제거 */
  border-radius: 0; /* 이 부분을 추가하거나 0으로 설정 */
}

/* Customize carousel navigation and pagination */
.carousel__prev,
.carousel__next {
  box-sizing: content-box;
  color: #8c5dff;
}

.carousel__pagination-button::after {
  background-color: #d1c4e9;
}

.carousel__pagination-button--active::after {
  background-color: #8c5dff;
}

@media (max-width: 768px) {
  .carousel__item {
    height: 300px;
  }
}
</style>