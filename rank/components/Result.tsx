import { FC } from 'react'
import { passEmoji } from '../utils'

type Props = {
  pass: boolean
}
const Result: FC<Props> = ({ pass }) => {
  const color = pass ? 'green' : 'red'
  return (
    <div
      className={`rounded-full bg-${color}-500 inline-flex items-center justify-center w-6 h-6 text-white`}
    >
      {passEmoji(pass)}
    </div>
  )
}

export default Result
