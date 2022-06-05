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
buildJdk = 'jdk_1.8_latest'
buildJdk11 = 'jdk_11_latest'
mavenVersion = 'maven_3.8.5'

def defaultPublishers = [artifactsPublisher(disabled: false), junitPublisher(ignoreAttachments: false, disabled: false),
                         findbugsPublisher(disabled: true), openTasksPublisher(disabled: true),
                         dependenciesFingerprintPublisher(disabled: false), invokerPublisher(disabled: true),
                         pipelineGraphPublisher(disabled: false),mavenLinkerPublisher(disabled: false)]

pipeline {
    agent { label "${LABEL}" }
    // Build should also start, if parent has been built successfully
    triggers { 
        upstream(upstreamProjects: 'Archiva/archiva-projects/archiva-parent/master,Archiva/archiva-projects/archiva-components/master', threshold: hudson.model.Result.SUCCESS)
    }

    options {
        disableConcurrentBuilds()
        durabilityHint('PERFORMANCE_OPTIMIZED')
        buildDiscarder(logRotator(numToKeepStr: '7', artifactNumToKeepStr: '2'))
        timeout(time: 120, unit: 'MINUTES')
    }

    stages {
        stage( 'JDK8' ) {
            steps {
                script{
                    if (env.NONAPACHEORG_RUN != 'y' && env.BRANCH_NAME == 'master')
                    {
                        asfStandardBuild.mavenBuild( buildJdk, "clean deploy -U -fae -T3", mavenVersion,
                                                     defaultPublishers )
                    } else {
                        asfStandardBuild.mavenBuild( buildJdk, "clean install -U -fae -T3", mavenVersion,
                                                     defaultPublishers )
                    }
                }
            }
        }
        stage('JDK11') {
            steps {
                script {
                    asfStandardBuild.mavenBuild(buildJdk11,"clean install -U -fae -T3",mavenVersion,[])
                }
            }
        }

    }

    post {
        always {
            cleanWs deleteDirs: true, notFailBuild: true, patterns: [[pattern: '.repository', type: 'EXCLUDE']]
        }    
        unstable {
            script{
                asfStandardBuild.notifyBuild( "Unstable Build ")
            }
        }
        failure {
            script{
                asfStandardBuild.notifyBuild( "Error in redback core build ")
            }
        }
        success {
            script {
                def previousResult = currentBuild.previousBuild?.result
                if (previousResult && !currentBuild.resultIsWorseOrEqualTo( previousResult ) ) {
                    asfStandardBuild.notifyBuild( "Fixed" )
                }
            }
        }
    }
}
// vim: et:ts=4:sw=4:ft=groovy
