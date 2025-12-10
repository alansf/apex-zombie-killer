#!/bin/bash
# Push updates to both origin (GitHub/GitLab) and Heroku
# Usage: ./scripts/push-all.sh [commit-message]

set -e

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

# Default commit message
COMMIT_MSG="${1:-Update: $(date +%Y-%m-%d\ %H:%M:%S)}"

echo "=== Checking Git Status ==="
git status --short

echo ""
read -p "Commit and push these changes? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 1
fi

echo ""
echo "=== Staging Changes ==="
git add .

echo ""
echo "=== Committing ==="
git commit -m "$COMMIT_MSG"

echo ""
echo "=== Pushing to Origin ==="
git push origin main || git push origin master

echo ""
echo "=== Pushing to Heroku ==="
git push heroku main || git push heroku master

echo ""
echo "✓ Successfully pushed to origin and Heroku"
echo ""
echo "View Heroku logs: heroku logs --tail -a apex-zombie-killer"

