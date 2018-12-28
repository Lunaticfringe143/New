#!groovy

def mergePullRequest() {
    step([$class         : 'GhprbPullRequestMerge', allowMergeWithoutTriggerPhrase: false, deleteOnMerge: true,
          disallowOwnCode: false, failOnNonMerge: true, mergeComment: 'Merged', onlyAdminsMerge: false])
}

try {

    node("ccone-slave") {


        def mavenHome
        def utProjects = ["tm-common", "org-management" , "user-management", "smm"]
        stage('Preparation') {
            checkout scm
            mavenHome = tool(name: 'maven-3.5.0', type: 'maven');
        }
        withEnv([
                'MAVEN_HOME=' + mavenHome,
                "PATH=${mavenHome}/bin:${env.PATH}"
        ]) {
            stage('Build') {
                for (project in utProjects) {
                    dir(project) {
                        sh "'${mavenHome}/bin/mvn' clean test"
                        //sh "'${mavenHome}/bin/mvn' clean "
                    }
                }
            }
        }
        stage('Merge') {
            currentBuild.result = "SUCCESS"
            if (ghprbCommentBody.startsWith("TM_MERGE")) {
                node("ccone-slave") {
                    checkout scm
                    mergePullRequest()
                }
            }
        }

    }

}
catch (error) {
    currentBuild.result = "FAILURE"
    throw error
}
