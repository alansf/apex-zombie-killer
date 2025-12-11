#!/bin/bash
# Prepare a clean public repository version while preserving current state
# This script creates a backup branch and prepares a clean public branch

set -e

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

echo "=== Preparing Public Repository Version ==="
echo ""

# Check if we're in a git repo
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "Error: Not a git repository. Initialize git first: git init"
    exit 1
fi

# Get current branch
CURRENT_BRANCH=$(git branch --show-current)
echo "Current branch: $CURRENT_BRANCH"
echo ""

# Step 1: Create backup branch/tag
echo "=== Step 1: Creating backup of current state ==="
BACKUP_BRANCH="backup/pre-public-cleanup-$(date +%Y%m%d)"
BACKUP_TAG="backup/pre-public-cleanup-$(date +%Y%m%d)"

# Commit any uncommitted changes first
if ! git diff-index --quiet HEAD --; then
    echo "Uncommitted changes detected. Stashing..."
    git stash push -m "Auto-stash before backup"
    STASHED=true
else
    STASHED=false
fi

# Create backup branch from current state
git checkout -b "$BACKUP_BRANCH" 2>/dev/null || git checkout "$BACKUP_BRANCH"
echo "✓ Created backup branch: $BACKUP_BRANCH"

# Create backup tag
git tag -a "$BACKUP_TAG" -m "Backup before public repo cleanup" 2>/dev/null || true
echo "✓ Created backup tag: $BACKUP_TAG"

# Return to original branch
git checkout "$CURRENT_BRANCH"
if [ "$STASHED" = true ]; then
    git stash pop
fi

echo ""
echo "=== Step 2: Creating clean public branch ==="
PUBLIC_BRANCH="public/demo-clean"

# Check if public branch exists
if git show-ref --verify --quiet refs/heads/"$PUBLIC_BRANCH"; then
    echo "Public branch already exists. Updating..."
    git checkout "$PUBLIC_BRANCH"
    git merge "$CURRENT_BRANCH" --no-edit || echo "Merge conflicts - resolve manually"
else
    echo "Creating new public branch from $CURRENT_BRANCH..."
    git checkout -b "$PUBLIC_BRANCH"
fi

echo "✓ On public branch: $PUBLIC_BRANCH"
echo ""

# Step 3: Run cleanup
echo "=== Step 3: Running cleanup ==="
echo "This will remove unnecessary files for public demo..."
echo ""

# Remove duplicate/unused directories
[ -d "src" ] && rm -rf src/ && echo "✓ Removed src/"
[ -d "web" ] && rm -rf web/ && echo "✓ Removed web/"
[ -d "claude" ] && rm -rf claude/ && echo "✓ Removed claude/"
[ -d "mcp" ] && rm -rf mcp/ && echo "✓ Removed mcp/"
[ -d "heroku" ] && rm -rf heroku/ && echo "✓ Removed heroku/"

# Remove example files (covered in README)
[ -d "server/src/main/resources/examples" ] && rm -rf server/src/main/resources/examples/ && echo "✓ Removed examples/"
[ -f "scripts/seed_data.apex" ] && rm -f scripts/seed_data.apex && echo "✓ Removed seed_data.apex"
[ -f "scripts/seed_examples.apex" ] && rm -f scripts/seed_examples.apex && echo "✓ Removed seed_examples.apex"

# Remove planning/internal docs
[ -f "ap.plan.md" ] && rm -f ap.plan.md && echo "✓ Removed ap.plan.md"
[ -f "DEMO-RUNBOOK.md" ] && rm -f DEMO-RUNBOOK.md && echo "✓ Removed DEMO-RUNBOOK.md"
[ -f "DEPLOY.md" ] && rm -f DEPLOY.md && echo "✓ Removed DEPLOY.md"
[ -f "IFRAME_TROUBLESHOOTING.md" ] && rm -f IFRAME_TROUBLESHOOTING.md && echo "✓ Removed IFRAME_TROUBLESHOOTING.md"

# Remove build artifacts
[ -d "server/target" ] && rm -rf server/target/ && echo "✓ Removed server/target/"
[ -d "node_modules" ] && rm -rf node_modules/ && echo "✓ Removed node_modules/"

echo ""
echo "=== Step 4: Staging cleanup changes ==="
git add -A
echo "✓ Changes staged"
echo ""

echo "=== Summary ==="
echo ""
echo "✓ Backup branch created: $BACKUP_BRANCH"
echo "✓ Backup tag created: $BACKUP_TAG"
echo "✓ Public branch ready: $PUBLIC_BRANCH"
echo ""
echo "Current status:"
git status --short
echo ""
echo "Next steps:"
echo "1. Review changes: git diff --cached"
echo "2. Commit cleanup: git commit -m 'Cleanup: prepare public demo repository'"
echo "3. Push public branch: git push origin $PUBLIC_BRANCH"
echo "4. To restore backup: git checkout $BACKUP_BRANCH"
echo ""

