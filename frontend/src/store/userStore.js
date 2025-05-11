import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    userInfo: null
  }),
  actions: {
    login(token, user) {
      this.token = token
      this.userInfo = user
      localStorage.setItem('token', token)
    },
    logout() {
      this.token = null
      this.userInfo = null
      localStorage.removeItem('token')
    }
  }
})
