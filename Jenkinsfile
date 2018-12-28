node {

  stage('SCM') {

    git 'https://github.com/Lunaticfringe143/New.git'

  }

      /*  stage('Compile and package'){

        def mvnHome = tool name: 'maven-3' , type: 'maven'

                sh "$(mvnHome)/bin/mvn/ package"

        }*/

        

  stage('SonarQube analysis') {

    withSonarQubeEnv('sonar-1') {
       def mvnHome = tool name: 'maven-3' , type: 'maven'

      // requires SonarQube Scanner for Maven 3.2+

      sh "${mvnHome}/bin/mvn/ sonar:sonar"

    }

  }
  
 }
