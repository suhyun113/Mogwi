import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import store from './store/index' // userStore 경로 확인
import axios from 'axios' // axios import 추가

const app = createApp(App)

// Axios 기본 URL 설정
// TODO: 실제 백엔드 서버의 주소와 포트로 변경해주세요.
// 개발 환경에서는 'http://localhost:8000'을 사용하고,
// 배포 환경에서는 실제 서버 도메인으로 변경해야 합니다.
axios.defaults.baseURL = 'http://localhost:8000';

app.use(createPinia())
app.use(router)
app.use(store) // Vuex store 사용
app.mount('#app')