import { createStore } from 'vuex'

export default createStore({
  state: {
    store_userid : localStorage.getItem('local_userid'), // 로컬스토리지에 저장된 값을 저장
    store_userpass : localStorage.getItem('local_userpass'),
    store_usermail : localStorage.getItem('local_usermail'),
    store_username : localStorage.getItem('local_username'),
    store_created_at : localStorage.getItem('local_created_at')
  },
  getters: {
  },
  mutations: {
    setUserInfo(state, payload){
      state.store_userid = payload.userid;
      state.store_userpass = payload.userpass;
      state.store_usermail = payload.usermail;
      state.store_username = payload.username;
      state.store_create_at = payload.created_at;

      localStorage.setItem('local_userid', payload.userid)
      localStorage.setItem('local_userpass', payload.userpass)
      localStorage.setItem('local_usermail', payload.usermail)
      localStorage.setItem('local_username', payload.username)
      localStorage.setItem('local_created_at', payload.created_at)
    }
  },
  actions: {
  },
  modules: {
  }
})
