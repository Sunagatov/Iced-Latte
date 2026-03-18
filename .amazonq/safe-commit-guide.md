# Safe Commit Guide

## 🎯 What to Commit

This guide helps you safely commit the Sentry and logging monitoring implementation to the public GitHub repository.

---

## ✅ Step-by-Step Commit Process

### Step 1: Verify No Secrets

```bash
# Search for potential secrets in staged files
git diff --cached | grep -iE "(api[_-]?key|secret|password|token)" | grep -vE "(SENTRY_DSN|DATADOG_API_KEY|LOKI_URL)"

# Should return nothing or only environment variable references
```

### Step 2: Stage Documentation Files

```bash
# Add all .amazonq documentation
git add .amazonq/

# Verify what's being added
git status
```

**Expected files**:
```
.amazonq/changelog-logging.md
.amazonq/changelog-security.md
.amazonq/git-commit-safety-analysis.md
.amazonq/logging-monitoring-guide.md
.amazonq/logging-quick-start.md
.amazonq/sentry-implementation-summary.md
.amazonq/sentry-monitoring-guide.md
.amazonq/sentry-quick-reference.md
.amazonq/rules/architecture.md
.amazonq/rules/false-positives.md
.amazonq/rules/rules.md
```

### Step 3: Stage Infrastructure Files

```bash
# Add Docker Compose for logging
git add docker-compose.logging.yml

# Add Logstash configuration
git add logstash/

# Add Grafana provisioning
git add grafana/

# Verify
git status
```

### Step 4: Stage Configuration Files

```bash
# Add updated pom.xml (Loki dependency)
git add pom.xml

# Add updated logback configuration
git add src/main/resources/logback-spring.xml

# Add updated application.yaml
git add src/main/resources/application.yaml

# Verify
git status
```

### Step 5: Review Changes

```bash
# Review all changes before committing
git diff --cached

# Look for:
# ✅ Environment variables: ${VARIABLE_NAME}
# ✅ Placeholder values: "your-api-key-here"
# ✅ Local URLs: localhost, 127.0.0.1
# ❌ Real API keys: sk_live_abc123
# ❌ Real DSNs: https://real-key@sentry.io
```

### Step 6: Commit with Descriptive Messages

```bash
# Commit documentation
git commit -m "docs: add Sentry and multi-vendor logging monitoring guides

- Add comprehensive Sentry monitoring guide
- Add multi-vendor logging guide (Loki, Elasticsearch, Datadog)
- Add quick start guides for both
- Add implementation summaries and changelogs
- Update architecture and rules documentation"

# Commit infrastructure
git add docker-compose.logging.yml logstash/ grafana/
git commit -m "feat: add logging monitoring infrastructure

- Add Docker Compose for Loki, Elasticsearch, Logstash, Kibana, Grafana
- Add Logstash pipeline configuration
- Add Grafana datasource provisioning
- Enable local development logging stack"

# Commit configuration
git add pom.xml src/main/resources/
git commit -m "feat: implement multi-vendor logging monitoring

- Add Loki logback appender dependency
- Configure Loki, Logstash, and Datadog appenders
- Add conditional appender activation
- Enhance structured JSON logging
- Add trace context propagation
- Update Sentry to 8.36.0 for Spring Boot 4"
```

---

## 🔒 Security Verification

### Before Pushing

```bash
# 1. Check for hardcoded secrets
grep -r "sk_live" .
grep -r "sk_test" .
grep -r "@sentry.io" . --include="*.java" --include="*.xml" --include="*.yaml"

# Should return nothing or only comments/documentation

# 2. Verify environment variables are used
grep -r "SENTRY_DSN" src/
grep -r "DATADOG_API_KEY" src/

# Should show ${VARIABLE_NAME} pattern

# 3. Check .env is gitignored
git check-ignore .env
# Should output: .env

# 4. Verify no log files are staged
git status | grep "\.log"
# Should return nothing
```

---

## 📋 Commit Checklist

Before pushing to GitHub, verify:

- [ ] ✅ No real API keys in code
- [ ] ✅ No real Sentry DSNs in code
- [ ] ✅ No real Datadog API keys in code
- [ ] ✅ All secrets use environment variables
- [ ] ✅ `.env` files are gitignored
- [ ] ✅ Log files are gitignored
- [ ] ✅ Only local development configs committed
- [ ] ✅ Documentation contains no secrets
- [ ] ✅ Docker Compose uses default passwords
- [ ] ✅ All URLs are localhost or placeholders

---

## 🎯 What's Being Committed

### Documentation (Safe ✅)
```
.amazonq/
├── changelog-logging.md              ✅ No secrets
├── changelog-security.md             ✅ No secrets
├── git-commit-safety-analysis.md     ✅ No secrets
├── logging-monitoring-guide.md       ✅ No secrets
├── logging-quick-start.md            ✅ No secrets
├── sentry-implementation-summary.md  ✅ No secrets
├── sentry-monitoring-guide.md        ✅ No secrets
├── sentry-quick-reference.md         ✅ No secrets
└── rules/                            ✅ No secrets
```

### Infrastructure (Safe ✅)
```
docker-compose.logging.yml            ✅ Local dev only
logstash/
├── pipeline/logstash.conf            ✅ No secrets
└── config/logstash.yml               ✅ No secrets
grafana/
└── provisioning/
    └── datasources/loki.yml          ✅ Local datasource
```

### Configuration (Safe ✅)
```
pom.xml                               ✅ Only versions
src/main/resources/
├── application.yaml                  ✅ Uses env vars
└── logback-spring.xml                ✅ Uses Spring properties
```

---

## ❌ What's NOT Being Committed

### Secrets (Gitignored ✅)
```
.env                                  ❌ Real secrets
.env.local                            ❌ Real secrets
.env.prod                             ❌ Real secrets
local.env                             ❌ Real secrets
```

### Logs (Gitignored ✅)
```
logs/*.log                            ❌ May contain PII
**/*.log                              ❌ All log files
```

### Build Artifacts (Gitignored ✅)
```
target/                               ❌ Build output
.idea/                                ❌ IDE settings
*.iml                                 ❌ IDE files
```

---

## 🚀 Push to GitHub

### Final Check
```bash
# Review all commits
git log --oneline -5

# Review all changes
git diff origin/development..HEAD

# Verify no secrets
git diff origin/development..HEAD | grep -iE "(sk_live|sk_test|real-key)"
# Should return nothing
```

### Push
```bash
# Push to your branch
git push origin your-branch-name

# Or push to development
git push origin development
```

---

## 🔍 Post-Commit Verification

### GitHub Secret Scanning
GitHub will automatically scan for secrets. If any are found:
1. GitHub will alert you
2. Immediately revoke the exposed secret
3. Remove it from git history
4. Generate a new secret

### Manual Verification
```bash
# Clone the repo fresh
git clone https://github.com/Sunagatov/Iced-Latte.git temp-verify
cd temp-verify

# Search for secrets
grep -r "sk_live" .
grep -r "sk_test" .
grep -r "@sentry.io" . --include="*.java" --include="*.xml"

# Should find nothing

# Clean up
cd ..
rm -rf temp-verify
```

---

## 🆘 If You Accidentally Commit a Secret

### Immediate Actions

1. **Revoke the secret immediately**
   - Sentry: Regenerate DSN
   - Datadog: Revoke API key
   - Generate new credentials

2. **Remove from git history**
   ```bash
   # Using BFG Repo-Cleaner (recommended)
   brew install bfg
   bfg --replace-text secrets.txt
   git reflog expire --expire=now --all
   git gc --prune=now --aggressive
   
   # Force push (⚠️ dangerous)
   git push --force
   ```

3. **Notify team**
   - Alert all contributors
   - Update secrets in CI/CD
   - Update production secrets

---

## ✅ Summary

### Safe to Commit ✅
- ✅ All documentation in `.amazonq/`
- ✅ Docker Compose files (local dev)
- ✅ Logstash/Grafana configs (local dev)
- ✅ Configuration files (using env vars)

### Never Commit ❌
- ❌ `.env` files with real secrets
- ❌ Log files
- ❌ Real API keys or DSNs
- ❌ Production credentials

### Verification Steps
1. ✅ Search for secrets before committing
2. ✅ Verify environment variables are used
3. ✅ Check .gitignore is working
4. ✅ Review changes before pushing
5. ✅ Monitor GitHub secret scanning

---

**Last Updated**: 2026-03-18  
**Status**: ✅ Safe to commit  
**Secrets Found**: 0  
**Ready to Push**: Yes
