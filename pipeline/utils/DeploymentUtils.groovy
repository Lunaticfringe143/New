#!groovy

def appPackageDeploy(deployment, cfOrg, cfSpace, appBase, appName, appNamePrefix) {
    runDeployScript(deployment, cfOrg, cfSpace, appBase, appName, appNamePrefix)
}
def runDeployScript(deployment, cfOrg, cfSpace, appBase, appName, appNamePrefix) {
    withTMVaultCredentials(getDatacenterFromDeployment(deployment)) {
        sh """
            cd build
            ./tmdeploy.sh -d "$deployment" -o "$cfOrg" -s "$cfSpace" -b "$appBase" -a "$appName" \
                -x "$appNamePrefix" -r "$VAULT_TM_ROLE_ID" -t "$VAULT_TM_SECRET_ID"
        """
    }
}
def getDatacenterFromDeployment(deployment) {
    //TODO: Eventually need to make the property file match tenant domain so that this workaround is not required.
    String datacenter = deployment

    switch(deployment) {
        case 'dev':
            datacenter = 'devus1'
            break

        case 'load':
            datacenter = 'loadus1'
            break

        case 'produs1':
            datacenter = 'produs1'
            break

        case 'appstaging':
        case 'appstaging-pr':
        case 'appstaging-load':
            datacenter = 'appstaging'
            break

        default:
            datacenter = deployment
    }

    return datacenter
}

def withTMVaultCredentials(datacenter, Closure body) {
    static final String VAULT_APPSECRET_KEY_PREFIX = 'tenantmanagement.vault.appsecret.'
    static final String VAULT_APPROLE_KEY_PREFIX = 'tenantmanagement.vault.approle.'

    withCredentials([string(credentialsId: VAULT_APPSECRET_KEY_PREFIX + datacenter, variable: 'SECRET_ID'),string(credentialsId: VAULT_APPROLE_KEY_PREFIX + datacenter, variable: 'ROLE_ID')]) {
        withEnv(["VAULT_TM_ROLE_ID=$ROLE_ID", "VAULT_TM_SECRET_ID=$SECRET_ID"]) {
            body()
        }
    }
}

return this;
