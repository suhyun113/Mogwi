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
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 100%; /* 부모 높이를 채우도록 추가 */
  max-width: 900px;
  position: relative;
}

.banner-text {
  flex-basis: 60%; /* 텍스트가 차지할 공간 */
  text-align: left;
  z-index: 1; /* 캐릭터 이미지 위로 오도록 */
}

.banner-text h2 {
  font-size: 2.8rem;
  font-weight: 800;
  margin-bottom: 10px;
  line-height: 1.2;
}

.banner-text p {
  font-size: 1.5rem;
  font-weight: 500;
  margin: 0;
}

.banner-character {
  flex-basis: 40%;
  height: auto;
  max-height: 400px; /* 이미지 최대 높이를 조정 */
  object-fit: contain;
  margin-right: -50px;
  z-index: 0;
}

@media (max-width: 768px) {
  .banner-card {
    padding: 15px;
    height: 300px; /* 모바일 높이 설정 */
  }
  .banner-content {
    flex-direction: column;
    text-align: center;
  }
  .banner-text {
    flex-basis: auto;
    margin-bottom: 10px;
    text-align: center;
  }
  .banner-text h2 {
    font-size: 1.8rem;
    margin-bottom: 5px;
  }
  .banner-text p {
    font-size: 1.1rem;
  }
  .banner-character {
    max-height: 200px; /* 모바일 이미지 최대 높이 조정 */
    margin-right: 0;
  }
}
</style>