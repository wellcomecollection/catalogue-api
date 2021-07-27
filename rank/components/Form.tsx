import { Children, FC, HTMLProps } from 'react'

type FormProps = HTMLProps<HTMLFormElement>
type LabelProps = HTMLProps<HTMLLabelElement>
type SelectProps = HTMLProps<HTMLSelectElement>
type ButtonProps = HTMLProps<HTMLButtonElement>
type InputProps = HTMLProps<HTMLInputElement>

const Form: FC<FormProps> = ({ children, ...formProps }) => {
  return (
    <form className="pt-2 pb-2 border-b border-gray-500" {...formProps}>
      {children}
    </form>
  )
}

const Label: FC<LabelProps> = ({ children, ...labelProps }) => {
  return (
    <label className="inline-block font-bold flex-1" {...labelProps}>
      {children}
    </label>
  )
}

const Select: FC<SelectProps> = ({ children, ...selectProps }) => {
  return (
    <select className="block" {...selectProps}>
      {children}
    </select>
  )
}

const TextInput: FC<InputProps> = ({ children, ...inputProps }) => {
  return (
    <input
      type="text"
      {...inputProps}
      className="px-3 py-1 border-2 border-pink-500"
    />
  )
}

const Button: FC<ButtonProps> = ({ children }) => {
  return (
    <button className="bg-pink-500 flex items-center justify-center px-3 py-1 text-white">
      {children}
    </button>
  )
}

export default Form
export { Label, Select, Button, TextInput }
