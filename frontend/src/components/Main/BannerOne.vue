<template>
  <div class="banner-card banner-one" :style="{ backgroundColor: bgColor }">
    <div class="banner-content">
      <div class="banner-text">
        <h2>새로운 문제들,</h2>
        <p>지금 바로 풀어보세요!</p>
      </div>
      <img :src="characterImage" alt="Thinking Mogwi" class="banner-character" />
    </div>
  </div>
</template>

<script>
import confusedMogwi from '@/assets/mogwi-confused.png'; // 적절한 모귀 캐릭터 이미지 경로로 변경

export default {
  name: 'BannerOne',
  props: {
    bgColor: {
      type: String,
      default: '#A471FF', // 기본 보라색 계열
    },
  },
  data() {
    return {
      characterImage: confusedMogwi,
    };
  },
};
</script>

<style scoped>
.banner-card {
  width: 100%;
  height: 700px; /* CustomCarousel의 carousel__item 높이와 동일하게 설정 */
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  box-sizing: border-box;
  color: white;
  border-radius: 0px;
  overflow: hidden;
  position: relative;
}

.banner-one {
  background-image: linear-gradient(to right, #a471ff 0%, #8c5dff 100%);
}

.banner-content {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0px;
  width: 100%;
  height: 100%;
  max-width: 900px;
  position: relative;
}

.banner-text {
  text-align: center;
  z-index: 1;
  margin: 50px;
  padding: 0;
  flex-basis: auto;
  margin-bottom: 0;
}

.banner-text h2,
.banner-text p {
  margin-bottom: 0;
}

.banner-text h2 {
  font-size: 2.3rem;
  font-weight: 800;
  line-height: 1.2;
}

.banner-text p {
  font-size: 1.2rem;
  font-weight: 500;
}

.banner-character {
  display: block;
  margin: 0;
  padding: 0;
  height: auto;
  max-height: 320px;
  object-fit: contain;
  z-index: 0;
  flex-basis: auto;
  width: auto;
}

@media (max-width: 768px) {
  .banner-card {
    padding: 15px;
    height: 300px; /* 모바일 높이 설정 */
  }
  .banner-content {
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0px;
    text-align: center;
  }
  .banner-text {
    margin: 0;
    padding: 0;
    margin-bottom: 0;
    text-align: center;
    flex-basis: auto;
  }
  .banner-text h2,
  .banner-text p {
    margin-bottom: 0;
  }
  .banner-character {
    max-height: 150px;
    margin: 0;
    padding: 0;
    display: block;
    flex-basis: auto;
    width: auto;
  }
}
</style>