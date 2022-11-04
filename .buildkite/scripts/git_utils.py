from commands import git


def get_changed_paths(*args, globs=None):
    """
    Returns a set of changed paths in a given commit range.

    :param args: Arguments to pass to ``git diff``.
    :param globs: List of file globs to include in changed paths.
    """
    if globs:
        args = list(args) + ["--", *globs]
    diff_output = git("diff", "--name-only", *args)

    return {line.strip() for line in diff_output.splitlines()}


def remote_default_branch():
    """Inspect refs to discover default branch @ remote origin."""
    return git("symbolic-ref", "refs/remotes/origin/HEAD").split("/")[-1]
