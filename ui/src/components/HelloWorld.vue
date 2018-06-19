<template>
  <div class="container">
    <div class="row" style="margin-bottom: 1rem;">
      <div class="col-2">
        <img class="logo" src="../assets/logo.png">
      </div>
      <div class="col">
        <h1>Doku</h1>
        <h3 class="text-muted">Start working..</h3>
      </div>
    </div>
    <div class="row">
      <div class="col form-group">
        <input v-model="searchText" type="text" class="form-control" placeholder="Search">
      </div>
    </div>
    <div class="card-columns">
      <div class="card card-document" v-bind:key="file.id" v-for="file of filterSeach(files)">
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
      searchText: '',
      files: []
    }
  },
  created () {
    axios.get('/api/docs')
      .then(response => {
        this.files = response.data
      })
      .catch(console.error)
  },
  methods: {
    filterSeach: function (files) {
      if (!this.searchText) return files
      return files.filter(f => f.name.includes(this.searchText))
    }
  }
}
</script>

<style scoped>
.card-document {
  text-align: center;
}

.logo {
  height: 6rem;
}
</style>
