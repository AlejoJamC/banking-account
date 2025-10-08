resource keyVault 'Microsoft.KeyVault/vaults@2023-02-01' = {
  name: 'banking-kv-${uniqueString(resourceGroup().id)}'
  location: location
  properties: {
    sku: { family: 'A', name: 'standard' }
    tenantId: subscription().tenantId
    accessPolicies: [
      // Grant Container App access
    ]
  }
}
