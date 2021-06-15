const query = {
  bool: {
    should: [
      {
        multi_match: {
          query: '{{query}}',
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
          type: 'best_fields',
          analyzer: 'whitespace_analyzer',
          operator: 'Or',
          _name: 'identifiers',
        },
      },
      {
        dis_max: {
          queries: [
            // {
            //   bool: {
            //     must: [
            //       {
            //         prefix: {
            //           'data.title.keyword': {
            //             value: '{{query}}',
            //           },
            //         },
            //       },
            //       {
            //         match_phrase: {
            //           'data.title': {
            //             query: '{{query}}',
            //           },
            //         },
            //       },
            //     ],
            //     boost: 1000,
            //     _name: 'title prefix',
            //   },
            // },
            {
              multi_match: {
                query: '{{query}}',
                fields: [
                  'data.title^100.0',
                  'data.title.english^100.0',
                  'data.title.shingles^100.0',
                  'data.alternativeTitles^100.0',
                ],
                type: 'best_fields',
                operator: 'And',
                _name: 'title exact spellings',
              },
            },
            {
              multi_match: {
                query: '{{query}}',
                fields: [
                  'data.title^80.0',
                  'data.title.english^80.0',
                  'data.title.shingles^80.0',
                  'data.alternativeTitles^80.0',
                ],
                type: 'best_fields',
                fuzziness: 'AUTO',
                operator: 'And',
                _name: 'title alternative spellings',
              },
            },
            {
              multi_match: {
                query: '{{query}}',
                fields: [
                  'data.title.arabic',
                  'data.title.bengali',
                  'data.title.french',
                  'data.title.german',
                  'data.title.hindi',
                  'data.title.italian',
                ],
                type: 'best_fields',
                operator: 'And',
                _name: 'non-english titles',
              },
            },
          ],
        },
      },
      {
        multi_match: {
          query: '{{query}}',
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
            'data.collectionPath.path',
            'data.collectionPath.label',
            'data.lettering',
          ],
          type: 'cross_fields',
          operator: 'And',
          _name: 'data',
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
    minimum_should_match: '1',
  },
}

export default query
