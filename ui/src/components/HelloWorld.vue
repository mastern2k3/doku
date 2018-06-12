<template>
  <div class="hello">
    <img src="../assets/logo.png">
    <h1>{{ msg }}</h1>
    <ul>
      <li v-bind:key="file.id" v-for="file of files">
        <router-link :to="'/doc/' + file.id">{{file.name}}</router-link> <small>{{file.path}}</small>
      </li>
    </ul>
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
