<template>
  <div class="banner-card banner-three" :style="{ backgroundColor: bgColor }">
    <div class="banner-content">
      <img :src="characterImage" alt="Happy Mogwi" class="banner-character" />
      <div class="banner-text">
        <h2>반복 학습으로</h2>
        <p>완벽하게 정복하세요!</p>
      </div>
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
  margin: 0;
  padding: 0;
  flex-basis: auto;
  margin-bottom: 0;
  position: relative;
  top: 18px;
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
  margin: 50px;
  padding: 0;
  height: auto;
  max-height: 270px;
  object-fit: contain;
  z-index: 0;
  flex-basis: auto;
  width: auto;
  position: relative;
  top: 18px;
}

@media (max-width: 768px) {
  .banner-card {
    padding: 15px;
    height: 100vw;
    min-height: 220px;
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
    top: 10px;
  }
  .banner-text h2,
  .banner-text p {
    margin-bottom: 0;
  }
  .banner-text h2 {
    font-size: 1.3rem;
  }
  .banner-text p {
    font-size: 0.95rem;
  }
  .banner-character {
    max-height: 100px;
    top: 10px;
    margin: 0;
    padding: 0;
    display: block;
    flex-basis: auto;
    width: auto;
  }
}
</style>