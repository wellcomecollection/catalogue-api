import { FC, useState } from 'react'
import type { estypes } from '@elastic/elasticsearch'

type Props = { hit: estypes.SearchHit<Record<string, any>> }
const Hit: FC<Props> = ({ hit }) => {
  const [showExplanation, setShowExplanation] = useState(false)
  const [showMatches, setShowMatches] = useState(false)
  const title =
    hit._source.source?.canonicalWork.data.title ?? hit._source.data?.title
  return (
    <div className="text-black">
      <h2 className="text-2xl font-semibold leading-tight">{title}</h2>
      <div className="text-sm font-semibold">{hit._id}</div>
      <div className="text-sm text-gray-600 pt-0.5">
        <div onClick={() => setShowExplanation(!showExplanation)}>
          <span className="font-semibold">Score: </span>
          {hit._score}
        </div>
        <div onClick={() => setShowMatches(!showMatches)}>
          <span className="font-semibold">Matched: </span>
          {hit.matched_queries.join(', ')}
        </div>

        {showMatches && (
          <div>
            {Object.entries(hit.highlight).map(([key, highlight]) => {
              return (
                <div key={key} className="leading-tight pb-1">
                  <h3 className="font-bold">{key}</h3>
                  {highlight.map((text) => (
                    <div
                      key={key}
                      dangerouslySetInnerHTML={{
                        __html: text,
                      }}
                    />
                  ))}
                </div>
              )
            })}
          </div>
        )}
        {showExplanation && (
          <pre className="text-xs">
            {JSON.stringify(hit._explanation, null, 2)}
          </pre>
        )}
      </div>
    </div>
  )
}

export default Hit
