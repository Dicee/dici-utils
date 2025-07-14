#!/bin/bash
set -e
git rev-list --count origin/`sh get_tracked_remote_branch.sh $1`...HEAD
exit