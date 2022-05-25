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
        query: 'DJmjW2cU',
        ratings: ['djmjw2cu'],
        description: 'Case insensitive IDs',
      },
      {
        query: '2013i',
        ratings: ['djmjw2cu'],
        description: 'Reference number as ID',
      },
      {
        query: '2599i',
        ratings: ['xxskepr5'],
        description: 'Reference number as ID',
      },
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
        knownFailure: true,
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
        query: '2013i 2599i',
        ratings: ['djmjw2cu', 'xxskepr5'],
        description: 'Multiple IDs',
      },
      {
        query: 'wa/hmm durham',
        ratings: ['euf49qkx', 'tpxy78kr', 'gu3z98y4', 'ad3rfubw'],
        description: 'Archive refno and a word from the title',
        knownFailure: true,
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
        knownFailure: true,
      },
      {
        query: 'لكشف',
        ratings: ['ymnmz59p'],
        description: 'Matches stemmed arabic text',
        knownFailure: true,
      },
      {
        query: 'الكشف',
        ratings: ['ymnmz59p'],
        description: 'Matches arabic text',
        knownFailure: true,
      },
      {
        query: 'معرفت',
        ratings: ['a9w79fzj'],
        description: 'Matches arabic text',
        knownFailure: true,
      },
      {
        query: 'عرف',
        ratings: ['a9w79fzj'],
        description: 'Matches stemmed arabic text',
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
      { query: 'nujum', ratings: ['jtbenqbq'], knownFailure: true },
      { query: 'arbeiten', ratings: ['xn7yyrqf'] },
      // we know that something strange has happened to the french and italian
      // stemming tests, but stemming _is_ still happening
      // see https://github.com/wellcomecollection/catalogue-api/issues/469
      {
        query: 'savoire',
        ratings: ['tbuwy9bk'],
        description: 'french stemming',
      },
      {
        query: 'conosceva',
        ratings: ['j3w6u4t2', 'mt8bj5zk', 'vhf56vvz'],
        description: 'italian stemming',
      },
      { query: 'sharh', ratings: ['frd5y363'] },
      {
        query: 'arkaprakāśa',
        ratings: ['qqh7ydr3', 'qb7eggtk', 'jvw4bdrz', 'jh46tazh'],
      },
    ],
    metric: {
      recall: {
        relevant_rating_threshold: 3,
        k: 1000,
      },
    },
  },
]

export default tests
