import { createRouter, createWebHistory } from 'vue-router'
import MainView from '@/views/MainView.vue'
import MyStudyView from '@/views/MyStudyView.vue'
import StudyView from '@/views/StudyView.vue'
import SolveView from '@/views/SolveView.vue'
import CreateView from '@/views/CreateView.vue'

const routes = [
  { 
    path: '/', 
    name: 'main', 
    component: MainView 
  },
  {
    path: '/study/:id',
    name: 'StudyView',
    component: StudyView
  },
  {
    path: '/study/:id/solve',
    name: 'SolveView',
    component: SolveView
  },
  { 
    path: '/mystudy', 
    name: 'mystudy', 
    component: MyStudyView 
  },
  {
    path: '/create',
    name: 'create',
    component: CreateView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
