import { equalTo0, equalTo1 } from './pass'

import { Test } from '../../types'
import { filterCaseRatings } from './queryAugmentation'

const tests: Test[] = [
  {
    label: 'Precision',
    description: 'TBD',
    pass: equalTo1,
    cases: [
      { query: 'crick dna sketch', ratings: ['gzv2hhgy'] },
      { query: 'gzv2hhgy', ratings: ['gzv2hhgy'] },
      { query: 'kmebmktz', ratings: ['gzv2hhgy'] }, // search for work ID and get associated images
      { query: 'L0033046', ratings: ['gzv2hhgy'] },
    ],
    metric: {
      precision: {
        relevant_rating_threshold: 3,
        k: 1,
      },
    },
  },
  {
    label: 'Recall',
    description: 'TBD',
    pass: equalTo1,
    cases: [
      { query: 'horse battle', ratings: ['ud35y7c8'] },
      {
        query: 'everest chest',
        ratings: [
          'bt9yvss2',
          'erth8sur',
          'fddgu7pe',
          'qbvq42t6',
          'u6ejpuxu',
          'xskq2fsc',
          'prrq5ajp',
          'zw53jx3j',
        ],
      },
      {
        query: 'Frederic Cayley Robinson',
        ratings: [
          'avvynvp3',
          'b286u5hw',
          'dey48vd8',
          'g6n5e53n',
          'gcr92r4d',
          'gh3y9p3y',
          'vmm6hvuk',
          'z894cnj8',
          'cfgh5xqh',
          'fgszwax3',
          'hc4jc2ax',
          'hfyyg6y4',
          'jz4bkatc',
          'khw4yqzx',
          'npgefkju',
          'q3sw6v4p',
          'z7huxjwf',
          'dkj7jswg',
          'gve6469u',
          'kyw8ufwn',
          'r49f89rq',
          'sptagjhw',
          't6jb62an',
          'tq4qjedt',
          'xkt8av46',
          'yh6evjnu',
          'dvg8e7h5',
          'knc95egk',
          'th8c2wan',
        ],
      },
    ],
    metric: {
      recall: {
        relevant_rating_threshold: 3,
        k: 30,
      },
    },
  },
  {
    label: 'Languages',
    description: 'TBD',
    pass: equalTo1,
    cases: [
      { query: 'arbeiten', ratings: ['sr4kxmk3', 'utbtee43'] },
      { query: 'conosco', ratings: ['nnh3nh47'] },
      { query: 'allons', ratings: ['dqnapkdx'] },
    ],
    metric: {
      recall: {
        relevant_rating_threshold: 3,
        k: 100,
      },
    },
  },
  {
    label: 'False positives',
    description:
      "Due to fuzzy matching on alternative spellings, we need to ensure we aren't too fuzzy.",
    pass: equalTo0,
    searchTemplateAugmentation: filterCaseRatings,
    cases: [
      {
        query: 'monsters',
        ratings: ['n7r5s65w'],
        description: "shouldn't match 'Monastery'",
      },
      {
        query: 'maori',
        ratings: ['fgksh2cc', 'tqk8vfq2'],
        description: "shouldn't match 'mary' or 'mori'",
      },
      {
        query: 'Deptford',
        ratings: ['c5zv5zqh', 'eq4pvgmu'],
        description: "shouldn't match 'dartford' or 'hertford'",
      },
      {
        query: 'Maclise',
        ratings: ['sxbgjm4y'],
        description: "shouldn't match 'machine'",
      },
      {
        query: 'machine',
        ratings: ['uyym87vg', 'hpjx2g82'],
        description: "shouldn't match 'martin' or 'vaccine'",
      },
      {
        query: 'asylum',
        ratings: ['bzsscsgq', 'abshch65'],
        description: "shouldn't match 'slums', 'assumed'",
      },
      {
        query: 'vestiges',
        ratings: ['zeqtq26h', 'ym9awu5d'],
        description: "shouldn't match 'vestitus', 'festival'",
      },
      {
        query: 'revolutions',
        ratings: ['rbznrd3v', 'nwgs7wtg'],
        description: "shouldn't match  'resolutive' or 'Renoult'",
      },
      {
        query: 'macaronic',
        ratings: [
          'c2c9phsz',
          'p9usexyk',
          'jfd42bs9',
          'uqtw6x7a',
          'cfwn8qpp',
          'emfaj7a7',
        ],
      }, // shouldn't match Macao, Aaron, matron, Martinez, Macon, Arago,
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
