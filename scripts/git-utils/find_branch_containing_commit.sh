#!/bin/bash
set -e
set -o pipefail
git branch --contains $1 | grep \* | cut -d ' ' -f2
exit