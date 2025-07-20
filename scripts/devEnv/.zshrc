export ZSH=$HOME/.oh-my-zsh

ZSH_THEME="myzsh"

plugins=(git)

source $ZSH/oh-my-zsh.sh
# https://github.com/ohmyzsh/ohmyzsh/issues/5327
# https://github.com/nvm-sh/nvm/issues/539#issuecomment-245791291
# source /usr/local/opt/nvm/nvm.sh
#export NVM_DIR="$HOME/.nvm"
#[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh" --no-use 
#
#alias node='unalias node ; unalias npm ; nvm use default ; node $@'
#alias npm='unalias node ; unalias npm ; nvm use default ; npm $@'

#source ~/.rvm/scripts/rvm

COMPLETION_WAITING_DOTS="true"
HIST_STAMPS="dd.mm.yyyy"

# Additional path 
export PATH=$HOME/bin:/usr/local/bin:$PATH
export PATH=$HOME/repos/personal/dici-utils/scripts:$PATH
export PATH=$HOME/repos/personal/dici-utils/scripts/devEnv:$PATH
export PATH=$HOME/repos/personal/dici-utils/scripts/python/ws:$PATH
export PATH=$HOME/repos/personal/dici-utils/scripts/python/cwgrep:$PATH
export PATH=$HOME/Documents/eclipse-workspace/algorithmicProblems/bin/:$PATH

export EDITOR='vim'

