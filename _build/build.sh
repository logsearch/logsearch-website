#!/bin/bash -ex
pushd `dirname $0` > /dev/null
SCRIPT_PATH=`pwd`
popd > /dev/null

pushd $SCRIPT_PATH/..

npm install
bower install
grunt