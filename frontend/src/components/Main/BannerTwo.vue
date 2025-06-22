<template>
  <div class="banner-card banner-two" :style="{ backgroundColor: bgColor }">
    <div class="banner-content">
      <div class="banner-text">
        <h2>나의 학습 현황,</h2>
        <p>리포트에서 자세히 확인하세요!</p>
      </div>
      <img :src="characterImage" alt="Mogwi with chart" class="banner-character" />
    </div>
  </div>
</template>

<script>
import sleepMogwi from '@/assets/mogwi-sleep.png'; // 적절한 모귀 캐릭터 이미지 경로로 변경

export default {
  name: 'BannerTwo',
  props: {
    bgColor: {
      type: String,
      default: '#FFC107', // 황금색 계열
    },
  },
  data() {
    return {
      characterImage: sleepMogwi,
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
  color: #333; /* 텍스트 색상 변경 */
  border-radius: 0px;
  overflow: hidden;
  position: relative;
}

.banner-two {
  background-image: linear-gradient(to right, #ffe066 0%, #ffc107 100%);
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
  max-height: 280px;
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
  }
  .banner-text h2,
  .banner-text p {
    margin-bottom: 0;
  }
  .banner-character {
    max-height: 130px;
    top: 10px;
  }
}
</style>