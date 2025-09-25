set -e

current_branch=$(git symbolic-ref --short HEAD)
tracked_remote=$(git for-each-ref --format='%(upstream:short)' "refs/heads/$current_branch")

if [ -z "$tracked_remote" ]; then
  echo "No remote branch is tracked for '$current_branch'."
  exit 1
else
  echo $tracked_remote
  exit 0
fi

