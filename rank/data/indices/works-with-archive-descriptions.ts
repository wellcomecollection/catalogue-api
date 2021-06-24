/* eslint-disable camelcase */

import { analyzer, char_filter, filter } from './analysis'
import { multilingualFields, shinglesFields } from './common'

export default {
  mappings: {
    dynamic: 'strict',
    properties: {
      search: {
        dynamic: 'false',
        type: "object",
        properties: {

          relations: {
            store: true,
            type: 'text',
            analyzer: 'with_slashes_text_analyzer',
          },
          titlesAndContributors: {
            store: true,
            type: 'text',
            fields: {
              ...multilingualFields,
              ...shinglesFields,
            },
          },
        },
      },
      data: {
        dynamic: 'false',
        type: "object",
        properties: {
          alternativeTitles: {
            copy_to: ['search.relations', 'search.titlesAndContributors'],
            type: 'text',
            fields: {
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
              shingles: {
                type: 'text',
                analyzer: 'shingle_asciifolding_analyzer',
              },
            },
          },
          collectionPath: {
            type: "object",
            properties: {
              depth: {
                type: 'token_count',
                analyzer: 'standard',
              },
              label: {
                type: 'text',
                copy_to: ['search.relations'],
              },
              path: {
                type: 'text',
                fields: {
                  keyword: {
                    type: 'keyword',
                  },
                },
                copy_to: ['data.collectionPath.depth', 'search.relations'],
                analyzer: 'path_hierarchy_analyzer',
              },
            },
          },
          contributors: {
            type: "object",
            properties: {
              agent: {
                type: "object",
                properties: {

                  label: {
                    copy_to: ['search.titlesAndContributors'],
                    type: 'text',
                    fields: {
                      keyword: {
                        type: 'keyword',
                      },
                      lowercaseKeyword: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                    analyzer: 'asciifolding_analyzer',
                  },
                },
              },
            },
          },
          description: {
            copy_to: ['search.relations'],
            type: 'text',
            fields: {
              english: {
                type: 'text',
                analyzer: 'english',
              },
            },
          },
          duration: {
            type: 'integer',
          },
          edition: {
            type: 'text',
          },
          format: {
            type: "object",
            properties: {
              id: {
                type: 'keyword',
              },
            },
          },
          genres: {
            type: "object",
            properties: {
              concepts: {
                type: "object",
                properties: {
                  label: {
                    type: 'text',
                    fields: {
                      keyword: {
                        type: 'keyword',
                      },
                      lowercaseKeyword: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                    analyzer: 'asciifolding_analyzer',
                  },
                },
              },
              label: {
                type: 'text',
                fields: {
                  keyword: {
                    type: 'keyword',
                  },
                  lowercaseKeyword: {
                    type: 'keyword',
                    normalizer: 'lowercase_normalizer',
                  },
                },
                analyzer: 'asciifolding_analyzer',
              },
            },
          },
          imageData: {
            type: "object",
            properties: {
              id: {
                type: "object",
                properties: {
                  canonicalId: {
                    type: 'keyword',
                    normalizer: 'lowercase_normalizer',
                  },
                  sourceIdentifier: {
                    dynamic: 'false',
                    type: "object",
                    properties: {
                      value: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                  },
                },
              },
            },
          },
          items: {
            type: "object",
            properties: {
              id: {
                type: "object",
                properties: {
                  canonicalId: {
                    type: 'keyword',
                    normalizer: 'lowercase_normalizer',
                  },
                  otherIdentifiers: {
                    type: "object",
                    properties: {
                      value: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                  },
                  sourceIdentifier: {
                    dynamic: 'false',
                    type: "object",
                    properties: {
                      value: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                  },
                },
              },
              locations: {
                type: "object",
                properties: {
                  accessConditions: {
                    type: "object",
                    properties: {
                      status: {
                        type: "object",
                        properties: {
                          type: {
                            type: 'keyword',
                          },
                        },
                      },
                    },
                  },
                  license: {
                    type: "object",
                    properties: {
                      id: {
                        type: 'keyword',
                      },
                    },
                  },
                  locationType: {
                    type: "object",
                    properties: {
                      id: {
                        type: 'keyword',
                      },
                    },
                  },
                  type: {
                    type: 'keyword',
                  },
                },
              },
            },
          },
          languages: {
            type: "object",
            properties: {
              id: {
                type: 'keyword',
              },
              label: {
                type: 'text',
                fields: {
                  keyword: {
                    type: 'keyword',
                  },
                  lowercaseKeyword: {
                    type: 'keyword',
                    normalizer: 'lowercase_normalizer',
                  },
                },
                analyzer: 'asciifolding_analyzer',
              },
            },
          },
          lettering: {
            type: 'text',
            fields: {
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
              shingles: {
                type: 'text',
                analyzer: 'shingle_asciifolding_analyzer',
              },
            },
          },
          notes: {
            type: "object",
            properties: {
              content: {
                type: 'text',
                fields: {
                  english: {
                    type: 'text',
                    analyzer: 'english',
                  },
                },
              },
            },
          },
          otherIdentifiers: {
            type: "object",
            properties: {
              value: {
                type: 'keyword',
                normalizer: 'lowercase_normalizer',
              },
            },
          },
          physicalDescription: {
            type: 'text',
            fields: {
              english: {
                type: 'text',
                analyzer: 'english',
              },
              keyword: {
                type: 'keyword',
              },
            },
          },
          production: {
            type: "object",
            properties: {
              agents: {
                type: "object",
                properties: {
                  label: {
                    type: 'text',
                    fields: {
                      keyword: {
                        type: 'keyword',
                      },
                      lowercaseKeyword: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                    analyzer: 'asciifolding_analyzer',
                  },
                },
              },
              dates: {
                type: "object",
                properties: {
                  label: {
                    type: 'text',
                    fields: {
                      keyword: {
                        type: 'keyword',
                      },
                      lowercaseKeyword: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                    analyzer: 'asciifolding_analyzer',
                  },
                  range: {
                    type: "object",
                    properties: {
                      from: {
                        type: 'date',
                      },
                    },
                  },
                },
              },
              function: {
                type: "object",
                properties: {
                  label: {
                    type: 'text',
                    fields: {
                      keyword: {
                        type: 'keyword',
                      },
                      lowercaseKeyword: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                    analyzer: 'asciifolding_analyzer',
                  },
                },
              },
              label: {
                type: 'text',
                fields: {
                  keyword: {
                    type: 'keyword',
                  },
                  lowercaseKeyword: {
                    type: 'keyword',
                    normalizer: 'lowercase_normalizer',
                  },
                },
                analyzer: 'asciifolding_analyzer',
              },
              places: {
                type: "object",
                properties: {
                  label: {
                    type: 'text',
                    fields: {
                      keyword: {
                        type: 'keyword',
                      },
                      lowercaseKeyword: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                    analyzer: 'asciifolding_analyzer',
                  },
                },
              },
            },
          },
          subjects: {
            type: "object",
            properties: {
              concepts: {
                type: "object",
                properties: {
                  label: {
                    type: 'text',
                    fields: {
                      keyword: {
                        type: 'keyword',
                      },
                      lowercaseKeyword: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                    analyzer: 'asciifolding_analyzer',
                  },
                },
              },
              label: {
                type: 'text',
                fields: {
                  keyword: {
                    type: 'keyword',
                  },
                  lowercaseKeyword: {
                    type: 'keyword',
                    normalizer: 'lowercase_normalizer',
                  },
                },
                analyzer: 'asciifolding_analyzer',
              },
            },
          },
          title: {
            copy_to: ['search.relations', 'search.titlesAndContributors'],
            type: 'text',
            fields: {
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
              shingles: {
                type: 'text',
                analyzer: 'shingle_asciifolding_analyzer',
              },
            },
          },
          workType: {
            type: 'keyword',
          },
        },
      },
      deletedReason: {
        dynamic: 'false',
        type: "object",
        properties: {
          type: {
            type: 'keyword',
          },
        },
      },
      invisibilityReasons: {
        dynamic: 'false',
        type: "object",
        properties: {
          type: {
            type: 'keyword',
          },
        },
      },
      redirectSources: {
        type: 'object',
        dynamic: 'false',
      },
      redirectTarget: {
        type: 'object',
        dynamic: 'false',
      },
      state: {
        type: "object",
        properties: {
          availabilities: {
            type: "object",
            properties: {

              id: {
                type: 'keyword',
              },
            },
          },
          canonicalId: {
            type: 'keyword',
            normalizer: 'lowercase_normalizer',
          },
          derivedData: {
            dynamic: 'false',
            type: "object",
            properties: {
              contributorAgents: {
                type: 'keyword',
              },
            },
          },
          indexedTime: {
            type: 'date',
          },
          mergedTime: {
            type: 'date',
          },
          relations: {
            dynamic: 'false',
            type: "object",
            properties: {
              ancestors: {
                type: "object",
                properties: {
                  collectionPath: {
                    type: "object",
                    properties: {
                      depth: {
                        type: 'token_count',
                        analyzer: 'standard',
                      },
                      label: {
                        type: 'text',
                        copy_to: ['search.relations'],
                      },
                      path: {
                        type: 'text',
                        fields: {
                          keyword: {
                            type: 'keyword',
                          },
                        },
                        analyzer: 'path_hierarchy_analyzer',
                        copy_to: ['search.relations'],
                      },
                    },
                  },
                  depth: {
                    type: 'integer',
                  },
                  id: {
                    type: 'keyword',
                    normalizer: 'lowercase_normalizer',
                  },
                  numChildren: {
                    type: 'integer',
                  },
                  numDescendents: {
                    type: 'integer',
                  },
                  title: {
                    type: 'text',
                    copy_to: ['search.relations'],
                    fields: {
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
                      shingles: {
                        type: 'text',
                        analyzer: 'shingle_asciifolding_analyzer',
                      },
                    },
                  },
                },
              },
            },
          },
          sourceIdentifier: {
            dynamic: 'false',
            type: "object",
            properties: {
              value: {
                type: 'keyword',
                normalizer: 'lowercase_normalizer',
              },
            },
          },
          sourceModifiedTime: {
            type: 'date',
          },
        },
      },
      type: {
        type: 'keyword',
      },
      version: {
        type: 'integer',
      },
    },
  },
  settings: {
    index: {
      analysis: {
        char_filter,
        filter: {
          ...filter,
          arabic_token_filter: {
            name: 'arabic',
            type: 'stemmer',
          },
          english_token_filter: {
            name: 'english',
            type: 'stemmer',
          },
          french_token_filter: {
            name: 'french',
            type: 'stemmer',
          },
          italian_token_filter: {
            name: 'italian',
            type: 'stemmer',
          },
          shingle_token_filter: {
            max_shingle_size: '4',
            min_shingle_size: '2',
            type: 'shingle',
          },
          hindi_token_filter: {
            name: 'hindi',
            type: 'stemmer',
          },
          english_possessive_token_filter: {
            name: 'possessive_english',
            type: 'stemmer',
          },
          german_token_filter: {
            name: 'german',
            type: 'stemmer',
          },
          bengali_token_filter: {
            name: 'bengali',
            type: 'stemmer',
          },
        },
        normalizer: {
          lowercase_normalizer: {
            filter: ['lowercase'],
            type: 'custom',
          },
        },
        analyzer: {
          ...analyzer,
          hindi_analyzer: {
            filter: ['lowercase', 'hindi_token_filter'],
            type: 'custom',
            tokenizer: 'standard',
          },
          asciifolding_analyzer: {
            filter: ['lowercase', 'asciifolding_token_filter'],
            type: 'custom',
            tokenizer: 'standard',
          },
          shingle_asciifolding_analyzer: {
            filter: [
              'lowercase',
              'shingle_token_filter',
              'asciifolding_token_filter',
            ],
            type: 'custom',
            tokenizer: 'standard',
          },
          french_analyzer: {
            filter: ['lowercase', 'french_token_filter'],
            type: 'custom',
            tokenizer: 'standard',
          },
          path_hierarchy_analyzer: {
            type: 'custom',
            tokenizer: 'path_hierarchy_tokenizer',
          },
          whitespace_analyzer: {
            type: 'custom',
            tokenizer: 'whitespace',
          },
          italian_analyzer: {
            filter: ['lowercase', 'italian_token_filter'],
            type: 'custom',
            tokenizer: 'standard',
          },
          arabic_analyzer: {
            filter: ['lowercase', 'arabic_token_filter'],
            type: 'custom',
            tokenizer: 'standard',
          },
          english_analyzer: {
            filter: [
              'lowercase',
              'english_token_filter',
              'english_possessive_token_filter',
            ],
            type: 'custom',
            tokenizer: 'standard',
          },
          bengali_analyzer: {
            filter: ['lowercase', 'bengali_token_filter'],
            type: 'custom',
            tokenizer: 'standard',
          },
          german_analyzer: {
            filter: ['lowercase', 'german_token_filter'],
            type: 'custom',
            tokenizer: 'standard',
          },
        },
        tokenizer: {
          path_hierarchy_tokenizer: {
            type: 'path_hierarchy',
            replacement: '/',
            delimiter: '/',
          },
        },
      },
    },
  },
}
