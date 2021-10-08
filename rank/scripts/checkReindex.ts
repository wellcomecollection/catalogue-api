import { info, success } from './utils'

import cliProgress from 'cli-progress'
import { getRankClient } from '../services/elasticsearch'

async function go() {
  const client = getRankClient()
  const reindexTasks = await client.cat.tasks({
    actions: 'indices:data/write/reindex',
  })

  const task_id = reindexTasks.body.split(' ')[1]
  var taskResponse = await client.tasks.get({ task_id })
  if (taskResponse.body.completed) {
    success('Reindex complete!')
  } else {
    info(`Still working on ${taskResponse.body.task.description}\n`)

    const progress = new cliProgress.SingleBar(
      {},
      cliProgress.Presets.shades_classic
    )
    progress.start(
      taskResponse.body.task.status.total,
      taskResponse.body.task.status.created
    )

    const timer = setInterval(async function () {
      taskResponse = await client.tasks.get({ task_id })
      progress.increment()
      progress.update(taskResponse.body.task.status.created)
      if (taskResponse.body.task.status.created >= progress.getTotal()) {
        clearInterval(timer)
        progress.stop()
        info('')
        success('Reindex complete!')
      }
    }, 10000) // refresh progress every 10s
  }
}

go()
