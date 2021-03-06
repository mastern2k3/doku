import Vue from 'vue'
import Router from 'vue-router'
import HelloWorld from '@/components/HelloWorld'
import EditDocument from '@/components/EditDocument'

Vue.use(Router)

export default new Router({
  mode: 'history',
  routes: [
    { path: '/', component: HelloWorld },
    { name: 'edit_doc', path: '/doc/:docId', component: EditDocument }
  ]
})
