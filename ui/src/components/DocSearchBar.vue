<template>
  <vue-autosuggest
    :suggestions="filteredDocs"
    :renderSuggestion="renderSuggestion"
    :onSelected="onSelected"
    :inputProps="{id: 'autosuggest__input', onInputChange: this.onInputChange, placeholder: 'Search'}" /> <!--""-->
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
    onSelected (suggestion) {
      this.$router.push({ name: 'edit_doc', params: { docId: suggestion.item.id } })
      // Optionally open a new tab for this document:
      // const routeData = this.$router.resolve({name: 'routeName', query: {data: "someData"}});
      // window.open(routeData.href, '_blank');
    },
    renderSuggestion (suggestion) {
      return suggestion.item.name
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

.autosuggest__results {
  position: absolute;
  background-color: powderblue;
  width: 100%;
}

.autosuggest__results_item-highlighted {
  background-color: aquamarine;
}
</style>
