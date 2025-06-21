import { createStore } from 'vuex'

export default createStore({
  state: {
    store_userid: localStorage.getItem('local_userid'),
    store_userpass: localStorage.getItem('local_userpass'),
    store_usermail: localStorage.getItem('local_usermail'),
    store_username: localStorage.getItem('local_username'),
    store_created_at: localStorage.getItem('local_created_at')
  },
  getters: {},
  mutations: {
    setUserInfo(state, payload) {
      state.store_userid = payload.userid;
      state.store_userpass = payload.userpass;
      state.store_usermail = payload.usermail;
      state.store_username = payload.username;
      state.store_created_at = payload.created_at;

      localStorage.setItem('local_userid', payload.userid)
      localStorage.setItem('local_userpass', payload.userpass)
      localStorage.setItem('local_usermail', payload.usermail)
      localStorage.setItem('local_username', payload.username)
      localStorage.setItem('local_created_at', payload.created_at)
    },
    clearUserInfo(state) {
      state.store_userid = null;
      state.store_userpass = null;
      state.store_usermail = null;
      state.store_username = null;
      state.store_created_at = null;

      localStorage.removeItem('local_userid')
      localStorage.removeItem('local_userpass')
      localStorage.removeItem('local_usermail')
      localStorage.removeItem('local_username')
      localStorage.removeItem('local_created_at')
    }
  },
  actions: {
    logout({ commit }) {
      commit('clearUserInfo');
    }
  },
  modules: {}
})
