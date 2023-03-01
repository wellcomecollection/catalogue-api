import { equalTo0, equalTo1 } from './services/pass'

import { Test } from './types/test'
import imageTests from '../data/tests/images.json'
import workTests from '../data/tests/works.json'

const evals = {
  equalTo0: equalTo0,
  equalTo1: equalTo1
}

const tests = {
  images: imageTests.map((test) => ({
    ...test,
    eval: evals[test.eval]
  })) as Test[],
  works: workTests.map((test) => ({
    ...test,
    eval: evals[test.eval]
  })) as Test[]
}

export default tests
