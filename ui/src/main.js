// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import App from './App'
import router from './router'

import DocSearchBar from '@/components/DocSearchBar'

import { library } from '@fortawesome/fontawesome-svg-core'
import { faSearch, faSave, faPlusSquare, faBan } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import VueAutosuggest from 'vue-autosuggest'

Vue.use(VueAutosuggest)

library.add(faSearch)
library.add(faSave)
library.add(faPlusSquare)
library.add(faBan)

Vue.component('ficon', FontAwesomeIcon)
Vue.component('doc-search-bar', DocSearchBar)

Vue.config.productionTip = false

/* eslint-disable no-new */
new Vue({
  el: '#app',
  router,
  components: { App },
  template: '<App/>'
})
