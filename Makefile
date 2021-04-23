ACCOUNT_ID = 760097843905

include makefiles/functions.Makefile
include makefiles/formatting.Makefile

include api.Makefile
include stacks/Makefile
include snapshots/Makefile

lambda-test: snapshot_scheduler-test

lambda-publish: snapshot_scheduler-publish
