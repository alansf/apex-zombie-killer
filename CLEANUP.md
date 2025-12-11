# Repository Cleanup Guide

This document outlines files and directories that can be safely removed or consolidated for a cleaner demo repository.

## Files/Directories to Remove

### 1. Duplicate/Unused Code Directories
- **`src/`** (root level) - Duplicate of `server/src/`, appears to be old structure
- **`web/`** - Unused React/Vite frontend (UI is in `server/src/main/resources/static/index.html`)

### 2. Build Artifacts (already in .gitignore)
- **`server/target/`** - Maven build output
- **`node_modules/`** - NPM dependencies

### 3. Optional Demo Files
- **`claude/`** - Agent prompts (optional, can be removed if not needed)
- **`mcp/`** - MCP tools configuration (optional, can be removed if not using MCP)
- **`heroku/connect-mapping.json`** - Heroku Connect mapping (if not using Heroku Connect)

### 4. Documentation Consolidation
Consider consolidating these into README.md:
- **`DEMO-RUNBOOK.md`** - Move key points to README
- **`DEPLOY.md`** - Already covered in README
- **`IFRAME_TROUBLESHOOTING.md`** - Move to Troubleshooting section in README
- **`FLOW_SETUP_GUIDE.md`** - Keep separate (detailed guide)

### 5. Example Files
- **`server/src/main/resources/examples/`** - Already covered in README Apex examples section
- **`scripts/seed_data.apex`** and **`scripts/seed_examples.apex`** - If not needed

### 6. Planning Files
- **`ap.plan.md`** - Internal planning doc, can be removed for public repo

## Cleanup Commands

### Safe to Remove (backup first!)
```bash
# Remove duplicate src directory
rm -rf src/

# Remove unused web directory
rm -rf web/

# Remove build artifacts (if committed)
rm -rf server/target/

# Remove optional directories (if not needed)
rm -rf claude/
rm -rf mcp/
rm -rf heroku/

# Remove example files (if covered in README)
rm -rf server/src/main/resources/examples/
rm -f scripts/seed_data.apex scripts/seed_examples.apex

# Remove planning/internal docs
rm -f ap.plan.md
rm -f DEMO-RUNBOOK.md
rm -f DEPLOY.md
rm -f IFRAME_TROUBLESHOOTING.md
```

### Consolidate Documentation
Move key content from removed docs into README.md sections:
- Deployment steps → Already in README
- Troubleshooting → Already in README
- Demo runbook → Add to Demo Flow section

## Recommended Structure After Cleanup

```
apex-zombie-killer/
├── README.md                    # Main documentation
├── FLOW_SETUP_GUIDE.md         # Detailed flow setup (keep separate)
├── .gitignore                   # Updated ignore rules
├── pom.xml                      # Root Maven POM
├── Procfile                     # Heroku Procfile
├── system.properties            # Java version
├── apispec.yaml                # OpenAPI spec template
├── sfdx-project.json            # Salesforce project config
├── server/                      # Spring Boot application
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/           # Java source code
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── schema.sql
│   │   │       └── static/
│   │   │           └── index.html
│   │   └── test/                # Tests
│   └── system.properties
├── force-app/                   # Salesforce metadata
│   └── main/default/
│       ├── classes/
│       ├── cspTrustedSites/
│       ├── flows/
│       ├── lwc/
│       ├── namedCredentials/
│       └── permissionsets/
└── scripts/                      # Utility scripts
    ├── deploy.sh
    ├── push-all.sh
    └── flow/
        └── patch-flow-op.sh
```

## What to Keep

✅ **Essential Files:**
- `README.md` - Main documentation
- `FLOW_SETUP_GUIDE.md` - Detailed flow instructions
- `server/` - All source code
- `force-app/` - All Salesforce metadata
- `scripts/` - Deployment scripts
- `pom.xml`, `Procfile`, `system.properties` - Build/deploy configs
- `apispec.yaml` - OpenAPI template
- `sfdx-project.json` - Salesforce project config

✅ **Optional but Useful:**
- `Makefile` - If you use it for common tasks
- `package.json` - If you have npm scripts

## Verification

After cleanup, verify:
1. `mvn clean package` still works
2. `git push heroku main` still works
3. `sf project deploy start` still works
4. All functionality still works

