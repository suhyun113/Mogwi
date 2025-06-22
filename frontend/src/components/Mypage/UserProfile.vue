<template>
  <section class="mypage-section user-profile-section">
    <div class="profile-header">
      <h2 class="section-title">{{ nickname }} 님 안녕하세요.</h2>
      <button @click="$emit('edit-info')" class="edit-info-button">
        <img src="@/assets/icons/edit.png" alt="수정" class="edit-icon" /> 수정
      </button>
    </div>
  </section>
</template>

<script>
/* eslint-disable no-unused-vars */ // 이 파일에서 'ref'와 'watch'가 직접적으로 사용되지 않아 발생하는 경고를 비활성화합니다.
import { ref, watch } from 'vue';

export default {
  name: 'UserProfile',
  props: {
    nickname: {
      type: String,
      required: true,
    },
  },
  emits: ['edit-info'],
  setup(props, { emit }) {
    return {};
  },
};
</script>

<style scoped>
/* 기존 스타일 유지 (MypageView.vue의 스타일과 연동) */
.user-profile-section {
  background-color: #fff;
  border: 1px solid #e5d8f7;
  border-radius: 10px;
  padding: 50px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 30px;
  max-width: 1400px;
  width: 100%;
  box-sizing: border-box;
  /* margin-left는 MypageView의 main-content 패딩으로 충분하므로 제거 */
  margin-left: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 100px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  color: #5a2e87;
  font-size: 1.8rem;
  font-weight: 600;
  margin: 0;
}

.edit-info-button {
  background-color: #f3eaff;
  color: #6a2dbd;
  border: 1.5px solid #e0d0ff;
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, box-shadow 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.2;
  box-shadow: 0 2px 8px rgba(106, 45, 189, 0.06);
  outline: none;
}

.edit-info-button:hover, .edit-info-button:focus {
  background-color: #e6dbff;
  color: #4a1e77;
  box-shadow: 0 4px 16px rgba(106, 45, 189, 0.10);
  border-color: #cbb6f7;
}

.edit-icon {
  width: 18px;
  height: 18px;
  display: inline-block;
  vertical-align: middle;
}

.profile-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  font-size: 1.1rem;
  color: #4a1e77;
  background-color: #fcfaff; /* 더 밝은 배경색으로 구분 */
  padding: 12px 18px; /* 패딩 조정 */
  border-radius: 8px;
  border: 1px solid #e9dffc;
}

.profile-item:last-child {
  margin-bottom: 0;
}

.label {
  font-weight: 600;
  margin-right: 15px; /* 라벨과 값 사이 간격 증가 */
  color: #6a3d9a;
  min-width: 80px;
}

.value {
  flex-grow: 1;
  font-weight: 500;
  color: #333;
}

/* 닉네임 직접 편집 관련 스타일은 필요 없으므로 제거 (주석 처리된 코드 기반) */
/* .nickname-display { ... } */
/* .edit-nickname-button { ... } */
/* .nickname-edit { ... } */
/* .nickname-input { ... } */
/* .save-nickname-button, .cancel-nickname-button { ... } */


@media (max-width: 768px) {
  .user-profile-section {
    padding: 20px;
  }
  .section-title {
    font-size: 1.5rem;
  }
  .edit-info-button {
    padding: 6px 12px;
    font-size: 0.85rem;
  }
  .profile-item {
    font-size: 1rem;
    padding: 8px 12px;
    flex-wrap: wrap;
  }
  .label {
    min-width: 60px;
    margin-bottom: 5px;
  }
  /* 닉네임 편집 관련 미디어 쿼리 스타일도 제거 (주석 처리된 코드 기반) */
  /* .nickname-edit { ... } */
  /* .nickname-input { ... } */
  /* .save-nickname-button, .cancel-nickname-button { ... } */
}

@media (min-width: 900px) {
  .user-profile-section {
    margin-left: 40px;
    width: calc(100% - 40px);
    align-items: flex-start;
  }
}
</style>