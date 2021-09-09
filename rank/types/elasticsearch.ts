/* eslint-disable camelcase */
import { Pass } from '../data/tests/pass'

type PrecisionMetricResponse = {
    precision: {
        relevant_docs_retrieved: number
        docs_retrieved: number
    }
}

type RecallMetricResponse = {
    recall: {
        relevant_docs_retrieved: number
        docs_retrieved: number
    }
}

type MetricResponse = PrecisionMetricResponse | RecallMetricResponse

export type RankDetail = {
    metric_score: number
    unrated_docs: unknown[]
    hits: unknown[]
    metric_details: MetricResponse
}

export type RankEvalResponse<DetailKeys extends string = string> = {
    metric_score: number
    details: Record<DetailKeys, RankDetail>
}

type PrecisionMetric = {
    precision: {
        relevant_rating_threshold: number
        k: number
    }
}

type RecallMetric = {
    recall: {
        relevant_rating_threshold: number
        k: number
    }
}

export type Metric = PrecisionMetric | RecallMetric

type RankEvalRequestRating<Rating = 0 | 1 | 2 | 3> = {
    _index: string
    _id: string
    rating: Rating
}

export type RankEvalRequestRequest<Params> = {
    id: string
    template_id: string
    params: Params
    ratings: RankEvalRequestRating[]
}

type RankEvalRequestTemplate = {
    id: string
    templates: {
        inline: unknown
    }
}

export type RankEvalRequest<TemplateParams> = {
    templates: RankEvalRequestTemplate[]
    requests: RankEvalRequestRequest<TemplateParams>[]
    metric: unknown
}

export type RankEvalResponsWithMeta = RankEvalResponse & {
    pass: Pass
    // We've set the passes here as I didn't want to augment the original response.
    // There's probably a better way to do this, but it'll do for now.
    passes: Record<string, Pass>
    queryId: string
    index: string
    query: {
        method: string
        path: string
        body: string
    }
}

export type Hit<DocType = Record<string, any>> = {
    _id: string
    _score: string
    _source: DocType
    _explanation: Record<string, unknown>
    highlight: Record<string, string[]>
    matched_queries: string[]
}

export class SearchResponse {
    took: number
    hits: {
        total: {
            value: number
            relation: 'eq' | 'gte'
        }
        max_score: number
        hits: Hit[]
    }
}
