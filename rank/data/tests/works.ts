import { equalTo0, equalTo1 } from './pass'

import { Test } from '../../types/test'
import { filterCaseRatings } from './queryAugmentation'

const tests: Test[] = [
  {
    id: 'precision',
    label: 'Precision',
    description:
      'Ensure that the query brings back the works we expect at the top of the list',
    eval: equalTo1,
    cases: [
      {
        query: 'Cassils Time lapse',
        ratings: ['ftqy78zj'],
        description: 'Contributor and title in the same query',
      },
      {
        query: 'bulloch history of bacteriology',
        ratings: ['rkcux48q'],
        description: 'Contributor and title in the same query',
      },
      {
        query: 'stim',
        ratings: ['e8qxq5mv'],
        description:
          'Exact match on title with lowercasing and punctuation stripping',
      },
      {
        query: 'stimming',
        ratings: ['uuem7v9a'],
        description:
          'Ensure that we return non-typos over typos e.g. query:stimming matches:stimming > swimming',
      },
      {
        query: 'The Piggle',
        ratings: ['vp7q52gs'],
        knownFailure: true,
        description:
          "Example of a known title's prefix, but not the full thing",
      },
      {
        query: 'Das neue Naturheilverfahren',
        ratings: ['execg22x'],
        description:
          "Example of a known title's prefix, but not the full thing",
      },
      { query: 'bills of mortality', ratings: ['xwtcsk93'] },
      {
        query: 'L0033046',
        ratings: ['kmebmktz'],
        description: 'Miro ID matching',
      },
      {
        query: 'kmebmktz',
        ratings: ['kmebmktz'],
        description: 'Work ID matching',
      },
      {
        query: 'gzv2hhgy',
        ratings: ['kmebmktz'],
        description: 'Image ID matching',
      },
      {
        query: 'Oxford dictionary of national biography',
        ratings: ['ruedafcw'],
        description:
          "Example of a known title's prefix, but not the full thing",
      },
    ],
    metric: {
      precision: {
        relevant_rating_threshold: 3,
        k: 1,
      },
    },
  },
  {
    id: 'recall',
    label: 'Recall',
    description:
      'Ensure that the query brings back the results we expect somewhere in the list',
    eval: equalTo1,
    cases: [
      {
        query: 'Atherosclerosis : an introduction to atherosclerosis',
        ratings: ['bcwvtknn', 'rty8296y'],
        description: 'Two works with matching titles',
      },
      {
        query: 'wa/hmm durham',
        ratings: ['euf49qkx', 'tpxy78kr', 'gu3z98y4', 'ad3rfubw'],
        description: 'Archive refno and a word from the title',
      },
      {
        query: 'wa/hmm benin',
        ratings: ['qfdvkegw', 'je5pm2gj', 'dppjjtqz'],
        description: 'Archive refno and a word from the description',
        knownFailure: true,
      },
      {
        query: 'eugenics society annual reports',
        ratings: ['k9w95csw', 'asqf8kzb', 'n499pzsr'],
        description: 'Matches archives without providing refnos',
      },
      {
        query: 'الكشف',
        ratings: ['ymnmz59p'],
        description: 'Matches stemmed arabic text',
      },
      {
        query: 'معرفت',
        ratings: ['a9w79fzj'],
        description: 'Matches stemmed arabic text',
      }
    ],
    metric: {
      recall: {
        relevant_rating_threshold: 3,
        k: 100,
      },
    },
  },
  {
    id: 'alternative-spellings',
    label: 'Alternative spellings',
    description:
      'Ensure that the query returns results for search terms which are misspelled or differently transliterated.',
    eval: equalTo1,
    cases: [
      { query: 'at-tib', ratings: ['qmm9mauk'] },
      { query: 'Aṭ-ṭib', ratings: ['qmm9mauk'] },
      { query: 'nuğūm', ratings: ['jtbenqbq'] },
      { query: 'nujum', ratings: ['jtbenqbq'] },
      { query: 'arbeiten', ratings: ['xn7yyrqf'], knownFailure: true },
      { query: 'travaillons', ratings: ['jb823ud6'] },
      { query: 'conosceva', ratings: ['va2vy7wb'] },
      { query: 'sharh', ratings: ['frd5y363'] },
      {
        query: 'arkaprakāśa',
        ratings: ['qqh7ydr3', 'qb7eggtk', 'jvw4bdrz', 'jh46tazh'],
      },
    ],
    metric: {
      recall: {
        relevant_rating_threshold: 3,
        k: 100,
      },
    },
  },
  {
    id: 'false-positives',
    label: 'False positives',
    description:
      "Ensure that the query doesn't return results which we know are irrelevant. Due to fuzzy matching on alternative spellings, we need to ensure we aren't too fuzzy.",
    eval: equalTo0,
    searchTemplateAugmentation: filterCaseRatings,
    cases: [
      {
        query: 'Deptford',
        ratings: ['pb4rbujd', 'g2awspp9'],
        description: "Shouldn't match 'dartford' or 'hertford'",
      },
      {
        query: 'Sahagún',
        ratings: ['neumfv84', 'dzhxzxcr'],
        description: "Shouldn't match 'gahagan'",
      },
      {
        query: 'posters',
        ratings: ['z85jd9f4', 'qpkfxsst'],
        description: "Shouldn't match 'porter'",
        knownFailure: true,
      },
      {
        query: 'gout',
        ratings: ['t67v2y55'],
        description: "Shouldn't match 'out'",
      },
      {
        query: 'L0062541',
        ratings: ['wsqmrqfj'],
        description: "Shouldn't match 'L0032741' in the title",
        knownFailure: true,
      },
      {
        query: 'Maori',
        ratings: ['h464szg9', 'y48zg6af', 'uf2ds6qs'],
        description: "shouldn't match 'mary' or 'amoris' or 'maris'",
        knownFailure: true,
      },
      {
        query: 'monsters',
        ratings: ['uyueynsp', 'd592f8ff'],
        description: "Should not match 'Monastery' or 'Ministers'",
        knownFailure: true,
      },
      {
        query: 'Maclise',
        ratings: ['kft2kzec'],
        description: "Should not match 'machine'",
        knownFailure: true,
      },
    ],
    metric: {
      recall: {
        relevant_rating_threshold: 3,
        k: 100,
      },
    },
  },
]

export default tests
