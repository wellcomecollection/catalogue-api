import { FC } from 'react'

type Props = {
  pass: boolean
}

function resultColor(pass: boolean): string {
  return pass ? 'green-500' : 'red-500'
}

function resultEmoji(pass: boolean): string {
  switch (pass) {
    case true:
      return '✔'
    case false:
      return '✘'
    default:
      return '?'
  }
}

const Result: FC<Props> = ({ pass }) => {
  return (
    <div
      className={`font-mono bg-${resultColor(
        pass
      )} inline-flex items-center justify-center w-6 h-6 text-white`}
    >
      {resultEmoji(pass)}
    </div>
  )
}

export default Result
export { resultEmoji, resultColor }
