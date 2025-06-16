<template>
  <div class="banner-card banner-three" :style="{ backgroundColor: bgColor }">
    <div class="banner-content">
      <div class="banner-text">
        <h2>반복 학습으로</h2>
        <p>완벽하게 정복하세요!</p>
      </div>
      <img :src="characterImage" alt="Happy Mogwi" class="banner-character" />
    </div>
  </div>
</template>

<script>
import lookMogwi from '@/assets/mogwi-look.png'; // 적절한 모귀 캐릭터 이미지 경로로 변경

export default {
  name: 'BannerThree',
  props: {
    bgColor: {
      type: String,
      default: '#4CAF50', // 초록색 계열
    },
  },
  data() {
    return {
      characterImage: lookMogwi,
    };
  },
};
</script>

<style scoped>
.banner-card {
  width: 100%;
  height: 700px;
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

.banner-three {
  background-image: linear-gradient(to right, #8bc34a 0%, #4caf50 100%);
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 100%;
  max-width: 900px;
  position: relative;
}

.banner-text {
  flex-basis: 60%;
  text-align: left;
  z-index: 1;
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
  max-height: 400px;
  object-fit: contain;
  margin-right: -50px;
  z-index: 0;
}

@media (max-width: 768px) {
  .banner-card {
    padding: 15px;
    height: 300px;
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
    max-height: 200px;
    margin-right: 0;
  }
}
</style>