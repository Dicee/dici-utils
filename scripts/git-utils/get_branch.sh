#!/bin/bash
set -e
set -o pipefail
git branch | grep \* | cut -d ' ' -f2
exit