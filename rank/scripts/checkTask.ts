import { error } from 'console'
import { getRankClient } from '../services/elasticsearch'

async function go() {
  const [task_id] = process.argv.slice(2)
  if (!task_id) {
    error('Please specify a task ID')
  }

  const client = getRankClient()
  const taskResponse = await client.tasks.get({ task_id })
  console.info(taskResponse.body)
}

go()
