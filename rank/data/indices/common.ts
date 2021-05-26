const asciifoldedFields = {
  asciifolded: {
    type: 'text',
    analyzer: 'shingle_asciifolding_analyzer',
  },
}

const shinglesFields = {
  shingles: {
    type: 'text',
    analyzer: 'shingle_asciifolding_analyzer',
  },
}

const multilingualFields = {
  arabic: {
    type: 'text',
    analyzer: 'arabic_analyzer',
  },
  bengali: {
    type: 'text',
    analyzer: 'bengali_analyzer',
  },
  english: {
    type: 'text',
    analyzer: 'english_analyzer',
  },
  french: {
    type: 'text',
    analyzer: 'french_analyzer',
  },
  german: {
    type: 'text',
    analyzer: 'german_analyzer',
  },
  hindi: {
    type: 'text',
    analyzer: 'hindi_analyzer',
  },
  italian: {
    type: 'text',
    analyzer: 'italian_analyzer',
  },
  keyword: {
    type: 'keyword',
    normalizer: 'lowercase_normalizer',
  },
}

export { multilingualFields, shinglesFields, asciifoldedFields }
