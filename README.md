# banking-account

## Assignment:
Write some code in Java to simulate a simple bank account. It should be possible to transfer and withdraw money from an account. It is possible to pay with either debit card or credit card. If a transfer/withdraw is done with a credit card, 1% of the amount is charged extra. Use design patterns where applicable and write some test cases as well.

## Requirement / Validations:
* A negative balance is not possible
* Account should contain at least some user details, card details and current balance
* One rest endpoint to see current available balance in all accounts
* One rest endpoint to withdraw money
* One rest endpoint to transfer money
* One credit card or debit card is linked with one account
* It should be able to audit transfers or withdrawals
* Front end part is not required
* Feel free to make some assumptions if needed & mention them in the code assignment

### Nice to have:
* Deploy this service somewhere (AWS/Azure) or deploy locally

## Solution:
## Technology Stack:
* Java 21
* Spring Boot
* Maven
* Docker
* PostgreSQL
* H2 Database
* LocalStack (AWS Emulation)
* Junit 5 & testcontainers
* OpenTelemetry

## Design Patterns Used:

## Local Setup:
1. Install Docker and Docker Compose
2. Clone the repository
3. Navigate to the project directory
4. Run `docker-compose up --build` to start the application and PostgreSQL database
5. The application will be accessible at `http://localhost:8080`
6. Use Postman or any API client to interact with the REST endpoints
7. To run tests, execute `mvn test` in the project directory
8. Access H2 console at `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:mem:testdb`, username `sa`, and password `password`
10. Access OpenTelemetry Collector at `http://localhost:4317` for tracing information
11. Access LocalStack at `http://localhost:4566` for AWS services emulation
12. Access PostgreSQL database at `localhost:5432` with username `myuser` and password `mypassword`

## REST Endpoints:
* `GET /api/accounts` - Get all accounts with current balance
* `POST /api/accounts/withdraw` - Withdraw money from an account
* `POST /api/accounts/transfer` - Transfer money between accounts
* `GET /api/audit` - Get all audit logs for transfers and withdrawals
* `GET /actuator/health` - Check application health status
* `GET /actuator/info` - Get application info

