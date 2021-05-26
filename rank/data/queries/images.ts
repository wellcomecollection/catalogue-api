import languages from '../languages'

export default {
  bool: {
    should: [
      {
        multi_match: {
          query: '{{query}}',
          fields: [
            'source.canonicalWork.data.collectionPath.label',
            'source.canonicalWork.data.collectionPath.path',
            'source.canonicalWork.data.contributors.agent.label^1000.0',
            'source.canonicalWork.data.description',
            'source.canonicalWork.data.edition',
            'source.canonicalWork.data.genres.concepts.label^10.0',
            'source.canonicalWork.data.language.label',
            'source.canonicalWork.data.physicalDescription',
            'source.canonicalWork.data.production.*.label^10.0',
            'source.canonicalWork.data.subjects.concepts.label^10.0',
            'source.redirectedWork.data.collectionPath.label',
            'source.redirectedWork.data.collectionPath.path',
            'source.redirectedWork.data.contributors.agent.label^1000.0',
            'source.redirectedWork.data.description',
            'source.redirectedWork.data.edition',
            'source.redirectedWork.data.genres.concepts.label^10.0',
            'source.redirectedWork.data.language.label',
            'source.redirectedWork.data.physicalDescription',
            'source.redirectedWork.data.production.*.label^10.0',
            'source.redirectedWork.data.subjects.concepts.label^10.0',
          ],
          type: 'cross_fields',
          operator: 'And',
        },
      },
      {
        dis_max: {
          queries: [
            {
              bool: {
                _name: 'redirected title prefix',
                boost: 1000.0,
                must: [
                  {
                    prefix: {
                      'source.redirectedWork.data.title.keyword': {
                        value: '{{query}}',
                      },
                    },
                  },
                  {
                    match_phrase: {
                      'source.redirectedWork.data.title': {
                        query: '{{query}}',
                      },
                    },
                  },
                ],
              },
            },
            {
              bool: {
                _name: 'canonical title prefix',
                boost: 1000.0,
                must: [
                  {
                    prefix: {
                      'source.canonicalWork.data.title.keyword': {
                        value: '{{query}}',
                      },
                    },
                  },
                  {
                    match_phrase: {
                      'source.canonicalWork.data.title': {
                        query: '{{query}}',
                      },
                    },
                  },
                ],
              },
            },
            {
              multi_match: {
                _name: 'title exact spellings',
                fields: [
                  'source.canonicalWork.data.alternativeTitles^100.0',
                  'source.canonicalWork.data.title.english^100.0',
                  'source.canonicalWork.data.title.shingles^100.0',
                  'source.canonicalWork.data.title^100.0',
                  'source.redirectedWork.data.alternativeTitles^100.0',
                  'source.redirectedWork.data.title.english^100.0',
                  'source.redirectedWork.data.title.shingles^100.0',
                  'source.redirectedWork.data.title^100.0',
                ],
                operator: 'And',
                query: '{{query}}',
                type: 'best_fields',
              },
            },
            {
              multi_match: {
                _name: 'title alternative spellings',
                fields: [
                  'source.canonicalWork.data.alternativeTitles^80.0',
                  'source.canonicalWork.data.title.english^80.0',
                  'source.canonicalWork.data.title.shingles^80.0',
                  'source.canonicalWork.data.title^80.0',
                  'source.redirectedWork.data.alternativeTitles^80.0',
                  'source.redirectedWork.data.title.english^80.0',
                  'source.redirectedWork.data.title.shingles^80.0',
                  'source.redirectedWork.data.title^80.0',
                ],
                fuzziness: 'AUTO',
                operator: 'And',
                query: '{{query}}',
                type: 'best_fields',
              },
            },
            {
              multi_match: {
                _name: 'non-english text',
                fields: languages.flatMap((language) => [
                  `source.canonicalWork.data.title.${language}`,
                  `source.canonicalWork.data.notes.${language}`,
                  `source.canonicalWork.data.lettering.${language}`,
                  `source.redirectedWork.data.title.${language}`,
                  `source.redirectedWork.data.notes.${language}`,
                  `source.redirectedWork.data.lettering.${language}`,
                ]),
                query: '{{query}}',
                operator: 'And',
                type: 'best_fields',
              },
            },
          ],
        },
      },
      {
        multi_match: {
          query: '{{query}}',
          _name: 'identifiers',
          fields: [
            'source.canonicalWork.data.otherIdentifiers.value',
            'source.canonicalWork.id.canonicalId',
            'source.canonicalWork.id.sourceIdentifier.value',
            'source.redirectedWork.data.otherIdentifiers.value',
            'source.redirectedWork.id.canonicalId',
            'source.redirectedWork.id.sourceIdentifier.value',
            'state.canonicalId',
            'state.sourceIdentifier.value',
          ],
          type: 'best_fields',
          analyzer: 'whitespace_analyzer',
          operator: 'Or',
          boost: 1000.0,
        },
      },
    ],
    minimum_should_match: '1',
  },
}
