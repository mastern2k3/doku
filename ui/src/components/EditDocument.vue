<template>
  <div>
  <nav class="navbar navbar-expand-sm sticky-top navbar-dark bg-dark">
    <a class="navbar-brand" href="/">
      <img class="logo" src="../assets/logo.png">
      Doku
    </a>
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarToggler" aria-controls="navbarToggler" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarToggler">
      <ul class="navbar-nav" style="width: 100%;">
        <li class="nav-item">
          <a href="javascript:void(0)" v-on:click="save" class="nav-link"><ficon icon="save" /> Save</a>
        </li>
        <li class="nav-item">
          <!--
            A form could work here perfectly, waiting for reasons not to use a form..
            <form class="form-inline">
            </form>
          -->
          <a v-show="!newDocState" href="javascript:void(0)" v-on:click="newDocInitiate" class="nav-link"><ficon icon="plus-square" /> New</a>
          <div v-show="newDocState" class="input-group input-group">
            <input v-model="newDocName" ref="newDocName" type="text" class="form-control" placeholder="New document name" aria-label="New document name">
            <div class="input-group-append">
              <button v-on:click="newDoc" class="btn btn-primary" type="button">New</button>
              <button v-on:click="newDocState = false" class="btn btn-secondary" type="button"><ficon icon="ban" /></button>
            </div>
          </div>
        </li>
        <li class="nav-item" style="flex-grow: 1; padding-right: 1em; padding-left: 0.5em;">
          <form class="form-inline" style="flex-grow: 1;">
            <div class="input-group input-group" style="flex-grow: 1;">
              <input type="text" class="form-control" placeholder="Search" aria-label="Search">
              <div class="input-group-append">
                <button v-on:click="newDocState = false" class="btn btn-primary" type="button"><ficon icon="search" /></button>
              </div>
            </div>
          </form>
        </li>
      </ul>
    </div>
    <div class="navbar-text" style="padding-top: 0; padding-bottom: 0;">
      <h4 class="doc-name">{{ metadata.name }}</h4>
      <div>
        <a href="javascript:void(0)" ref="pluginsButton" class="doctag badge badge-secondary" data-toggle="popover" title="Plugins" data-placement="left">@</a>
        <span v-if="docTags">
          <a href="http://google.com" v-bind:key="tag" v-for="tag of docTags.unchanged" class="doctag badge badge-primary">#{{tag}}</a><a href="http://google.com" v-bind:key="tag" v-for="tag of docTags.added" class="doctag badge badge-success">#{{tag}}*</a><a href="http://google.com" v-bind:key="tag" v-for="tag of docTags.removed" class="doctag badge badge-danger">#{{tag}}</a>
        </span>
      </div>
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
      newDocName: '',
      newDocState: false
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
    newDocInitiate () {
      this.newDocName = ''
      this.newDocState = true
      this.$nextTick(() => this.$refs.newDocName.focus())
    },
    newDoc () {
      const win = window.open(`/api/docs/new?hintName=${encodeURI(this.newDocName)}`, '_blank')
      this.newDocName = ''
      this.newDocState = false
      win.focus()
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

    window.onbeforeunload = event => {
      if (this.cm.isClean(this.cleanGeneration)) {
        return null
      }
      const dialogText = 'You have some unsaved changes. Click "Save" or press "Ctrl-S" in order to save your changes.'
      event.returnValue = dialogText
      return dialogText
    }

    $(window).bind('keydown', event => {
      if ((event.ctrlKey || event.metaKey) &&
          String.fromCharCode(event.which).toLowerCase() === 's') {
        event.preventDefault()
        this.save()
      }
    })

    this.loadDoc(this.$route.params.docId)
  },
  mounted () {
    function betterTab (cm) {
      if (cm.somethingSelected()) {
        cm.indentSelection('add')
      } else {
        cm.replaceSelection(cm.getOption('indentWithTabs') ? '\t'
          : Array(cm.getOption('indentUnit') + 1).join(' '), 'end', '+input')
      }
    }

    const self = this

    $(function () { $(self.$refs.pluginsButton).popover({ content: self.pluginsClick, html: true }) })

    CodeMirror.commands.save = function (insance) {
      self.save()
    }

    this.cm = CodeMirror(this.$refs.codeEditor, {
      lineNumbers: true,
      lineWrapping: true,
      extraKeys: { Tab: betterTab },
      indentUnit: 4,
      value: '',
      mode: 'gfm',
      theme: 'neo'
    })

    this.cm.on('changes', (cm, changes) => {
      changes.forEach(change => {
        if (!self.plugins.hashtags.staged) {
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

.doc-name {
  margin-bottom: 0;
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
  border-bottom: dashed 0.1em #28a745;
}

.CodeMirror .cm-header-2 {
  font-size: 1.7em;
  border-bottom: dashed 0.1em #28a745;
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

.CodeMirror .cm-link {
  background-position: center right;
  background-repeat: no-repeat;
  background-image: linear-gradient(transparent,transparent),url("data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2212%22 height=%2212%22%3E %3Cpath fill=%22%23fff%22 stroke=%22%23333%22 d=%22M1.5 4.518h5.982V10.5H1.5z%22/%3E %3Cpath fill=%22%23333%22 d=%22M5.765 1H11v5.39L9.427 7.937l-1.31-1.31L5.393 9.35l-2.69-2.688 2.81-2.808L4.2 2.544z%22/%3E %3Cpath fill=%22%23fff%22 d=%22M9.995 2.004l.022 4.885L8.2 5.07 5.32 7.95 4.09 6.723l2.882-2.88-1.85-1.852z%22/%3E %3C/svg%3E");
  padding-right: 13px;
}

.CodeMirror .cm-quote.cm-quote-1 {
  background-color: #E0E3DA;
}

.CodeMirror-cursor {
  border-left: 1px solid black !important;
  background: rgba(0,0,0,0) !important;
}
</style>
