#!/bin/bash

set -e

if [[ $# != '2' ]]; then
  echo "ERROR: expected run_with_settings.sh [command] [config_key] but had $# parameters"
  exit 1
fi

command=$1
config_key=$2

config=$(cat environment.json | jq ".\"${config_key}\"" -c | sed -e s/login/$USER/g)
settings="$config" npm run release "$command"

