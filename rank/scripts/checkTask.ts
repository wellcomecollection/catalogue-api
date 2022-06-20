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
      .tasks()
      .then((res) => res.body)
      // the task response comes back as a list of whitespace delimited strings like:
      //   action  task_id  parent_task_id  type  start_time  timestamp  running_time  ip  node
      // from which we extract the task_id
      // see https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-tasks.html
      .then((body) => body.split('\n'))
      .then((lines) =>
        lines.map((line) => {
          const [action, task_id] = line.split(' ')
          return { action, task_id }
        })
      )
      .then((tasks) =>
        tasks.filter((task) => task.task_id && task.task_id !== '')
      )

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
      progress.increment()
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
