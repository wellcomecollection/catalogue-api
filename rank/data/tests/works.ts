import { Test } from '../../types/test'
import { equalTo0, equalTo1 } from './pass'
import { filterCaseRatings } from './queryAugmentation'

const tests: Test[] = [
  {
    id: 'precision',
    label: 'Precision',
    description: 'TBD',
    pass: equalTo1,
    cases: [
      {
        query: 'Cassils Time lapse',
        ratings: ['ftqy78zj'],
        description: 'Contributor and title in the same query',
      },
      {
        query: 'stim',
        ratings: ['e8qxq5mv'],
        description:
          'exact match on title with lowercasing and punctuation stripping',
      },
      {
        query: 'bulloch history of bacteriology',
        ratings: ['rkcux48q'],
        description: 'Contributor and title in the same query',
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
        description: 'miro ID matching',
      },
      {
        query: 'kmebmktz',
        ratings: ['kmebmktz'],
        description: 'work ID matching',
      },
      {
        query: 'gzv2hhgy',
        ratings: ['kmebmktz'],
        description: 'image ID matching',
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
    description: 'TBD',
    pass: equalTo1,
    cases: [
      {
        query: 'Atherosclerosis : an introduction to atherosclerosis',
        ratings: ['bcwvtknn', 'rty8296y'],
        description: "two works with matching titles"
      },
      {
        query: 'wa/hmm durham',
        ratings: ['euf49qkx', 'tpxy78kr', 'gu3z98y4', 'ad3rfubw'],
        description: 'archive refno and a word from the title',
      },
      {
        query: 'wa/hmm benin',
        ratings: ['qfdvkegw', 'je5pm2gj', 'dppjjtqz'],
        description: 'archive refno and a word from the description',
      },
      {
        query: 'eugenics society annual reports',
        ratings: ['k9w95csw', 'asqf8kzb', 'n499pzsr'],
        description: "matches archives without providing refnos"
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
    description: 'TBD',
    pass: equalTo1,
    cases: [
      { query: 'at-tib', ratings: ['qmm9mauk'] },
      { query: 'Aṭ-ṭib', ratings: ['qmm9mauk'] },
      { query: 'nuğūm', ratings: ['jtbenqbq'] },
      { query: 'nujum', ratings: ['jtbenqbq'] },
      { query: 'arbeiten', ratings: ['xn7yyrqf'] },
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
      "Due to fuzzy matching on alternative spellings, we need to ensure we aren't too fuzzy.",
    pass: equalTo0,
    searchTemplateAugmentation: filterCaseRatings,
    cases: [
      {
        query: 'Deptford',
        ratings: ['pb4rbujd', 'g2awspp9'],
        description: "shouldn't match 'dartford' or 'hertford'",
      },
      {
        query: 'Sahagún',
        ratings: ['neumfv84', 'dzhxzxcr'],
        description: "shouldn't match 'gahagan'",
      },
      {
        query: 'posters',
        ratings: ['z85jd9f4', 'qpkfxsst'],
        description: "shouldn't match 'porter'",
      },
      {
        query: 'gout',
        ratings: ['t67v2y55'],
        description: "shouldn't match 'out'",
      },
      {
        query: 'L0062541',
        ratings: ['wsqmrqfj'],
        description: "shouldn't match 'L0032741' in the title",
      },
      {
        query: 'Maori',
        ratings: ['h464szg9', 'y48zg6af', 'uf2ds6qs'],
        description: "shouldn't match 'mary' or 'amoris' or 'maris'",
      },
      {
        query: 'monsters',
        ratings: ['uyueynsp', 'd592f8ff'],
        description: "should not match 'Monastery' or 'Ministers'",
      },
      {
        query: 'Maclise',
        ratings: ['kft2kzec'],
        description: "should not match 'machine'",
      },
    ],
    metric: {
      recall: {
        relevant_rating_threshold: 3,
        k: 10,
      },
    },
  },
]

export default tests
