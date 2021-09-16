// TODO: fix perf issues
// Jest's default timeout is 5s - too short for any of the problems underlying
// our performance weirdness to start manifesting. Bumping that limit to 30s
// allows the tests to run and opens the door to investigating the performance
// issues, but we shouldn't be deploying new queries with such a loose threshold.
jest.setTimeout(30000)
