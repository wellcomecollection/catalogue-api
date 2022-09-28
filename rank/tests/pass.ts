import { RankEvalRankEvalMetricDetail as MetricDetail } from '@elastic/elasticsearch/lib/api/types'

type Pass = {
  score: number // should be between 0-1
  pass: boolean
}

type Eval = (score: MetricDetail) => Pass

const equalTo1: Eval = (metricDetail: MetricDetail) => {
  return {
    score: metricDetail.metric_score,
    pass: metricDetail.metric_score === 1,
  }
}

const equalTo0: Eval = (metricDetail: MetricDetail) => {
  return {
    score: metricDetail.metric_score,
    pass: metricDetail.metric_score === 0,
  }
}

export { equalTo0, equalTo1 }
export type { Pass, Eval }
