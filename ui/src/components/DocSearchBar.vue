<template>
<!--   <div>
    <div class="input-group input-group" style="flex-grow: 1;">
      <input type="text" class="form-control" placeholder="Search" aria-label="Search">
      <div class="input-group-append">
        <button class="btn btn-primary" type="button"><ficon icon="search" /></button>
      </div>
    </div>
    <ul>
      <li  class="list-group-item list-group-item-action doc-suggestions"></li>
    </ul>
  </div> -->
  <div class="list-group-flush">
    <vue-autosuggest
      :suggestions="filteredDocs"
      :renderSuggestion="renderSuggestion"
      :onSelected="onSelected"
      :inputProps="{id: 'autosuggest__input', onInputChange: this.onInputChange, placeholder: 'Search'}" /> <!-- "" -->
  </div>
  <!-- <vue-autosuggest
    :suggestions="filteredOptions"
    @focus="focusMe"
    @click="clickHandler"
    :onSelected="onSelected"
    :renderSuggestion="renderSuggestion"
    :getSuggestionValue="getSuggestionValue"
    :inputProps="{id:'autosuggest__input', onInputChange: this.onInputChange, placeholder:''}" /> -->
</template>

<script>
import axios from 'axios'
import _ from 'lodash'

export default {
  name: 'DocSearchBar',
  data () {
    return {
      filteredDocs: [],
      docs: [],
      selected: ''
    }
  },
  created () {
    axios.get('/api/docs')
      .then(response => {
        this.docs = [{data: response.data}]
      })
      .catch(console.error)
  },
  methods: {
    // _empty: _.empty,
    onSelected (suggestion) {
      this.$router.push({ name: 'edit_doc', params: { docId: suggestion.item.id } })
      // Optionally open a new tab for this document:
      // const routeData = this.$router.resolve({name: 'routeName', query: {data: "someData"}});
      // window.open(routeData.href, '_blank');
    },
    renderSuggestion (suggestion) {
      
      const n = this.$createElement('div', { class: 'doc-suggestions' }, [suggestion.item.name])

      // n.innerHTML = 
      // n.className = 'badge badge-secondary'

      return n
    },
    onInputChange (text, oldText) {
      if (text === null) {
        return
      }

      if (_.isEmpty(this.docs)) {
        return
      }

      const filteredData = this.docs[0].data.filter(doc => {
        return doc.name.toLowerCase().includes(text) ||
          _.find(_.get(doc, 'metadata.hashtags.tags', []), t => t.toLowerCase().includes(text))
      })

      this.filteredDocs = [{data: filteredData.slice(0, 5)}]
    }
  }
}
</script>

<style scoped>
</style>

<style>
.autosuggest__results-container {
  position: relative;
}

.autosuggest__results-container ul {
  list-style: none;
  padding-left: 0;
}

.autosuggest__results {
  background-color: white;
  position: absolute;
  width: 100%;
  border: #b7b7b7 1px solid;
  border-bottom-right-radius: .15rem;
  border-bottom-left-radius: .15rem;
  padding-bottom: .rem;
}

.autosuggest__results_item-highlighted {
  background-color: #78ddae;
  font-weight: bold;
}

.doc-suggestions:hover {
  background-color: #78ddae;
  font-weight: bold;
}

.doc-suggestions {
  padding: .3rem .75rem
}
</style>
