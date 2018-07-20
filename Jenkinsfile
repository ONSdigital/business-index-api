#!groovy
@Library('jenkins-pipeline-shared') _

pipeline {
    environment {
        RELEASE_TYPE = "PATCH"

        GITLAB_CREDS = "bi-gitlab-id"

        ORGANIZATION = "ons"
        TEAM = "bi"
        MODULE_NAME = "business-index-api"
	    
    	STAGE = "NONE"
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
            environment{ STAGE = "Checkout" }
            steps {
                deleteDir()
                checkout scm
                stash name: 'app'
                sh "sbt version"
                script {
                    version = '1.0.' + env.BUILD_NUMBER
                    currentBuild.displayName = version
                }
            }
        }

        stage('Build') {
            agent any
            environment{ STAGE = "Build" }
            steps{
                sh "sbt clean compile"
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }

        stage('Test'){
            agent any
            environment{ STAGE = "Test"  }
            steps {
                colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL} from branch ${env.BRANCH_NAME}")
                sh 'sbt coverage test coverageReport'
            }
            post {
                success {
                    junit '**/target/test-reports/*.xml
                    step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/*/coverage-report/cobertura.xml'])
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }

        stage('Static Analysis') {
            agent any
            environment{ STAGE = "Static Analysis" }
            steps {
                parallel (
                    "Scalastyle" : {
                        colourText("info","Running scalastyle analysis")
                        sh "sbt scalastyle"
                    },
                    "Scapegoat" : {
                        colourText("info","Running scapegoat analysis")
                        sh "sbt scapegoat"
                    }
                )
            }
            post {
                success {
                    step([$class: 'CheckStylePublisher', pattern: '**/target/code-quality/style/*scalastyle*.xml'])
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }

        stage('Package'){
            agent any
            when{ expression{ isBranch("master") }}
            environment{ 
                STAGE = "Package" 
                DEPLOY_TO = "dev"    
            }
            steps {
                dir('gitlab') {
                    git(url: "$GITLAB_URL/BusinessIndex/${MODULE_NAME}-api.git", credentialsId: GITLAB_CREDS, branch: "master")
                }
                sh 'sbt universal:packageBin'
                sh "cp target/universal/${ORGANIZATION}-${MODULE_NAME}-*.zip ${DEPLOY_TO}-${ORGANIZATION}-${MODULE_NAME}.zip"
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }

        stage('Deploy CF'){
            agent any
            when{ expression{ isBranch("master") }}
            environment{ 
                STAGE = "Deploy CF"
                DEPLOY_TO = "dev" 
            }
            steps {
                milestone(1)
                lock('BI API Deployment Initiated') {
                    colourText("info", "${env.DEPLOY_TO}-${CH_TABLE}-${MODULE_NAME} deployment in progress")
                    deploy()
                    colourText("success", "${env.DEPLOY_TO}-${CH_TABLE}-${MODULE_NAME} Deployed.")
                }
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
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
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${env.STAGE}"
        }
        failure {
            colourText("warn","Process failed at: ${env.STAGE}")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${env.STAGE}"
        }
    }
}

def isBranch(String branchName){
    return env.BRANCH_NAME.toString().equals(branchName)
}

def deploy () {
    CF_SPACE = "${env.DEPLOY_NAME}".capitalize()
    CF_ORG = "${TEAM}".toUpperCase()
    echo "Deploying app to ${env.DEPLOY_NAME}"
    deployToCloudFoundry("${TEAM}-${env.DEPLOY_NAME}-cf", "${CF_ORG}", "${CF_SPACE}", "${env.DEPLOY_NAME}-${MODULE_NAME}", "${env.DEPLOY_NAME}-${ORGANIZATION}-${MODULE_NAME}.zip", "conf/${env.DEPLOY_NAME}/manifest.yml")
}
