/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Main build file for Jenkins Multibranch pipeline.
 *
 * The pipeline builds, runs the test and deploys to the archiva snapshot repository.
 *
 * Uses one stage for build and deploy to avoid running it multiple times.
 * The settings for deployment with the credentials must be provided by a MavenSettingsProvider.
 *
 * Only the war and zip artifacts are archived in the jenkins build archive.
 */
LABEL = 'ubuntu'
buildJdk = 'JDK 1.8 (latest)'
buildJdk11 = 'JDK 11 (latest)'

pipeline {
    agent {
        label "${LABEL}"
    }

    stages {
        stage('Builds') {
            parallel {

                stage('BuildAndDeploy-JDK8') {
                    steps {
                        timeout(120) {
                            mavenBuild(buildJdk,"clean deploy -U -fae",
                                       [artifactsPublisher(disabled: false),
                                        junitPublisher(disabled: false, ignoreAttachments: false),
                                        pipelineGraphPublisher(disabled: false)])
                        }
                    }
                    post {
                        failure {
                            notifyBuild("Failure in BuildAndDeploy Stage")
                        }
                    }
                }

                stage('JDK11') {
                    steps {
                        ws("${env.JOB_NAME}-JDK11") {
                            checkout scm
                            timeout(120) {
                                mavenBuild(buildJdk11,"clean install -U -fae",
                                           [junitPublisher(disabled: false, ignoreAttachments: false)])
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        unstable {
            notifyBuild("Unstable Build")
        }
        failure {
            notifyBuild("Error in redback build")
        }
        success {
            script {
                def previousResult = currentBuild.previousBuild?.result
                if (previousResult && !currentBuild.resultIsWorseOrEqualTo(previousResult)) {
                    notifyBuild("Fixed")
                }
            }
        }
    }
}

def mavenBuild(jdk, cmdline, options) {
    buildMvn = 'Maven 3.5.2'
    deploySettings = 'archiva-uid-jenkins'
    def mavenOpts = '-Xms1g -Xmx2g -Djava.awt.headless=true'

    withMaven(maven: buildMvn, jdk: "$jdk",
              publisherStrategy: 'EXPLICIT',
              mavenOpts: mavenOpts,
              mavenSettingsConfig: deploySettings,
              mavenLocalRepo: ".repository",
              options: options
    )
        {
            sh "mvn -V -B -T3 -e -Dmaven.test.failure.ignore=true $cmdline"
        }
}

// Send a notification about the build status
def notifyBuild(String buildStatus) {
    // default the value
    buildStatus = buildStatus ?: "UNKNOWN"

    def email = "notifications@archiva.apache.org"
    def summary = "${env.JOB_NAME}#${env.BUILD_NUMBER} - ${buildStatus} - ${currentBuild?.currentResult}"
    def detail = """<h4>Job: <a href='${env.JOB_URL}'>${env.JOB_NAME}</a> [#${env.BUILD_NUMBER}]</h4>
  <p><b>${buildStatus}</b></p>
  <table>
    <tr><td>Build</td><td><a href='${env.BUILD_URL}'>${env.BUILD_URL}</a></td><tr>
    <tr><td>Console</td><td><a href='${env.BUILD_URL}console'>${env.BUILD_URL}console</a></td><tr>
    <tr><td>Test Report</td><td><a href='${env.BUILD_URL}testReport/'>${env.BUILD_URL}testReport/</a></td><tr>
  </table>
  """

    emailext(
            to: email,
            subject: summary,
            body: detail,
            mimeType: 'text/html'
    )
}

// vim: et:ts=4:sw=4:ft=groovy
