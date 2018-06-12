<template>
  <div class="hello">
    <h1>{{ metadata.name }}</h1>
    <h3>/ {{ metadata.path.join(" / ") }}</h3>
    <button v-on:click="save">Save</button>
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
  methods: {
    save: function () {
      axios.post(`/api/docs/${this.$route.params.docId}/save`, this.cmInstance.getValue())
        .then(response => {
          alert('Saved!')
        })
        .catch(console.error)
    }
  },
  created () {
    axios.get(`/api/docs/${this.$route.params.docId}`)
      .then(response => {
        this.metadata = response.data
        document.title = this.metadata.name
      })
      .catch(console.error)

    axios.get(`/api/docs/${this.$route.params.docId}/raw`)
      .then(response => {
        this.document = response.data

        this.cmInstance = CodeMirror(this.$refs.codeEditor, {
          lineNumbers: true,
          lineWrapping: true,
          value: this.document,
          mode: 'gfm',
          theme: 'neo'
        })

        this.cmInstance.on('changes', (cm, changes) => {
          changes.forEach(change => {
            cm.eachLine(change.from.line, change.to.line + 1, handle => {
              console.log(handle.text)
            })
          })
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

.CodeMirror .cm-header-1 {
  font-size: 2.5em;
}

.CodeMirror .cm-header-2 {
  font-size: 2em;
}

.CodeMirror .cm-header-3 {
  font-size: 1.5em;
}

.CodeMirror .cm-header-4 {
  font-size: 1.4em;
}

.CodeMirror .cm-header-5 {
  font-size: 1.2em;
}

.CodeMirror .cm-property + .cm-variable-2  {
  text-decoration: line-through;
}

.CodeMirror-cursor {
  border-left: 1px solid black !important;
  background: rgba(0,0,0,0) !important;
}
</style>
