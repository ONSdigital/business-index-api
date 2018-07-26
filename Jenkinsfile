#!groovy
@Library('jenkins-pipeline-shared') _

pipeline {
    environment {
        RELEASE_TYPE = "PATCH"

        GITLAB_CREDS = "bi-gitlab-id"

        ORGANIZATION = "ons"
        TEAM = "bi"
        MODULE_NAME = "business-index-api"

        SBT_HOME = tool name: 'sbt.13.13', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'
        PATH = "${env.SBT_HOME}/bin:${env.PATH}"
    }
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    agent any
    stages {
        stage('Checkout') {
            agent any
            steps {
                deleteDir()
                checkout scm
                stash name: 'app'
                sh 'sbt version'
            }
        }

        stage('Build') {
            agent any
            steps{
                sh 'sbt clean compile'
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE_NAME} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE_NAME} failed!")
                }
            }
        }

        stage('Test'){
            agent any
            steps {
                colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL} from branch ${env.BRANCH_NAME}")
                sh 'sbt coverage test coverageReport coverageOff'
            }
            post {
                success {
                    junit '**/target/test-reports/*.xml'
                    step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/*/coverage-report/cobertura.xml'])
                    colourText("info","Stage: ${env.STAGE_NAME} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE_NAME} failed!")
                }
            }
        }

        stage('Static Analysis') {
            parallel {
                stage('Scalastyle') {
                    agent any
                    steps {
                        colourText("info", "Running scalastyle analysis")
                        sh 'sbt scalastyle'
                    }
                }
                post {
                    success {
                        step([$class: 'CheckStylePublisher', pattern: '**/target/code-quality/style/*scalastyle*.xml'])
                        colourText("info","Stage: ${env.STAGE_NAME} successful!")
                    }
                    failure {
                        colourText("warn","Stage: ${env.STAGE_NAME} failed!")
                    }
                }
                stage('Scapegoat') {
                    agent any
                    steps {
                        colourText("info", "Running scapegoat analysis")
                        sh 'sbt scapegoat'
                    }
                }
                post {
                    success {
                        step([$class: 'CheckStylePublisher', pattern: '**/target/*/scapegoat-report/scapegoat-scalastyle.xml'])
                        colourText("info","Stage: ${env.STAGE_NAME} successful!")
                    }
                    failure {
                        colourText("warn","Stage: ${env.STAGE_NAME} failed!")
                    }
                }
            }

        }

        stage('Package'){
            agent any
            when{ expression{ isBranch("master") }}
            environment{ 
                STAGE = "Package"
            }
            steps {
                sh 'sbt universal:packageBin'
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE_NAME} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE_NAME} failed!")
                }
            }
        }

        stage('Deploy - DEV'){
            agent any
            when{ expression{ isBranch("master") }}
            environment{ 
                STAGE = "Deploy - DEV"
                DEPLOY_TO = "dev"
                CF_ROUTE = "${env.DEPLOY_TO}-${MODULE_NAME}"
            }
            steps {
                milestone(1)
                dir('gitlab') {
                    git(url: "$GITLAB_URL/BusinessIndex/${MODULE_NAME}.git", credentialsId: GITLAB_CREDS, branch: "master")
                }
                lock("${env.CF_ROUTE}") {
                    colourText("info", "${env.DEPLOY_TO}-${MODULE_NAME} deployment in progress")
                    deploy()
                    colourText("success", "${env.DEPLOY_TO}-${MODULE_NAME} deployed")
                }
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE_NAME} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE_NAME} failed!")
                }
            }
        }
    }
    post {
        success {
            colourText("success", "All stages complete. Build was successful.")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST"
        }
        unstable {
            colourText("warn", "Something went wrong, build finished with result ${currentResult}. This may be caused by failed tests, code violation or in some cases unexpected interrupt.")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${env.STAGE_NAME}"
        }
        failure {
            colourText("warn","Process failed at: ${env.STAGE}")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${env.STAGE_NAME}"
        }
    }
}

def isBranch(String branchName){
    return env.BRANCH_NAME.toString().equals(branchName)
}

def deploy () {
    CF_SPACE = "${env.DEPLOY_TO}".capitalize()
    CF_ORG = "${TEAM}".toUpperCase()
    echo "Deploying app to ${env.DEPLOY_TO}"
    deployToCloudFoundry("${TEAM}-${env.DEPLOY_TO}-cf", "${CF_ORG}", "${CF_SPACE}", "${env.CF_ROUTE}", "${env.DEPLOY_TO}-${ORGANIZATION}-${MODULE_NAME}.zip", "gitlab/${env.DEPLOY_TO}/manifest.yml")
}
