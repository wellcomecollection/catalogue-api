export const envs = ['local', 'remote'] as const
export type Env = typeof envs[number]

export function isEnv(v: any): v is Env {
  return v && envs.includes(v.toString())
}
