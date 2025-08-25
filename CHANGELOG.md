# Changelog

All notable changes to the Iced-Latte project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive changelog documentation
- Improved database migration structure

### Changed
- Enhanced project documentation

### Deprecated

### Removed

### Fixed

### Security

## [0.0.1] - 2025-01-25

### Added
- **Core Features**
  - Product catalog with search and filtering
  - User authentication and authorization (JWT-based)
  - Shopping cart functionality
  - Order management system
  - Product reviews and ratings
  - Favorite products functionality
  - Payment integration with Stripe
  - File upload and management (AWS S3)
  - Email notifications (AWS SES)
  - Audit logging system

- **Security Features**
  - JWT token authentication
  - Login attempt tracking and account locking
  - Password validation and security
  - Role-based access control

- **Technical Infrastructure**
  - Spring Boot 3.5.5 application
  - PostgreSQL database with Liquibase migrations
  - Redis caching layer
  - Docker containerization
  - Prometheus monitoring
  - OpenAPI documentation
  - Comprehensive test coverage (JUnit 5, TestContainers)

- **Database Schema**
  - Product management tables
  - User authentication and profile tables
  - Shopping cart and order tables
  - Review and rating system
  - Audit logging tables
  - File metadata management

- **API Endpoints**
  - Product CRUD operations
  - User registration and authentication
  - Shopping cart management
  - Order processing
  - Review and rating system
  - Favorite products management
  - Payment processing

### Technical Details
- **Architecture**: Monolithic Spring Boot application
- **Java Version**: 21
- **Database**: PostgreSQL with Liquibase migrations
- **Security**: JWT tokens, Spring Security
- **Testing**: JUnit 5, TestContainers, Rest Assured
- **Documentation**: OpenAPI 3.0 specifications
- **Deployment**: Docker containers with GitHub Actions CI/CD

### Dependencies
- Spring Boot 3.5.5
- Spring Security
- Spring Data JPA
- PostgreSQL 42.7.7
- Liquibase 4.32.0
- JWT 0.12.6
- MapStruct 1.6.3
- Lombok 1.18.36
- Stripe Java SDK 28.4.0
- AWS SDK 2.32.25

[Unreleased]: https://github.com/Sunagatov/Iced-Latte/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/Sunagatov/Iced-Latte/releases/tag/v0.0.1