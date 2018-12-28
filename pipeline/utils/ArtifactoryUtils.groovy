#!groovy

import groovy.json.JsonSlurper
// Build parameters which must be configured in the job
// and then passed down to this script as env variables:
//
//    ARTIFACTORY_AUTH
//    ARTIFACTORY_MASTER_ID
//    ARTIFACTORY_LOCAL_ID


//def Constants = load('pipeline/utils/Constants.groovy')

def publishArtifact(buildName, buildNum, uploadSpec) {
    def server = Artifactory.server env.ARTIFACTORY_LOCAL_ID

    def buildInfo = Artifactory.newBuildInfo()
    buildInfo.setName buildName
    buildInfo.setNumber buildNum

    server.upload spec: uploadSpec, buildInfo: buildInfo
    server.publishBuildInfo buildInfo
}

def downloadArtifact(repo, searchSpec) {
    def server = Artifactory.server env.ARTIFACTORY_MASTER_ID
    def artifactNameToFetch = getArtifactNameMatchingQuery(searchSpec)

    def downloadSpec = payloadForArtifactDownload(repo, artifactNameToFetch)
    server.download spec: downloadSpec

    return artifactNameToFetch
}

def promoteBuild(repo, buildName, buildNum, buildStatus) {
    def server = Artifactory.server env.ARTIFACTORY_MASTER_ID
    def promotionConfig = payloadForBuildPromotion(repo, buildName, buildNum, buildStatus)
    server.promote promotionConfig
}

/**
 Currently sort and limit functionalities are not available in the artifactory download spec
 So we need to invoke the artifactory API directly to get unique build number matching the query
 **/
def getArtifactNameMatchingQuery(searchQuery) {
    httpRequest authentication: env.ARTIFACTORY_AUTH,
            httpMode: 'POST', outputFile: 'artifactoryResponse.out',
            requestBody: searchQuery, url: Constants.ARTIFACTORY_AQL_URL
    sh "cat artifactoryResponse.out"
    def response = readFile 'artifactoryResponse.out'
    def jsonSlurper = new JsonSlurper()
    def json = jsonSlurper.parseText(response)
    return json.results[0].name
}

def payloadForArtifactDownload(repo, artifactName) {
    return """{
        "files":[{
            "aql":{
                "items.find":{
                    "repo":{"\$eq" : "${repo}"},
                    "name":{"\$eq" : "${artifactName}"}
                }
            },
            "flat":"true",
            "target":"./"
        }]
    }"""
}

def payloadForBuildPromotion(repo, buildName, buildNum, buildStatus) {
    def promotionConfig = [
            'buildName'  : buildName,
            'buildNumber': buildNum,
            'targetRepo' : repo,
            'status'     : buildStatus,
    ]
    return promotionConfig
}

def downloadArtifactByBuildNum(repo, buildName, buildNum) {
    def searchSpec = searchQueryByBuildNum(repo, buildName, buildNum)
    downloadArtifact(repo, searchSpec)
}

def downloadArtifactByBuildStatus(repo, buildName, buildStatus) {
    def searchSpec = searchQueryByBuildStatus(repo, buildName, buildStatus)
    downloadArtifact(repo, searchSpec)
}

def searchQueryByBuildStatus(repo, buildName, buildStatus) {
    return """items.find({ \
        "repo" : {"\$eq" : "$repo"}, \
        "artifact.module.build.name" : {"\$eq" : "$buildName"}, \
        "artifact.module.build.promotion.status" : {"\$eq" : "$buildStatus"} \
    }).sort({"\$desc": ["modified"]}).limit(1)"""
}

def searchQueryByBuildNum(repo, buildName, buildNum) {
    return """items.find({ \
        "repo" : {"\$eq" : "$repo"}, \
        "artifact.module.build.name" : {"\$eq" : "$buildName"}, \
        "artifact.module.build.number" : {"\$eq" : "$buildNum"} \
    })"""
}

/**
 CJP TM related code
 **/

def getArtifactoryBuildName(packageName) {
    return "cjp-tm-${packageName}_publishRelease"
}


def publishTMAppPackage(appBase, buildNum) {
    def buildName = getArtifactoryBuildName(appBase)

    def jarName = appBase + "-1.0.jar"
    def artifactoryJarName = appBase+"-"+ buildNum+".jar"
    sh """
#!/bin/bash

mvn clean install -Dmaven.test.skip=true

mv target/$jarName $artifactoryJarName
    """


    def uploadSpec = payloadForTMAppPackageUpload("cjp-tenant-managment-release", appBase)
    publishArtifact(buildName, buildNum, uploadSpec)
}

def publishTMMigrationAppPackage(appBase, buildNum) {
    def buildName = getArtifactoryBuildName(appBase)

    def jarName = appBase + ".jar"
    def artifactoryJarName = appBase+"-"+ buildNum+".jar"
    sh """
#!/bin/bash
mv target/$jarName $artifactoryJarName
    """


    def uploadSpec = payloadForTMAppPackageUpload("cjp-tenant-managment-release", appBase)
    publishArtifact(buildName, buildNum, uploadSpec)
}

def payloadForTMAppPackageUpload(repo, packageName) {
    return """{
        "files": [{
            "pattern": "$packageName*.jar",
            "target": "$repo/com/cisco/cjp/tenantmanagment/${packageName}/",
            "recursive": "false"
        }]
    }"""
}

return this;
