#!/bin/bash -e
pushd `dirname $0` > /dev/null
SCRIPT_PATH=`pwd`
popd > /dev/null

pushd $SCRIPT_PATH/..

npm install
CI=true bower install --allow-root
