#!/bin/bash
set -e

if [[ $(git diff --shortstat 2> /dev/null | tail -n1) != "" ]]; then
    echo "changes"
else
    echo "no change"
fi

exit
