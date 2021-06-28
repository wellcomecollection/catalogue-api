export const envs = ['prod', 'stage', 'local'] as const
export type Env = typeof envs[number]

export function isEnv(v: any): v is Env {
  return envs.includes(v.toString())
}
