import { FC, HTMLProps } from 'react'

type Props = {}
type FormProps = HTMLProps<HTMLFormElement>
type LabelProps = HTMLProps<HTMLLabelElement>
type SelectProps = HTMLProps<HTMLSelectElement>
type ButtonProps = HTMLProps<HTMLButtonElement>

const Form: FC<Props & FormProps> = ({ children, ...formProps }) => {
  return (
    <form className="pt-2 pb-2 border-b border-gray-500" {...formProps}>
      {children}
    </form>
  )
}

const FormBar: FC<Props> = ({ children }) => {
  return <div className="flex space-x-5">{children}</div>
}

const Label: FC<Props & LabelProps> = ({ children, ...labelProps }) => {
  return (
    <label className="inline-block font-bold flex-1" {...labelProps}>
      {children}
    </label>
  )
}

const Select: FC<Props & SelectProps> = ({ children, ...selectProps }) => {
  return (
    <select className="block" {...selectProps}>
      {children}
    </select>
  )
}

const Submit: FC = () => {
  return (
    <button
      type="submit"
      className="rounded-full bg-pink-500 flex items-center justify-center w-12 h-12"
    >
      <span className="sr-only">Submit</span>
      🔍
    </button>
  )
}

export default Form
export { FormBar, Label, Select, Submit }
