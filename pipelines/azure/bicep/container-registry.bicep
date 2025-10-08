resource acr 'Microsoft.ContainerRegistry/registries@2023-01-01-preview' = {
  name: 'bankingaccountacr${uniqueString(resourceGroup().id)}'
  location: location
  sku: { name: 'Basic' }
  properties: {
    adminUserEnabled: true
  }
}
