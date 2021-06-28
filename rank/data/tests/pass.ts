import { RankDetail } from '../../types/elasticsearch'

type Pass = {
  score: number // should be a range between 0 => 1
  pass: boolean
}
type Eval = (score: RankDetail) => Pass

const equalTo1: Eval = (rankDetail: RankDetail) => {
  return {
    score: rankDetail.metric_score,
    pass: rankDetail.metric_score === 1,
  }
}

const equalTo0: Eval = (rankDetail: RankDetail) => {
  return {
    score: rankDetail.metric_score,
    pass: rankDetail.metric_score === 0,
  }
}

export { equalTo0, equalTo1 }
export type { Pass, Eval }
