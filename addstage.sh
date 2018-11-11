#!/bin/bash

MP="$1"
PL=$(echo "$MP" | sed -e 's#/# #g' -e 's/^\.//g')

echo "$PL"
STR="project"

if [ -f "${MP}/pom.xml" ]; then
  for DIR in $PL; do
    STR="${STR}.parent"
  done
else
  exit 0
fi

echo "$MP $PL"
grep -q '<properties>' $MP/pom.xml
if [ $? -eq 0 ]; then
  sed -i -e 's#<properties>#<properties>\n    <site.staging.base>${'${STR}'.basedir}</site.staging.base>#g' $MP/pom.xml
else
  grep -q '<packaging>' $MP/pom.xml
  if [ $? -eq 0 ]; then
    sed -i -e 's#\(.*</packaging>.*\)#\1\n  <properties>\n    <site.staging.base>${'${STR}'.basedir}</site.staging.base>\n  </properties>#g' $MP/pom.xml
  else
    sed -i -e 's#\(.*</name>.*\)#\1\n  <properties>\n    <site.staging.base>${'${STR}'.basedir}</site.staging.base>\n  </properties>#g' $MP/pom.xml
  fi
fi
