/* eslint-disable camelcase */

const char_filter = {
  // This analyzer "keeps" the slash, by turning it into
  // `__` which isn't removed by the standard tokenizer
  with_slashes_char_filter: {
    type: 'mapping',
    mappings: ['/ => __'],
  },
}

const filter = {
  asciifolding_token_filter: {
    type: 'asciifolding',
    preserve_original: 'true',
  },
}

// This should never really be used as you can just use
// "analyzer": "standard"
// it's more if you would like to extend or modify just parts of it
const standard_analyzer = {
  tokenizer: 'standard',
  filter: ['lowercase'],
}

const analyzer = {
  with_slashes_text_analyzer: {
    ...standard_analyzer,
    char_filter: ['with_slashes_char_filter'],
    filter: [...standard_analyzer.filter, 'asciifolding_token_filter'],
  },
}

export { analyzer, char_filter, filter }
