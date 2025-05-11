import { createRouter, createWebHistory } from 'vue-router'
import MainView from '@/views/MainView.vue'
import LoginView from '@/views/LoginView.vue'
import MyStudyView from '@/views/MyStudyView.vue'

const routes = [
  { path: '/', name: 'home', component: MainView },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/mystudy', name: 'mystudy', component: MyStudyView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
