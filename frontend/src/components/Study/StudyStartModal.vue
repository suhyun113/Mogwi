<template>
  <div class="study-modal-backdrop">
    <div class="study-modal">
      <p>나의 학습에 추가되었습니다.<br />이제 이 문제를 학습할 수 있습니다.</p>
      <div class="study-buttons">
        <button @click="$emit('go-preview')">문제 구경하기</button>
        <button @click="$emit('go-study')">문제 바로 풀기</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StudyStartModal',
}
</script>

<style scoped>
.study-modal-backdrop {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}
.study-modal {
  background: white;
  padding: 32px 40px;
  width: 360px;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  text-align: center;
}
.study-modal p {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 24px;
  color: #333;
  line-height: 1.6;
}
.study-buttons {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
button {
  flex: 1;
  padding: 10px 0;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  font-size: 14px;
  transition: background-color 0.2s ease;
}
button:first-child {
  background-color: #f0f0f0;
  color: #444;
}
button:first-child:hover {
  background-color: #e0e0e0;
}
button:last-child {
  background-color: #a471ff;
  color: white;
}
button:last-child:hover {
  background-color: #854fe6;
}
</style>
