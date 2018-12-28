#!groovy
try {
    node("ccone-slave") {
        def mavenHome
        def ltProjects = ["tm-services-loadtest"]
        stage('Preparation') {
            checkout scm
            mavenHome = tool(name: 'maven-3.5.0', type: 'maven');
        }
        withEnv([
                'MAVEN_HOME=' + mavenHome,
                "PATH=${mavenHome}/bin:${env.PATH}"
        ]) {
            stage('Build') {
                for (project in ltProjects) {
                    dir(project) {
                        sh "'${mavenHome}/bin/mvn' clean gatling:test"
                    }
                }
            }
        }
        stage('Report') {
            currentBuild.result = "SUCCESS"
            for (project in ltProjects) {
                    dir(project) {
                        gatlingArchive()
                    }
            }
        }
    }
}
catch (error) {
    currentBuild.result = "FAILURE"
    throw error
}