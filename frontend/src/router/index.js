import { createRouter, createWebHistory } from 'vue-router'
import MainView from '@/views/MainView.vue'
import MyStudyView from '@/views/MyStudyView.vue'

const routes = [
  { path: '/', name: 'home', component: MainView },
  { path: '/mystudy', name: 'mystudy', component: MyStudyView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
