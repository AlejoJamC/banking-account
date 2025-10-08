resource containerApp 'Microsoft.App/containerApps@2023-05-01' = {
  name: 'banking-account-app'
  location: location
  properties: {
    configuration: {
      ingress: {
        external: true
        targetPort: 8080
      }
      secrets: [
        { name: 'db-password', value: dbPassword }
      ]
    }
    template: {
      containers: [
        {
          name: 'banking-account'
          image: '${acrName}.azurecr.io/banking-account:latest'
          env: [
            { name: 'SPRING_PROFILES_ACTIVE', value: 'prod' }
            { name: 'SPRING_DATASOURCE_URL', value: 'jdbc:postgresql://...' }
            { name: 'SPRING_DATASOURCE_PASSWORD', secretRef: 'db-password' }
          ]
          resources: {
            cpu: json('0.5')
            memory: '1Gi'
          }
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 3
      }
    }
  }
}
