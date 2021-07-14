import { FC } from 'react'

const H2: FC = ({ children }) => {
  return (
    <h2 className="text-3xl mt-1 mb-4 border-l-4 pl-2 border-blue-300">
      {children}
    </h2>
  )
}

const H3: FC = ({ children }) => {
  return (
    <h2 className="text-lg mt-1 mb-2 border-l-4 pl-2 border-pink-300">
      {children}
    </h2>
  )
}

export { H2, H3 }
