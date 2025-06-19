<template>
  <div class="report-banner">
    <div class="banner-content">
      <div class="text-content">
        <h1 class="banner-title">나의 학습 리포트</h1>
        <p class="banner-description">
          모귀와 함께한 학습 여정을 한눈에 확인하고,<br>
          더 효율적인 학습 계획을 세워보세요!
        </p>
      </div>
      <img src="@/assets/mogwi-character.png" alt="모귀 리포트 캐릭터" class="banner-mogwi-character">
    </div>
  </div>
</template>

<script>
export default {
  name: 'ReportBanner',
};
</script>

<style scoped>
.report-banner {
  width: 100%;
  padding: 80px 20px; /* 패딩을 60px에서 80px로 증가 */
  background: linear-gradient(to right, #8c5dff, #a471ff); /* 그라데이션 배경 */
  color: white;
  position: relative;
  overflow: hidden; /* 캐릭터가 튀어나오지 않도록 */
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  min-height: 350px; /* 최소 높이를 300px에서 350px로 증가 */
  display: flex;
  align-items: center; /* 세로 중앙 정렬 */
  justify-content: center; /* 가로 중앙 정렬 (내부 컨텐츠) */
}

.banner-content {
  position: relative; /* z-index를 위해 유지 */
  z-index: 10;
  max-width: 1000px; /* 내부 컨텐츠의 최대 너비 */
  width: 100%; /* 너비를 꽉 채우도록 */
  display: flex; /* flexbox 사용 */
  align-items: center; /* 수직 중앙 정렬 */
  justify-content: space-between; /* 텍스트와 이미지 양쪽 끝으로 정렬 */
  padding: 0 40px; /* 좌우 여백 */
  box-sizing: border-box; /* 패딩 포함 너비 계산 */
}

.text-content {
  text-align: left; /* 텍스트 왼쪽 정렬 */
  max-width: 50%; /* 텍스트가 차지할 최대 너비 */
  /* 필요에 따라 margin-right 또는 flex-grow 추가 가능 */
}

.banner-title {
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 15px;
  text-shadow: 2px 2px 5px rgba(0, 0, 0, 0.2);
}

.banner-description {
  font-size: 1.3rem;
  line-height: 1.6;
  margin-bottom: 25px;
  font-weight: 400;
  opacity: 0.95;
}

.banner-mogwi-character {
  /* position: absolute;  제거 - flex item으로 작동하도록 */
  /* bottom: -20px; 제거 */
  /* right: 5%; 제거 */
  width: 220px; /* 크기 조절 */
  height: auto;
  opacity: 0.8;
  filter: drop-shadow(5px 5px 10px rgba(0,0,0,0.2));
  flex-shrink: 0; /* 이미지가 줄어들지 않도록 */
}

@media (max-width: 992px) {
  .banner-content {
    padding: 0 20px;
  }
  .text-content {
    max-width: 60%; /* 텍스트 영역을 조금 더 넓게 */
  }
  .banner-title {
    font-size: 2.5rem;
  }
  .banner-description {
    font-size: 1.1rem;
  }
  .banner-mogwi-character {
    width: 180px;
  }
}

@media (max-width: 768px) {
  .report-banner {
    padding: 50px 15px; /* 모바일에서도 패딩 증가 */
    min-height: 300px; /* 모바일에서의 최소 높이도 증가 */
  }
  .banner-content {
    flex-direction: column; /* 화면이 작아지면 세로로 전환 */
    align-items: center; /* 세로 전환 시 중앙 정렬 */
    text-align: center; /* 텍스트도 중앙 정렬 */
    padding: 0; /* 패딩 제거 또는 조정 */
  }
  .text-content {
    max-width: 100%; /* 텍스트가 전체 너비 사용 */
    margin-bottom: 20px; /* 텍스트와 이미지 사이 간격 */
    text-align: center; /* 텍스트 중앙 정렬 */
  }
  .banner-title {
    font-size: 2rem;
  }
  .banner-description {
    font-size: 1rem;
    margin-bottom: 15px;
  }
  .banner-mogwi-character {
    width: 150px;
    /* 세로 배치 시 bottom/right 조정 필요 없을 수 있음 */
  }
}

@media (max-width: 480px) {
  .report-banner {
    padding: 40px 10px; /* 작은 화면에서도 패딩 증가 */
    min-height: 250px; /* 작은 화면에서의 최소 높이도 증가 */
  }
  .banner-title {
    font-size: 1.7rem;
  }
  .banner-description {
    font-size: 0.9rem;
  }
  .banner-mogwi-character {
    width: 120px;
  }
}
</style>