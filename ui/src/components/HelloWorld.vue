<template>
  <div class="hello container">
    <img src="../assets/logo.png">
    <h1>{{ msg }}</h1>
    <div class="card-columns">
      <div class="card" v-bind:key="file.id" v-for="file of files">
        <div class="card-header">
          <h5>{{file.name}}</h5>
          <a href="http://google.com" class="doctag badge badge-primary">#some_tag</a> <a href="http://google.com" class="doctag badge badge-primary">#pin</a>
        </div>
        <div class="card-body">
          <router-link :to="'/doc/' + file.id" class="card-link">Edit</router-link>
          <a href="#" class="card-link">Another link</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'HelloWorld',
  data () {
    return {
      msg: 'Welcome to Your Vue.js App',
      files: []
    }
  },
  created () {
    axios.get('/api/docs')
      .then(response => {
        this.files = response.data
      })
      .catch(console.error)
  }
}
</script>

<style scoped>
h1, h2 {
  font-weight: normal;
}

small {
  font-size: 0.7em;
}

ul {
  text-align: left;
  list-style-type: none;
  padding: 0;
}

li {
  margin: 0 10px;
}
</style>
