This folder contains a small toolbox that I found useful to make me faster on some common command-line operations in daily development. It might also be useful to you!

## Features

- automatically installs most of its own dependencies and updates every two days
- auto-detect the workspace's root, the package folder you're in, as well as all downstream dependencies of a package that are also checked-in in the workspace
- parallel execution of built-in or arbitrary commands to all or a subset of checked-in packages
- parallel builds respecting build dependency order
- interactive rebase/push of multiple packages
- display the dependency graph of a checked-in package in your browser, filtered down to DA packages with visual cues to indicate check-in packages
- stdout/stdin log collection to be able to debug when a command failed for a given package

### Supported operations

```none
╭───courtino ~
╰➤  ws --help
usage: ws.py [-h] subcommand

positional arguments:
  subcommand
              create      Creates a new workspace and pulls selected packages in parallel
              clone       Clones multiple Brazil repositories in parallel
              remove      Removes a list of Brazil packages
              sync        Syncs the version set's metadata and pulls all checked packages in parallel
              pull        Pulls selected packages in parallel
              run         Executes an arbitrary command in parallel to all selected packages
              check       Lists the differences between the local HEAD and the associated remote branch for all packages with unpushed local commits
              status      Runs 'git status' on all packages with unstaged changes
              stash       Stashes unstaged changes in all packages with unstaged changes
              rebase      Detects all packages with unpushed local commits and interactively allows rebasing them one by one
              push        Detects all packages with unpushed local commits and interactively allows pushing them one by one
              relate      Relates specified commits in selected packages to each other (will show in code.amazon.com)
              graph       Generates the dependency graph of the current package and opens it in the default browser
              bbr         Runs a build target for building recursively and in parallel as much as possible while respecting the build order of all dependencies
              clean-logs  Cleans the logs older than the specified duration

optional arguments:
  -h, --help  show this help message and exit
```

### Installation

The tool will install its dependencies automatically except some basic dependencies that are expected to already
be available (e.g. bash, brew, python3, pip3 etc). The tool auto-updates every 2 days before executing the command 
requested by the user. 

```bash
git clone https://github.com/Dicee/dici-utils.git
if [[ ! $(command -v ws) ]]; then
  echo "Please add /wherever/you/put/it/dici-utils/scripts/ws to your path"
else 
  ws -h
fi
```

### Backlog

#### Feature
- given a CR id, find all commits corresponding to this CR and push them interactively
- add an option to filter the graph based on custom conditions
- try to make fresh builds faster by triggering package caching in parallel since it doesn't require to respect any particular build order
- allow pushing a subset of commits for each package interactively selected for pushing rather than all commits
- allow running an arbitrary command in parallel on all packages in topological order, and not just `brazil-build` sub-commands
- implement `ws pop -name .*`
- add an option to blacklist some packages from being built by `ws bbr` if we know they're already built

##### Bugs (those that are not features!) 
- fix `-b` option for `ws rebase` and `ws push`
- fix `ws status`. Right now it behaves like `git diff`, and doesn't show staged commits

##### Code improvement
- ideally, we should not depend on `kcurl` but rather use `requests-kerberos` package
- replace `exec_cmd` with `_execute_for_package` for all commands that run in a specific package
- better module structure for general utils
- minor: try to make imports shorter without introducing cyclic imports in `execution_utils`

## Dependencies

- bash (for Windows users, assuming the Ubuntu bash would work)
- brew (Mac OSX only)
- yum (RHEL54 only)
- apt-get (Ubuntu only)
- git
- python3
- pip3 (normally comes with python3)
- arghandler (Python module)
- sty (Python module)
- pytimeparse (Python module)
- graphviz
