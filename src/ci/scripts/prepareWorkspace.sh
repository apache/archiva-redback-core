#!/usr/bin/env bash
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
#  Author: Martin Stockhammer <martin_s@apache.org>
#  Date:   2017-05-24
#
#  Clears the workspace, if the build number is higher
#  for automatic workspace cleanup on the build slaves
#
#  To request a workspace cleanup increase the number for CIBUILD
#  in the file ../env/build-info.txt and

## 
BUILDINFO_DIR=${WORKSPACE}/ci
BUILDINFO_FILE=${BUILDINFO_DIR}/buildinfo.sh
REQ_BUILDINFO_FILE=$(dirname $0)/../env/build-info.txt
git checkout ${REQ_BUILDINFO_FILE}
if [ -f ${REQ_BUILDINFO_FILE} ]; then
  . ${REQ_BUILDINFO_FILE}
else
  CIBUILD=0
fi
REQUESTED_BUILD=${CIBUILD}
CIBUILD=0
if [ -f ${BUILDINFO_FILE} ]; then
  . ${BUILDINFO_FILE}
fi
if [ ${CIBUILD} -lt ${REQUESTED_BUILD} ]; then
  echo "Clearing workspace"
  rm -rf ${WORKSPACE}/*
  mkdir -p ${BUILDINFO_DIR}
  echo "CIBUILD=${REQUESTED_BUILD}" >${BUILDINFO_FILE}
fi
