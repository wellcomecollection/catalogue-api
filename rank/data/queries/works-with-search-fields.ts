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
              bool: {
                _name: 'title prefix',
                boost: 1000.0,
                must: [
                  {
                    prefix: {
                      'data.title.keyword': {
                        value: '{{query}}',
                      },
                    },
                  },
                  {
                    match_phrase: {
                      'data.title': {
                        query: '{{query}}',
                      },
                    },
                  },
                ],
              },
            },
            {
              multi_match: {
                _name: 'title and contributor exact spellings',
                fields: [
                  'search.titlesAndContributors.^100.0',
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
            operator: 'And',
            boost: 1000.0,
          },
        },
      },
      {
        multi_match: {
          _name: 'data',
          fields: [
            'data.contributors.agent.label^1000.0',
            'data.description',
            'data.edition',
            'data.genres.concepts.label^10.0',
            'data.language.label',
            'data.lettering',
            'data.notes.content',
            'data.physicalDescription',
            'data.production.*.label^10.0',
            'data.subjects.concepts.label^10.0',
          ],
          operator: 'And',
          query: '{{query}}',
          type: 'cross_fields',
        },
      },
    ],
    filter: [
      {
        term: {
          type: {
            value: 'Visible',
          },
        },
      },
    ],
  },
}
