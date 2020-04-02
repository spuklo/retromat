import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const state = {
  stats: {
    'sessions':0,
    'safety_levels': 0,
    'min_safety': 0,
    'max_safety': 0,
    'avg_safety': 0
  },
  cards: [],
  connected : false,
  votes_remaining: 5
}

const getters = {
  getStats: state => state.stats,
  votesRemaining: state => state.votes_remaining,
  getCards: state => state.cards,
  isDisconnected: state => !state.connected
}

const mutations = {
  SOCKET_ONOPEN(state) {
    state.connected = true
  },
  SOCKET_ONCLOSE(state) {
    state.connected = false
  },
  SOCKET_ONMESSAGE(state, message) {
    console.log(message)
    if (message.data) {
      const json = JSON.parse(message.data)
      if (json.type === 'STATS') {
        state.stats = json.body
      } else if (json.type === 'RETRO') {
        state.cards = json.body.cards
      } else if (json.type === 'CARD') {
        state.cards = state.cards.filter(it => it.id !== json.body.id)
        state.cards.push(json.body)
      }
    }
  },
  onUpVote(state) {
    if (state.votes_remaining > 0) {
      state.votes_remaining -= 1
    }
  },
  onDownVote(state) {
    if (state.votes_remaining < 5) {
      state.votes_remaining += 1
    }
  }
}

const actions = {
  handleVote({ commit }, voted) {
    if (voted) {
      commit('onUpVote')
    } else {
      commit('onDownVote')
    }
  }
}

export default new Vuex.Store({
  state,
  getters,
  mutations,
  actions
})
