const sort = [
  {
    'state.canonicalId': {
      order: 'asc',
    },
  },
]

const aggs = {
  format: {
    terms: {
      field: 'data.format.id',
      size: 23,
    },
  },
  availabilities: {
    terms: {
      field: 'state.availabilities.id',
      size: 2,
    },
  },
  genres: {
    terms: {
      field: 'data.genres.concepts.label.keyword',
      size: 20,
    },
  },
  languages: {
    terms: {
      field: 'data.languages.id',
      size: 200,
    },
  },
  subjects: {
    terms: {
      field: 'data.subjects.label.keyword',
      size: 20,
    },
  },
  contributors: {
    terms: {
      field: 'state.derivedData.contributorAgents',
      size: 20,
    },
  },
}

export { sort, aggs }
