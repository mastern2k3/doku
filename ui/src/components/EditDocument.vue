<template>
  <div class="hello">
    <h1>{{ metadata.name }}</h1>
    <h3>/ {{ metadata.path.join(" / ") }}</h3>
    <h5>
      <span href="#" v-bind:key="tag" v-for="tag of docTags()" class="doctag badge badge-primary">#{{tag}}</span>
    </h5>
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

function getTags (text) {
  var re = /[^\n#]#([a-zA-Z_-]+)/g
  var m

  var tags = []

  do {
    m = re.exec(text)

    if (m) {
      tags.push(m[1])
    }
  } while (m)

  return tags
}

export default {
  name: 'EditDocument',
  data () {
    return {
      document: '',
      metadata: {},
      gdata: {
        tags: {}
      }
    }
  },
  methods: {
    docTags: function () {
      return Array.from(Object.values(this.gdata.tags).reduce((acc, arr) => { arr.forEach(acc.add.bind(acc)); return acc }, new Set()))
    },
    handleTagsInLine: function (tags, lineNumber) {
      this.gdata.tags[lineNumber] = tags
      this.$forceUpdate()
    },
    save: function () {
      axios.post(`/api/docs/${this.$route.params.docId}/save`, this.cm.getValue())
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
      })
      .catch(console.error)
    axios.get(`/api/docs/${this.$route.params.docId}/raw`)
      .then(response => {
        this.document = response.data
        this.cm = CodeMirror(this.$refs.codeEditor, {
          lineNumbers: true,
          lineWrapping: true,
          value: this.document,
          mode: 'gfm',
          theme: 'neo'
        })
        var self_ = this
        this.cm.on('changes', (cm, changes) => {
          changes.forEach(change => {
            cm.eachLine(change.from.line, change.to.line + 1, handle => {
              const tags = getTags(handle.text)
              if (tags) {
                self_.handleTagsInLine(tags, handle.lineNo())
              }
            })
          })
        })
        setTimeout(() => this.cm.eachLine(handle => {
          const tags = getTags(handle.text)
          if (tags) {
            self_.handleTagsInLine(tags, handle.lineNo())
          }
        }), 0)
      })
      .catch(console.error)
  }
}
</script>

<style scoped>
.doctag {
  margin-right: 0.3em;
}

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

</style>
