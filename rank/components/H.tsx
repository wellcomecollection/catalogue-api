import { FC } from 'react'

const H2: FC = ({ children }) => {
  return (
    <h2 className="text-xl mt-1 mb-2 border-t border-gray-200">{children}</h2>
  )
}

export { H2 }
