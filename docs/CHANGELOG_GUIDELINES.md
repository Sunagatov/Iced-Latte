# Changelog Guidelines

This document outlines the standards and practices for maintaining changelogs in the Iced-Latte project.

## Overview

We maintain two types of changelogs:
1. **Project Changelog** (`CHANGELOG.md`) - High-level feature and release tracking
2. **Database Changelogs** (`src/main/resources/db/changelog/`) - Database schema evolution

## Project Changelog Standards

### Format
- Follow [Keep a Changelog](https://keepachangelog.com/) format
- Use [Semantic Versioning](https://semver.org/)
- Maintain reverse chronological order (newest first)

### Categories
- **Added**: New features, endpoints, dependencies
- **Changed**: Modifications to existing functionality
- **Deprecated**: Features marked for future removal
- **Removed**: Deleted features or dependencies
- **Fixed**: Bug fixes and patches
- **Security**: Security-related changes

### Writing Guidelines
- Use clear, concise language
- Write in present tense
- Include relevant issue/PR numbers
- Mark breaking changes prominently
- Group related changes together

## Database Changelog Standards

### File Organization
```
db/changelog/
├── changelog-master.yaml
└── version-X.Y/
    ├── changelog-master-version-X.Y.yaml
    └── DD.MM.YYYY.partN.description.sql
```

### Naming Convention
- **Format**: `DD.MM.YYYY.partN.description.sql`
- **Example**: `25.01.2025.part1.create-user-table.sql`
- Use descriptive names for easy identification

### Migration File Structure
```sql
--liquibase formatted sql

--changeset author:changeset-id
--comment: Brief description
--author: author-name
--date: YYYY-MM-DD

-- Detailed comments about the migration
-- Include impact and rollback strategy

[SQL statements]

--rollback [rollback statements]
```

### Documentation Requirements
- Include comprehensive comments
- Explain the purpose and impact
- Document rollback strategy
- Add preconditions when necessary
- Use meaningful changeset IDs

### Grouping Strategy
Organize migrations by functional areas:
- Core product system
- User management & security
- Shopping cart system
- Order management
- Review & rating system
- Audit & compliance

## Best Practices

### Project Changelog
1. **Update with every release**
2. **Keep unreleased section current**
3. **Be specific about changes**
4. **Include migration notes for database changes**
5. **Reference documentation for complex features**

### Database Changelog
1. **One logical change per file**
2. **Test migrations thoroughly**
3. **Include rollback scripts**
4. **Use transactions when appropriate**
5. **Document performance implications**
6. **Follow consistent naming patterns**

## Review Process

### Before Merging
- [ ] Changelog entries are accurate and complete
- [ ] Database migrations are tested
- [ ] Rollback procedures are documented
- [ ] Breaking changes are clearly marked
- [ ] Version numbers follow semantic versioning

### Release Checklist
- [ ] Move unreleased changes to new version section
- [ ] Update version links at bottom of changelog
- [ ] Verify all database migrations are included
- [ ] Confirm changelog reflects actual changes
- [ ] Tag release with appropriate version number

## Tools and Automation

### Recommended Tools
- Use templates from `docs/templates/`
- Validate changelog format with tools
- Automate version link updates
- Generate release notes from changelog

### Integration
- Link changelog updates to CI/CD pipeline
- Require changelog updates for feature PRs
- Automate database migration validation
- Generate migration documentation

## Examples

See `docs/templates/` for:
- Database migration template
- Changelog entry template
- Version release template