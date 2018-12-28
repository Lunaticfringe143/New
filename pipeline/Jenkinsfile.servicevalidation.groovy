#!groovy


def setDisplayName(buildNum, appBase) {
    currentBuild.displayName = '#' + buildNum + ' ' + appBase
}

node("ccone-slave") {

    def mavenHome
    final String buildNum = (currentBuild.number + BASE_BUILD_NUM.toInteger()).toString()
    setDisplayName(buildNum, APP_BASE)

    final String testsDir = APP_BASE + '-test'

    def artifactoryUtils
    def Constants
    def GitUtils
    def deploymentUtils

    final String APP_NAME = APP_BASE
    final String CF_ORG = 'tenant-management'
    final String CF_SPACE = 'app'
    final String DEPLOYMENT = 'dev'
    final String APP_NAME_PREFIX = 'green-'

    try {


        node("ccone-slave") {
            stage('Preparation') {
                checkout scm
                Constants = load('pipeline/utils/Constants.groovy')
                artifactoryUtils = load('pipeline/utils/ArtifactoryUtils.groovy')
                GitUtils = load('pipeline/utils/GitUtils.groovy')
                deploymentUtils = load('pipeline/utils/DeploymentUtils.groovy')
                GitUtils.checkoutCommitHash()
                mavenHome = tool(name: 'maven-3.5.0', type: 'maven');

            }
            withEnv([
                    'MAVEN_HOME=' + mavenHome,
                    "PATH=${mavenHome}/bin:${env.PATH}"
            ]) {
                stage("UT") {
                    dir(APP_BASE) {
                        sh "'${mavenHome}/bin/mvn' clean test"
                    }
                }

                /*//app deploy
                stage("dev-deploy"){
                    dir(APP_BASE){
                        sh "'${mavenHome}/bin/mvn' clean install"
                    }
                    deploymentUtils.appPackageDeploy(DEPLOYMENT, CF_ORG, CF_SPACE,APP_BASE, APP_NAME, APP_NAME_PREFIX)
                }

                //execute ft
                stage("FT") {
                    dir(testsDir) {
                        sh "'${mavenHome}/bin/mvn' test -Dspring.profiles.active=dev"
                    }
                }*/

                stage("Publish Release") {
                    dir(APP_BASE) {
                        artifactoryUtils.publishTMAppPackage(APP_BASE, buildNum)
                    }
                }

                stage("validate") {
                    //Waiting for replication to complete can remove once we have validation steps
                    sleep(60)
                }

                stage("Promote Build") {
                    final String buildName = artifactoryUtils.getArtifactoryBuildName(APP_BASE)
                    artifactoryUtils.promoteBuild(Constants.TM_ARTIFACTORY_REPO, buildName, buildNum, Constants.RELEASE_STATUS)
                }
                currentBuild.result = "SUCCESS"
            }
        }
    }
    catch (error) {
        currentBuild.result = "FAILURE"
        throw error
    }

    //To generate the cucumber report
    
    // step([$class: 'CucumberReportPublisher',
    //        jenkinsBasePath: '',
    //        fileIncludePattern: '',
    //        fileExcludePattern: '',
    //        jsonReportDirectory: '',
    //        ignoreFailedTests: true,
    //        missingFails: false,
    //        pendingFails: false,
    //        skippedFails: false,
    //        undefinedFails: false,
    //        parallelTesting: false])

}

