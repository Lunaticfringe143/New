#!groovy

node("ccone-slave") {
    stage("Invoke Validate pipeline") {

        checkout scm
        def tmServices = ["org-management" ,"user-management"]
        def tmMigrationServices = ["tenant-migration"]
        foldersChanged = []

        def GitUtils = load('pipeline/utils/GitUtils.groovy')
        def commitHash = GitUtils.getCommitHash()

        def changeLogSets = currentBuild.changeSets
        for(changeSet in changeLogSets) {
            for(entry in changeSet.items) {
                echo "${entry.commitId} by ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}"
                def files = new ArrayList(entry.affectedFiles)
                for(file in files) {
                    echo "  ${file.editType.name} ${file.path}"
                    def tokens = file.path.split('/')
                    def parentFolder = tokens[0]
                    foldersChanged.add(parentFolder)
                }
            }
        }

        for(folder in foldersChanged.toSet()){
            if(tmServices.contains(folder)){
                echo "Service changed: $folder"
                triggerServiceValidation(commitHash, folder)
            } else if (tmMigrationServices.contains(folder)) {
                echo "Migration Service changed: $folder"
                triggerMigrationValidation(commitHash, folder)
            }
        }
    }
}

def triggerServiceValidation(commitHash, service) {
    echo "Starting service validation pipeline : $service"
    build job: 'tm-service-validation',
            parameters: [
                    string(name: 'BUILD_COMMIT_HASH', value: commitHash),
                    string(name: 'APP_BASE', value: service)
            ], propagate: false, wait: false
}

def triggerMigrationValidation(commitHash, service) {
    echo "Starting service validation pipeline : $service"
    build job: 'tm-migration-validation',
            parameters: [
                    string(name: 'BUILD_COMMIT_HASH', value: commitHash),
                    string(name: 'APP_BASE', value: service)
            ], propagate: false, wait: false
}
