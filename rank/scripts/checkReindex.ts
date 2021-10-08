import { info, success } from './utils'

import { getRankClient } from '../services/elasticsearch'

const cliProgress = require('cli-progress')

async function go() {
  const client = getRankClient()
  const reindexTasks = await client.cat.tasks({
    actions: 'indices:data/write/reindex',
  })

  const task_id = reindexTasks.body.split(' ')[1]
  const taskResponse = await client.tasks.get({ task_id })
  if (taskResponse.body.completed) {
    success('Complete!')
  } else {
    info('Still working!\n')

    const progress = new cliProgress.SingleBar(
      {},
      cliProgress.Presets.shades_classic
    )
    progress.start(
      taskResponse.body.task.status.total,
      taskResponse.body.task.status.created
    )
    progress.stop()
    info('')
  }
}

go()
