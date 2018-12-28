#!groovy


def setDisplayName(buildNum, appBase) {
    currentBuild.displayName = '#' + buildNum + ' ' + appBase
}

node("ccone-slave") {

    def mavenHome
    final String buildNum = (currentBuild.number + BASE_BUILD_NUM.toInteger()).toString()
    setDisplayName(buildNum, APP_BASE)

    def artifactoryUtils
    def Constants
    def GitUtils

    def migratioinFiles = ["tm-data-extractor", "tm-data-loader"]
    try {

            stage('Preparation') {
                checkout scm
                Constants = load('pipeline/utils/Constants.groovy')
                artifactoryUtils = load('pipeline/utils/ArtifactoryUtils.groovy')
                GitUtils = load('pipeline/utils/GitUtils.groovy')
                GitUtils.checkoutCommitHash()
                mavenHome = tool(name: 'maven-3.5.0', type: 'maven');


            }
            withEnv([
                    'MAVEN_HOME=' + mavenHome,
                    "PATH=${mavenHome}/bin:${env.PATH}"
            ]) {
                stage("UT") {
                    dir(APP_BASE) {
                        sh """
                            echo "ENV :: START"
                            env | sort
                            echo "ENV :: END"
                            ls -la ~
                            echo "CAT SETTINGS.xml"
                            cat ~/.m2/settings.xml
                            echo "STARTING MVN CLEAN TEST"
                                '${mavenHome}/bin/mvn' clean test
                        """
                    }
                }
                stage("Build Migration Packages") {
                    dir(APP_BASE){

                        sh """
#!/bin/bash

mvn clean install -Dmaven.test.skip=true
"""
                    }
                }

                stage("Publish Release") {

                    dir(APP_BASE) {

                        for (migrationPart in migratioinFiles) {
                            dir(migrationPart){
                                artifactoryUtils.publishTMMigrationAppPackage(migrationPart, buildNum)
                            }
                        }
                    }

                }
                stage("validate") {

                    //Waiting for replication to complete can remove once we have validation steps
                    sleep(60)

                }
                stage("Promote Build") {
                    for (migrationPart in migratioinFiles) {
                        final String buildName = artifactoryUtils.getArtifactoryBuildName(migrationPart)
                        artifactoryUtils.promoteBuild(Constants.TM_ARTIFACTORY_REPO, buildName, buildNum, Constants.RELEASE_STATUS)
                    }
                }
                currentBuild.result = "SUCCESS"
            }
    }
    catch (error) {
        currentBuild.result = "FAILURE"
        throw error
    }

}

