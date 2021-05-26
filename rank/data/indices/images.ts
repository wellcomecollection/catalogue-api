export default {
  mappings: {
    dynamic: 'strict',
    properties: {
      locations: {
        dynamic: 'false',
        properties: {
          license: {
            properties: {
              id: {
                type: 'keyword',
              },
            },
          },
        },
      },
      modifiedTime: {
        type: 'date',
      },
      source: {
        properties: {
          canonicalWork: {
            dynamic: 'false',
            properties: {
              data: {
                properties: {
                  alternativeTitles: {
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
                      path: {
                        type: 'text',
                      },
                    },
                  },
                  contributors: {
                    properties: {
                      agent: {
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
                  description: {
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
                  edition: {
                    type: 'text',
                  },
                  genres: {
                    properties: {
                      concepts: {
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
                  languages: {
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
                    properties: {
                      content: {
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
                    },
                  },
                  otherIdentifiers: {
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
                    properties: {
                      agents: {
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
                      function: {
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
                      places: {
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
                    properties: {
                      concepts: {
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
                  title: {
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
                },
              },
              id: {
                properties: {
                  canonicalId: {
                    type: 'keyword',
                    normalizer: 'lowercase_normalizer',
                  },
                  sourceIdentifier: {
                    dynamic: 'false',
                    properties: {
                      value: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                  },
                },
              },
              type: {
                type: 'keyword',
              },
            },
          },
          redirectedWork: {
            dynamic: 'false',
            properties: {
              data: {
                properties: {
                  alternativeTitles: {
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
                      path: {
                        type: 'text',
                      },
                    },
                  },
                  contributors: {
                    properties: {
                      agent: {
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
                  description: {
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
                  edition: {
                    type: 'text',
                  },
                  genres: {
                    properties: {
                      concepts: {
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
                  languages: {
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
                    properties: {
                      content: {
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
                    },
                  },
                  otherIdentifiers: {
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
                    properties: {
                      agents: {
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
                      function: {
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
                      places: {
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
                    properties: {
                      concepts: {
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
                  title: {
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
                },
              },
              id: {
                properties: {
                  canonicalId: {
                    type: 'keyword',
                    normalizer: 'lowercase_normalizer',
                  },
                  sourceIdentifier: {
                    dynamic: 'false',
                    properties: {
                      value: {
                        type: 'keyword',
                        normalizer: 'lowercase_normalizer',
                      },
                    },
                  },
                },
              },
              type: {
                type: 'keyword',
              },
            },
          },
          type: {
            type: 'keyword',
          },
        },
      },
      state: {
        properties: {
          canonicalId: {
            type: 'keyword',
            normalizer: 'lowercase_normalizer',
          },
          derivedData: {
            dynamic: 'false',
            properties: {
              sourceContributorAgents: {
                type: 'keyword',
              },
            },
          },
          inferredData: {
            properties: {
              aspectRatio: {
                type: 'float',
              },
              binMinima: {
                type: 'float',
                index: false,
              },
              binSizes: {
                type: 'integer',
                index: false,
              },
              features1: {
                type: 'dense_vector',
                dims: 2048,
              },
              features2: {
                type: 'dense_vector',
                dims: 2048,
              },
              lshEncodedFeatures: {
                type: 'keyword',
              },
              palette: {
                type: 'keyword',
              },
            },
          },
          sourceIdentifier: {
            dynamic: 'false',
            properties: {
              value: {
                type: 'keyword',
                normalizer: 'lowercase_normalizer',
              },
            },
          },
        },
      },
      version: {
        type: 'integer',
      },
    },
  },
  settings: {
    index: {
      analysis: {
        filter: {
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
          asciifolding_token_filter: {
            type: 'asciifolding',
            preserve_original: 'true',
          },
        },
        normalizer: {
          lowercase_normalizer: {
            filter: ['lowercase'],
            type: 'custom',
          },
        },
        analyzer: {
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
