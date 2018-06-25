<template>
  <div>
  <nav class="navbar sticky-top navbar-dark bg-dark">
    <a class="navbar-brand" href="/">
      <img class="logo" src="../assets/logo.png">
      Doku
    </a>
    <div class="navbar-nav mr-auto">
        <a href="javascript:void(0)" v-on:click="save" class="nav-link">Save</a>
        <div class="input-group input-group-sm mb-5">
          <input v-model="newDocName" type="text" class="form-control" placeholder="Recipient's username" aria-label="Recipient's username" aria-describedby="basic-addon2">
          <div class="input-group-append">
            <button v-on:click="newDoc" class="btn btn-success" type="button" >new</button>
          </div>
        </div>
    </div>
    <div>
      <h4 class="navbar-text">{{ metadata.name }}</h4>
      <h6>
        <a href="javascript:void(0)" ref="pluginsButton" class="doctag badge badge-secondary" data-toggle="popover" title="Plugins" data-placement="left">@</a>
        <span v-if="docTags">
          <a href="http://google.com" v-bind:key="tag" v-for="tag of docTags.unchanged" class="doctag badge badge-primary">#{{tag}}</a><a href="http://google.com" v-bind:key="tag" v-for="tag of docTags.added" class="doctag badge badge-success">#{{tag}}*</a><a href="http://google.com" v-bind:key="tag" v-for="tag of docTags.removed" class="doctag badge badge-danger">#{{tag}}</a>
        </span>
      </h6>
    </div>
  </nav>
  <div style="text-align: center;">
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
    <div id="message-danger">
      <div style="padding: 5px;">
        <div id="inner-message-danger" class="alert alert-danger alert-dismissible fade" role="alert">
          <button type="button" class="close" data-dismiss="alert" aria-label="Close">
  <span aria-hidden="true">&times;</span>
</button>
          Error while saving :(
        </div>
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
  var re = /[^\n\S#]#([a-zA-Z_-]+)/g
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
      docId: null,
      document: '',
      metadata: {},
      plugins: {},
      newDocName: ''
    }
  },
  computed: {
    docTags: function () {
      if (!this.plugins.hashtags) return null

      const committed = _.uniq(_.flatten(_.values(this.plugins.hashtags.committed)))
      const staged = _.uniq(_.flatten(_.values(this.plugins.hashtags.staged)))

      const removed = _.difference(committed, staged)
      const added = _.difference(staged, committed)
      const unchanged = _.intersection(staged, committed)

      return { added: added, removed: removed, unchanged: unchanged }
    }
  },
  beforeRouteUpdate (to, from, next) {
    this.loadDoc(to.params.docId)
    next()
  },
  methods: {
    newDoc () {
      const self = this
      axios.put(`/api/docs`, { name: this.newDocName })
        .then(response => {
          self.$router.push({path: `/doc/${response.data.id}`})
        })
    },
    save () {
      const saveTimeGeneration = this.cm.changeGeneration()

      axios.post(`/api/docs/${this.docId}/save`, this.cm.getValue())
        .then(response => {
          this.cleanGeneration = saveTimeGeneration

          this.getDetails()

          $('#inner-message').addClass('show')
          setTimeout(() => {
            $('#inner-message').removeClass('show')
          }, 2000)
        })
        .catch(e => {
          $('#inner-message-danger').addClass('show')

          console.error(e)
        })
    },
    pluginsClick: function () {
      if (!this.metadata.metadata) {
        return ''
      }

      const c = document.createElement('h6')

      _.keys(this.metadata.metadata).forEach(p => {
        const n = document.createElement('a')
        n.innerHTML = '@' + p
        n.className = 'doctag badge badge-secondary'
        n.setAttribute('href', 'javascript:void(0)')

        c.appendChild(n)
      })

      return c
    },
    getDetails () {
      return axios.get(`/api/docs/${this.docId}`)
        .then(response => {
          this.metadata = response.data
          document.title = this.metadata.name
          if (this.metadata.metadata && this.metadata.metadata.hashtags) {
            this.$set(this.plugins.hashtags, 'committed', this.metadata.metadata.hashtags.tags)
          }
        })
        .catch(console.error)
    },
    loadDoc (docId) {
      this.docId = docId

      this.$set(this.plugins, 'hashtags', {})

      this.getDetails()

      axios.get(`/api/docs/${this.docId}/raw`)
        .then(response => {
          this.document = response.data

          if (this.cm) {
            this.cm.setValue(this.document)
            this.cm.refresh()
          }

          this.cleanGeneration = this.cm.changeGeneration()

          _.defer(() => {
            const staged = {}

            this.cm.eachLine(handle => {
              const tags = getTags(handle.text)
              if (_.isEmpty(tags)) return
              staged[handle.lineNo()] = tags
            })

            this.$set(this.plugins.hashtags, 'staged', staged)
          })
        })
        .catch(console.error)
    }
  },
  created () {
    // _.defer(axios.post, `/api/docs/${this.docId}/visit`)

    window.onbeforeunload = function (e) {
      if (this.cm.isClean(this.cleanGeneration)) {
        return null
      }

      const dialogText = 'You have some unsaved changes. Click "Save" or press "Ctrl-S" in order to save your changes.'

      e.returnValue = dialogText

      return dialogText
    }

    this.loadDoc(this.$route.params.docId)
  },
  mounted () {
    const self = this

    $(function () { $(self.$refs.pluginsButton).popover({ content: self.pluginsClick, html: true }) })

    CodeMirror.commands.save = function (insance) {
      self.save()
    }

    this.cm = CodeMirror(this.$refs.codeEditor, {
      lineNumbers: true,
      lineWrapping: true,
      value: '',
      mode: 'gfm',
      theme: 'neo'
    })

    this.cm.on('changes', (cm, changes) => {
      changes.forEach(change => {
        if (self.plugins.hashtags) {
          return
        }

        cm.eachLine(change.from.line, change.to.line + 1, handle => {
          const tags = getTags(handle.text)
          if (!(handle.lineNo() in self.plugins.hashtags.staged)) {
            self.$set(self.plugins.hashtags.staged, handle.lineNo(), tags)
          } else {
            if (!_.isEqual(self.plugins.hashtags.staged[handle.lineNo()], tags)) {
              self.plugins.hashtags.staged[handle.lineNo()] = tags
            }
          }
        })
      })
    })
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

a.nav-link {
  cursor: pointer;
}

#codeEditor {
  margin-top: 0.7em;
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

#message-danger {
  position: fixed;
  bottom: 0;
  right: 0;
  z-index: 999;
}

#inner-message-danger {
  margin: 0 auto;
}

.logo {
  height: 2rem;
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
