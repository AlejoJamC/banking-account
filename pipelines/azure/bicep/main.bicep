// Parameters
param location string = 'westeurope'
param environment string = 'dev'
param dbAdminPassword string // Key Vault read

// modules
module acr 'modules/container-registry.bicep' = { }
module db 'modules/postgresql.bicep' = { }
module containerApp 'modules/container-app.bicep' = { }
module keyVault 'modules/key-vault.bicep' = { }

// Outputs
output containerAppUrl string = containerApp.outputs.fqdn
