# Changelog Entry Template

Use this template when adding new entries to CHANGELOG.md

## Version Entry Template

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New feature descriptions
- New API endpoints
- New dependencies

### Changed
- Modified functionality
- Updated dependencies
- Performance improvements

### Deprecated
- Features marked for removal
- API endpoints to be removed

### Removed
- Deleted features
- Removed dependencies
- Cleaned up code

### Fixed
- Bug fixes
- Security patches
- Performance fixes

### Security
- Security improvements
- Vulnerability patches
- Authentication changes
```

## Database Migration Entry Template

```markdown
### Database Changes
- **Migration**: `YYYY-MM-DD.partN.description.sql`
- **Purpose**: Brief description of the migration
- **Impact**: Any breaking changes or important notes
- **Tables Affected**: List of tables modified/created
```

## Guidelines

1. **Keep entries concise but descriptive**
2. **Use present tense** ("Add feature" not "Added feature")
3. **Group related changes** under appropriate sections
4. **Include migration details** for database changes
5. **Reference issue numbers** when applicable: `(#123)`
6. **Mark breaking changes** with `**BREAKING:**` prefix