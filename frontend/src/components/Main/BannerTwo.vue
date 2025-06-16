<template>
  <div class="banner-card banner-two" :style="{ backgroundColor: bgColor }">
    <div class="banner-content">
      <img :src="characterImage" alt="Mogwi with chart" class="banner-character" />
      <div class="banner-text">
        <h2>나의 학습 현황,</h2>
        <p>리포트에서 자세히 확인하세요!</p>
      </div>
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
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 100%;
  max-width: 900px;
  position: relative;
}

.banner-text {
  flex-basis: 60%;
  text-align: right; /* 텍스트 오른쪽 정렬 */
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
  margin-left: -50px; /* 왼쪽으로 살짝 튀어나오게 */
  z-index: 0;
}

@media (max-width: 768px) {
  .banner-card {
    padding: 15px;
    height: 300px;
  }
  .banner-content {
    flex-direction: column-reverse; /* 이미지 먼저 나오도록 순서 변경 */
    text-align: center;
  }
  .banner-text {
    flex-basis: auto;
    margin-top: 10px;
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
    margin-left: 0;
  }
}
</style>