import languages from '../languages'

export default {
  bool: {
    minimum_should_match: '1',
    should: [
      {
        multi_match: {
          _name: 'identifiers',
          analyzer: 'whitespace_analyzer',
          fields: [
            'state.canonicalId^1000.0',
            'state.sourceIdentifier.value^1000.0',
            'data.otherIdentifiers.value^1000.0',
            'data.items.id.canonicalId^1000.0',
            'data.items.id.sourceIdentifier.value^1000.0',
            'data.items.id.otherIdentifiers.value^1000.0',
            'data.imageData.id.canonicalId^1000.0',
            'data.imageData.id.sourceIdentifier.value^1000.0',
            'data.imageData.id.otherIdentifiers.value^1000.0',
          ],
          operator: 'Or',
          query: '{{query}}',
          type: 'best_fields',
        },
      },
      {
        dis_max: {
          queries: [
            {
              multi_match: {
                _name: 'title and contributor exact spellings',
                fields: [
                  'search.titlesAndContributors^100.0',
                  'search.titlesAndContributors.english^100.0',
                  'search.titlesAndContributors.shingles^100.0',
                ],
                operator: 'And',
                query: '{{query}}',
                type: 'best_fields',
              },
            },
            {
              multi_match: {
                _name: 'title and contributor alternative spellings',
                fields: [
                  'search.titlesAndContributors^80.0',
                  'search.titlesAndContributors.english^80.0',
                  'search.titlesAndContributors.shingles^80.0',
                ],
                fuzziness: 'AUTO',
                operator: 'And',
                query: '{{query}}',
                type: 'best_fields',
                prefix_length: '2',
              },
            },
            {
              multi_match: {
                _name: 'non-english titles and contributors',
                fields: languages.map(
                  (language) => `search.titlesAndContributors.${language}`
                ),
                query: '{{query}}',
                operator: 'And',
                type: 'best_fields',
              },
            },
          ],
        },
      },
      {
        match: {
          'search.relations': {
            _name: 'relations',
            query: '{{query}}',
            operator: 'AND',
            boost: 1000,
          },
        },
      },
      {
        multi_match: {
          _name: 'data',
          fields: [
            'data.contributors.agent.label^1000.0',
            'data.subjects.concepts.label^10.0',
            'data.genres.concepts.label^10.0',
            'data.production.*.label^10.0',
            'data.description',
            'data.physicalDescription',
            'data.language.label',
            'data.edition',
            'data.notes.content',
            'data.lettering',
          ],
          operator: 'And',
          query: '{{query}}',
          type: 'cross_fields',
        },
      },
    ],
  },
}
