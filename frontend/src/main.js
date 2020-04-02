import Vue from 'vue'
import App from './App.vue'
import store from './store'
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import VueNativeSock from 'vue-native-websocket'

Vue.config.productionTip = false
Vue.use(ElementUI);
Vue.use(VueNativeSock, 'ws://'+window.location.host+'/retro', { store: store })

new Vue({
  store,
  render: h => h(App)
}).$mount('#app')
