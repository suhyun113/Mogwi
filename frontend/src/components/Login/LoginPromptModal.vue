<template>
  <div class="modal-backdrop">
    <div class="modal">
      <p>로그인 후 이용 가능한 기능입니다.</p>
      <div class="buttons">
        <button @click="$emit('continue')">문제 구경하기</button>
        <button @click="$emit('open-login')">로그인하러 가기</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal {
  background: white;
  padding: 32px 40px;
  width: 360px;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  text-align: center;
}

.modal p {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 24px;
  color: #333;
}

.buttons {
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
