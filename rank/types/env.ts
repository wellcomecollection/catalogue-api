const envs = ['prod', 'stage', 'local'] as const
type Env = typeof envs[number]

function isEnv(v: any): v is Env {
  return envs.includes(v.toString())
}

export { envs, isEnv }
export type { Env }
