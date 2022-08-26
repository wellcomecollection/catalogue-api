import { info, success } from './utils'

import cliProgress from 'cli-progress'
import { getRankClient } from '../services/elasticsearch'
import prompts from 'prompts'
import yargs from 'yargs'

async function go() {
  const client = getRankClient()
  let task_id
  const args = yargs(process.argv)
    .options({
      task: {
        type: 'string',
        description: 'The task to check',
      },
    })
    .exitProcess(false)
    .parseSync()

  if (args.task) {
    task_id = args.task
  } else {
    const tasks = await client.cat
      .tasks({ format: 'json', h: 'action,task_id' })
      .then((res) => res.body)

    task_id = await prompts({
      type: 'select',
      name: 'task_id',
      message: 'Select a task',
      choices: tasks.map((task) => ({
        title: task.task_id,
        description: task.action,
        value: task.task_id,
      })),
    }).then(({ task_id }) => task_id)
  }

  var taskResponse = await client.tasks.get({ task_id })
  if (taskResponse.body.completed) {
    success('Task complete!')
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
      const createdOrUpdatedCount =
        taskResponse.body.task.status.created +
        taskResponse.body.task.status.updated
      progress.update(createdOrUpdatedCount)
      if (createdOrUpdatedCount >= progress.getTotal()) {
        clearInterval(timer)
        progress.stop()
        info('')
        success('Task complete!')
      }
    }, 1000) // refresh progress every 1s
  }
}

go()
