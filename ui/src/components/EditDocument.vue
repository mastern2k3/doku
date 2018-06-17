<template>
  <div class="hello">
    <h1>{{ metadata.name }}</h1>
    <h3 v-if="metadata.path" class="text-muted">/ {{ metadata.path.join(" / ") }}</h3>
    <h5 v-if="docTags">
      <a href="http://google.com" v-bind:key="tag" v-for="tag of docTags.unchanged" class="doctag badge badge-primary">#{{tag}}</a><a href="http://google.com" v-bind:key="tag" v-for="tag of docTags.added" class="doctag badge badge-success">#{{tag}}</a><a href="http://google.com" v-bind:key="tag" v-for="tag of docTags.removed" class="doctag badge badge-danger">#{{tag}}</a>
    </h5>
    <button v-on:click="save">Save</button>
    <div id="codeEditor" ref="codeEditor">
    </div>
    <div id="message">
      <div style="padding: 5px;">
        <div id="inner-message" class="alert alert-success alert-dismissible fade" role="alert">
          <button type="button" class="close" data-dismiss="alert" aria-label="Close">
  <span aria-hidden="true">&times;</span>
</button>
          Saved!
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import _ from 'lodash'
import axios from 'axios'
import CodeMirror from 'codemirror-minified'

import 'codemirror-minified/lib/codemirror.css'
import 'codemirror-minified/theme/neo.css'
import 'codemirror-minified/mode/gfm/gfm'

$('.alert').alert()

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
      plugins: {}
    }
  },
  computed: {
    docTags: function () {
      if (!this.plugins.tags) return {}

      const committed = _.uniq(_.flatten(_.values(this.plugins.tags.committed)))
      const staged = _.uniq(_.flatten(_.values(this.plugins.tags.staged)))

      const removed = _.difference(committed, staged)
      const added = _.difference(staged, committed)
      const unchanged = _.intersection(staged, committed)

      return { added: added, removed: removed, unchanged: unchanged }
    }
  },
  methods: {
    save: function () {
      const saveTimeGeneration = this.cm.changeGeneration()

      axios.post(`/api/docs/${this.$route.params.docId}/save`, this.cm.getValue())
        .then(response => {
          this.cleanGeneration = saveTimeGeneration
          
          $('#inner-message').addClass('show')
          setTimeout(() => {
            $('#inner-message').removeClass('show')
          }, 2000)
        })
        .catch(console.error)
    }
  },
  created () {
    _.defer(axios.post, `/api/docs/${this.$route.params.docId}/visit`)

    axios.get(`/api/docs/${this.$route.params.docId}`)
      .then(response => {
        this.metadata = response.data
        document.title = `${this.metadata.name} @ ${this.metadata.path.join('/')}`
      })
      .catch(console.error)

    axios.get(`/api/docs/${this.$route.params.docId}/raw`)
      .then(response => {
        this.document = response.data

        const self = this
        CodeMirror.commands.save = function (insance) {
          self.save()
        }

        this.cm = CodeMirror(this.$refs.codeEditor, {
          lineNumbers: true,
          lineWrapping: true,
          value: this.document,
          mode: 'gfm',
          theme: 'neo'
        })

        this.cleanGeneration = this.cm.changeGeneration()

        window.onbeforeunload = function (e) {
          if (!self.cm.isClean(self.cleanGeneration)) {
            const dialogText = 'You have some unsaved changes. Click "Save" or press "Ctrl-S" in order to save your changes.'
            e.returnValue = dialogText
            return dialogText
          }

          return null
        }

        this.cm.on('changes', (cm, changes) => {
          changes.forEach(change => {
            cm.eachLine(change.from.line, change.to.line + 1, handle => {
              if (!(handle.lineNo() in this.plugins.tags.staged)) {
                this.$set(this.plugins.tags.staged, handle.lineNo(), getTags(handle.text))
              } else {
                if (!_.isEqual(this.plugins.tags.staged[handle.lineNo()], getTags(handle.text))) {
                  this.plugins.tags.staged[handle.lineNo()] = getTags(handle.text)
                }
              }
            })
          })
        })

        _.defer(() => {
          const committed = {}
          this.cm.eachLine(handle => {
            const tags = getTags(handle.text)
            if (_.isEmpty(tags)) return
            committed[handle.lineNo()] = tags
          })
          this.$set(this.plugins, 'tags', { committed: committed, staged: _.cloneDeep(committed) })
        })
      })
      .catch(console.error)
  }
}
</script>

<style scoped>
.doctag + .doctag {
  margin-left: 0.3em;
}

.badge.badge-danger {
  text-decoration: line-through;
}

#codeEditor {
  text-align: left;
}

#message {
  position: fixed;
  bottom: 0;
  right: 0;
  z-index: 999;
}

#inner-message {
  margin: 0 auto;
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
