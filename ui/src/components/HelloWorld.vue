<template>
  <div class="container container-main">
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
        <input v-model="searchText" type="text" class="form-control" placeholder="Search" autofocus>
      </div>
    </div>
    <div class="card-columns">
      <div class="card card-document" v-for="file of filterSearch(files)" v-bind:key="file.id">
        <div class="card-header">
          <h5>{{file.name}}</h5>
          <div v-if="file.metadata && file.metadata.hashtags && file.metadata.hashtags.tags">
            <a v-for="tag in file.metadata.hashtags.tags" v-bind:key="tag" href="http://google.com" class="doctag badge badge-primary">#{{tag}}</a>
          </div>
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
import _ from 'lodash'

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

        function tagsOf (file) {
          if (file.metadata && file.metadata.hashtags && file.metadata.hashtags.tags) {
            return file.metadata.hashtags.tags
          } else {
            return []
          }
        }

        this.files.sort((a, b) => {
          const aPinned = tagsOf(a).includes('pin')
          const bPinned = tagsOf(b).includes('pin')

          if (aPinned && !bPinned) {
            return -1
          } else if (!aPinned && bPinned) {
            return 1
          }

          if (a.name > b.name) {
            return -1
          } else {
            return 1
          }
        })
      })
      .catch(console.error)
  },
  methods: {
    filterSearch: function (files) {
      if (!this.searchText) return files
      const lowerCaseSearchText = this.searchText.toLowerCase()

      return files.filter(f =>
        f.name.toLowerCase().includes(lowerCaseSearchText) ||
        _.find(_.get(f, 'metadata.hashtags.tags', []), t => t.toLowerCase().includes(lowerCaseSearchText)))
    }
  }
}
</script>

<style scoped>
.container-main {
  margin-top: 60px;
}

.card-document {
  text-align: center;
}

.logo {
  height: 6rem;
}

.doctag + .doctag {
  margin-left: 0.3em;
}
</style>
