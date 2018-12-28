#!groovy

class Constants {

    static final String TM_ARTIFACTORY_REPO = 'cjp-tenant-managment-release'
    //static final String ARTIFACTORY_AQL_URL = 'http://engci-maven-master.cisco.com/artifactory/api/search/aql'
    static final String ARTIFACTORY_AQL_URL = 'http://ccone-maven.thunderhead.io/artifactory/libs-release'

    static final String RELEASE_STATUS = 'Release'
    static final String DEPLOYED_STATUS = 'Deployed'

    static final String GITHUB_STATUS_URL = 'https://sqbu-github.cisco.com/api/v3/repos'

}

return new Constants();