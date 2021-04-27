ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile

STACK_ROOT 	= .

PROJECT_ID = catalogue_api

SBT_APPS = search
SBT_NO_DOCKER_APPS =

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES = display

PYTHON_APPS = update_api_docs
LAMBDAS 	=

$(val $(call stack_setup))
