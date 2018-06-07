<template>
  <div class="hello">
    <h1>{{ metadata.name }}</h1>
    <h3>{{ metadata.path }}</h3>
    <div id="codeEditor" ref="codeEditor">
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import CodeMirror from 'codemirror-minified'

import 'codemirror-minified/lib/codemirror.css'
import 'codemirror-minified/theme/neo.css'
import 'codemirror-minified/mode/gfm/gfm'

export default {
  name: 'EditDocument',
  data () {
    return {
      document: '',
      metadata: {}
    }
  },
  created () {
    axios.get(`/api/docs/${this.$route.params.docId}`)
      .then(response => {
        this.metadata = response.data
      })
      .catch(console.error)
    axios.get(`/api/docs/${this.$route.params.docId}/raw`)
      .then(response => {
        this.document = response.data
        CodeMirror(this.$refs.codeEditor, {
          lineNumbers: true,
          value: this.document,
          mode: 'gfm',
          theme: 'neo'
        })
      })
      .catch(console.error)
  }
}
</script>

<style scoped>
h3 {
  color: #677d92;
}

#codeEditor {
  text-align: left;
}

a {
  color: #42b983;
}
</style>

<style>
.CodeMirror {
  height: auto;
}
</style>
