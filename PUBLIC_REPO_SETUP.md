# Public Repository Setup Guide

This guide helps you create a clean, public-facing version of the repository while preserving your current working state.

## Strategy

We'll use Git branches to:
1. **Preserve** your current state in a backup branch
2. **Create** a clean public branch for demo purposes
3. **Maintain** both versions in the same repository

## Quick Start

Run the preparation script:

```bash
./scripts/prepare-public-repo.sh
```

This will:
- Create a backup branch (`backup/pre-public-cleanup-YYYYMMDD`)
- Create a backup tag for easy reference
- Create a clean public branch (`public/demo-clean`)
- Remove unnecessary files from the public branch

## Manual Process

If you prefer to do it manually:

### Step 1: Create Backup

```bash
# Ensure all changes are committed
git add .
git commit -m "Save current state before cleanup"

# Create backup branch
git checkout -b backup/pre-public-cleanup-$(date +%Y%m%d)

# Create backup tag
git tag -a backup/pre-public-cleanup-$(date +%Y%m%d) -m "Backup before public cleanup"

# Return to main branch
git checkout main  # or your default branch
```

### Step 2: Create Public Branch

```bash
# Create public branch from current state
git checkout -b public/demo-clean
```

### Step 3: Clean Up Public Branch

Remove unnecessary files:

```bash
# Remove duplicate/unused directories
rm -rf src/ web/ claude/ mcp/ heroku/

# Remove example files (covered in README)
rm -rf server/src/main/resources/examples/
rm -f scripts/seed_data.apex scripts/seed_examples.apex

# Remove planning/internal docs
rm -f ap.plan.md DEMO-RUNBOOK.md DEPLOY.md IFRAME_TROUBLESHOOTING.md

# Remove build artifacts
rm -rf server/target/ node_modules/
```

### Step 4: Commit Cleanup

```bash
git add -A
git commit -m "Cleanup: prepare public demo repository

- Remove duplicate src/ directory
- Remove unused web/ directory  
- Remove optional claude/ and mcp/ directories
- Remove example files (covered in README)
- Remove planning/internal documentation
- Remove build artifacts"
```

## Branch Structure

After setup, you'll have:

```
main (or your default branch)
├── backup/pre-public-cleanup-YYYYMMDD  (preserves current state)
└── public/demo-clean                   (clean public version)
```

## Using the Branches

### Work on Current Demo
```bash
git checkout main  # or your default branch
# Make changes, test, etc.
```

### Update Public Version
```bash
git checkout public/demo-clean
git merge main  # Bring in changes from main
# Review and remove any new files that shouldn't be public
git commit -m "Update public demo with latest changes"
```

### Restore from Backup
```bash
git checkout backup/pre-public-cleanup-YYYYMMDD
# Or use the tag
git checkout backup/pre-public-cleanup-YYYYMMDD
```

## Publishing the Public Version

### Option 1: Push Public Branch
```bash
git checkout public/demo-clean
git push origin public/demo-clean
```

### Option 2: Create Separate Public Repo
```bash
# Create a new directory for public repo
cd ..
git clone <your-repo-url> apex-zombie-killer-public
cd apex-zombie-killer-public
git checkout public/demo-clean
git push origin public/demo-clean

# Or push to a different remote
git remote add public <public-repo-url>
git push public public/demo-clean:main
```

### Option 3: GitHub Actions (Automated)
Create `.github/workflows/publish-public.yml`:

```yaml
name: Publish Public Demo

on:
  push:
    branches:
      - public/demo-clean

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Push to public repo
        run: |
          git remote add public <public-repo-url>
          git push public public/demo-clean:main
```

## What Gets Removed

The cleanup removes:

- **Duplicate code**: `src/` (duplicate of `server/src/`)
- **Unused frontend**: `web/` (UI is in `server/src/main/resources/static/`)
- **Optional tools**: `claude/`, `mcp/`, `heroku/`
- **Example files**: Already documented in README
- **Internal docs**: Planning and troubleshooting docs
- **Build artifacts**: `target/`, `node_modules/`

## What Stays

Essential files remain:

- ✅ `README.md` - Main documentation
- ✅ `FLOW_SETUP_GUIDE.md` - Detailed flow instructions
- ✅ `server/` - All source code
- ✅ `force-app/` - Salesforce metadata
- ✅ `scripts/` - Deployment scripts
- ✅ Build configs: `pom.xml`, `Procfile`, `system.properties`
- ✅ `apispec.yaml` - OpenAPI template
- ✅ `.gitignore` - Updated ignore rules

## Verification

After cleanup, verify:

```bash
# Build still works
mvn clean package

# Heroku deploy still works
git push heroku public/demo-clean:main

# Salesforce deploy still works
sf project deploy start --source-dir force-app --target-org <org-alias>
```

## Maintenance

### Keep Public Branch Updated

```bash
# Periodically sync public branch with main
git checkout public/demo-clean
git merge main
# Review and clean up any new files
git commit -m "Update public demo"
git push origin public/demo-clean
```

### Add New Files to Public Branch

When adding new files to main, decide:
- **Public-friendly**: Merge to public branch
- **Internal-only**: Don't merge, or remove during merge

## Troubleshooting

### Accidentally Deleted Something Important

```bash
# Restore from backup branch
git checkout backup/pre-public-cleanup-YYYYMMDD -- <file-path>
git checkout public/demo-clean
git add <file-path>
git commit -m "Restore accidentally deleted file"
```

### Need to Re-run Cleanup

```bash
git checkout public/demo-clean
# Re-run cleanup commands
# Commit changes
```

---

**Remember**: Your original work is safely preserved in the backup branch and tag!

