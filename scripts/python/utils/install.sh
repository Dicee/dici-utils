#!/bin/bash

set +x
set -e

exit_with_message() {
  echo 'ERROR:' $1 >&2
  exit 1
}

portable_md5() {
  # Linux
  if [[ $OSTYPE == "linux-gnu" ]]; then
    md5sum $1 | cut -d \  -f1
  # Mac OSX
  elif [[ $OSTYPE == "darwin"* ]]; then
    md5 -q $1
  else
    exit_with_message "OS not supported: $OSTYPE"
  fi
}

last_modified_time() {
  if [[ ! -f $1 ]]; then
    echo 0
  else
    if uname | grep -q "Darwin"; then
      mod_time_fmt="-f %m"
    else
      mod_time_fmt="-c %Y"
    fi
    stat $mod_time_fmt $1
  fi
}

is_installed() {
  command -v $1
}

too_lazy() {
  if [[ ! $(is_installed $1) ]]; then
    if [[ $# == 2 ]]; then
        suffix=" This doc might be helpful: $2."
    fi
    exit_with_message "I don't know how to install $1 for you, please come back when you set it up. $suffix"
  else
    echo "$1 is already installed"
  fi
}

export_path() {
    installation_dir=$1
    config=$2

    if [[ -e ${config} ]]; then
        echo '' >> ${config}
        echo '# Added by cwgrep tool' >> ${config}
        echo "export PATH=\$PATH:${installation_dir}" >> ${config}
        echo '' >> ${config}

        if [[ ${config} == *"zsh"* ]]; then
            zsh -c "source ${config}"
        else
            sh -c "source ${config}"
        fi

        if [[ $? -eq 0 ]]; then
            exit_with_message "Failed to add ${config} to your path and source from ${config}. Please resolve the issue manually and re-run this script."
        fi

        echo "Added installation directory to your path in ${config}"
    fi
}

update_user_path() {
    installation_dir=$1

    if [[ "$PATH" != *"$installation_dir"* ]]; then
        export_path ${installation_dir} ~/.zshrc
        export_path ${installation_dir} ~/.bashrc
        export_path ${installation_dir} ~/.bash_profile
    fi
}

update() {
  installation_dir=$1
  update_marker=$2
  update_frequency=$3

  last_updated=`last_modified_time $update_marker`
  now=`date +%s`

  if [[ $(($now - $last_updated)) -gt $update_frequency ]]; then
    echo "Updating..."

    too_lazy git
    cwd=`pwd`

    cd $installation_dir
    gpull
    echo '' > $update_marker
    cd $cwd

    echo 'Update successful!'
  fi
}

install_python_module() {
  python3 -m pip install $1
}

install_graphviz() {
  # Linux
  if [[ $OSTYPE == "linux-gnu" ]]; then
    # Ubuntu
    if [[ $(is_installed apt-get) ]]; then
      sudo apt-get install graphviz
    # RHEL54
    elif [[ $(is_installed yum) ]]; then
      sudo yum install graphviz
    else
      exit_with_message "What's your package manager? Help me install graphviz, please"
    fi
  # Mac OSX
  elif [[ $OSTYPE == "darwin"* ]]; then
    too_lazy brew
    HOMEBREW_NO_AUTO_UPDATE=1 brew install graphviz
  else
    exit_with_message "OS not supported: $OSTYPE"
  fi
}

install_aws() {
  too_lazy pip3
  python3 -m pip install awscli --upgrade
  exit_with_message "Please update your PATH to finalize AWS CLIT installation. For Mac, add something like that to your .zshrc/.bashrc: export PATH=/Users/$USER/.local/lib/aws/bin:\$PATH"
}

install_aws2() {
  if [[ -d aws ]]
  then
      exit_with_message "Folder 'aws' already exists, probably from a previously failed run of the installation. Please check that it can safely be deleted and restart the installation."
  fi

  # Linux
  if [[ $OSTYPE == "linux-gnu" ]]; then
    curl "https://d1vvhvl2y92vvt.cloudfront.net/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install
  # Mac OSX
  elif [[ $OSTYPE == "darwin"* ]]; then
    curl "https://d1vvhvl2y92vvt.cloudfront.net/awscli-exe-macos.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install
  else
    exit_with_message "OS not supported: $OSTYPE. Please follow instructions to install the AWS CLI: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2-windows.html#cliv2-windows-install"
  fi
}

install_dependency() {
  if [[ $1 == 'arghandler' ]]; then
    install_python_module $1
  elif [[ $1 == 'sty' ]]; then
    install_python_module $1
  elif [[ $1 == 'pytimeparse' ]]; then
    install_python_module $1
  elif [[ $1 == 'frozenlist' ]]; then
    install_python_module $1
  elif [[ $1 == 'boto3' ]]; then
    install_python_module $1
  elif [[ $1 == 'requests' ]]; then
    install_python_module $1
  elif [[ $1 == 'urllib3' ]]; then
    install_python_module $1
    python3 -m pip install --upgrade urllib3==1.24.3 # https://github.com/streamlink/streamlink/issues/2448
  elif [[ $1 == 'graphviz' ]]; then
    install_graphviz
  # keep all commands below
  elif [[ $(is_installed $1) ]]; then
    echo "Already installed: $1"
  elif [[ $1 == 'git' ]]; then
    too_lazy $1
  elif [[ $1 == 'python3' ]]; then
    too_lazy $1
  elif [[ $1 == 'pip3' ]]; then
    too_lazy $1
  elif [[ $1 == 'aws' ]]; then
    install_aws
  elif [[ $1 == 'aws2' ]]; then
    install_aws2
  else
    exit_with_message "What's $1? I haven't learnt to install that"
  fi
}

install_dependencies() {
  requirements=$1
  install_marker=$2

  req_md5=`portable_md5 $requirements`
  if [[ ! -f $install_marker || $(< $install_marker) != "$req_md5" ]]; then
    echo "Seems like it's the first time you executed this tool since the last time requirements were updated. Running setup script..."
    while read line; do
      install_dependency $line
    done <$requirements

    echo $req_md5 > $install_marker
    echo "Successfully installed all dependencies"
  fi
}
