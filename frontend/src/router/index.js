import { createRouter, createWebHistory } from 'vue-router'
import MainView from '@/views/MainView.vue'
import MyStudyView from '@/views/MyStudyView.vue'
import StudyView from '@/views/StudyView.vue'
import SolveView from '@/views/SolveView.vue'
import CreateView from '@/views/CreateView.vue'
import EditView from '@/views/EditView.vue'
import ReportView from '@/views/ReportView.vue'
import MypageView from '@/views/MypageView.vue'

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
  },
  {
    path: '/edit/:id',
    name: 'edit',
    component: EditView
  },
  {
    path: '/report',
    name: 'report',
    component: ReportView
  },
  {
    path: '/mypage',
    name: 'mypage',
    component: MypageView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
