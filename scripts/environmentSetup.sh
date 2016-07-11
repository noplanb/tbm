#!/bin/bash

# Fix the CircleCI path
function getAndroidSDK(){
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

  DEPS="$ANDROID_HOME/installed-dependencies"

  if [ ! -e $DEPS ]; then
    cp -r /usr/local/android-sdk-linux $ANDROID_HOME &&
    echo y | android update sdk -u -a -t android-23 &&
    echo y | android update sdk -u -a -t platform-tools &&
    echo y | android update sdk -u -a -t build-tools-23.0.3 &&
    #echo y | android update sdk -u -a -t sys-img-x86-android-19 &&
    #echo y | android update sdk -u -a -t addon-google_apis-google-18 &&
    echo no | android create avd -n testAVD -f -t android-23 --abi default/x86 &&
    touch $DEPS
  fi
}
