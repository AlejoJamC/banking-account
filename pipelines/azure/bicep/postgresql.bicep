resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2023-03-01-preview' = {
  name: 'banking-db-${environment}'
  location: location
  sku: {
    name: 'Standard_B1ms'  // Dev: Burstable, Prod: GP
    tier: 'Burstable'
  }
  properties: {
    version: '16'
    administratorLogin: 'bankadmin'
    administratorLoginPassword: dbAdminPassword
    storage: { storageSizeGB: 32 }
    backup: { backupRetentionDays: 7 }
  }
}
