ACCOUNT_ID = 760097843905

include makefiles/functions.Makefile
include makefiles/formatting.Makefile

include snapshots/Makefile

STACK_ROOT 	= .

PROJECT_ID = catalogue_api

SBT_APPS = search items
SBT_NO_DOCKER_APPS = requests

SBT_DOCKER_LIBRARIES    = stacks
SBT_NO_DOCKER_LIBRARIES = display  search_common

PYTHON_APPS = update_api_docs
LAMBDAS 	=

$(val $(call stack_setup))

lambda-test: snapshot_scheduler-test

lambda-publish: snapshot_scheduler-publish
