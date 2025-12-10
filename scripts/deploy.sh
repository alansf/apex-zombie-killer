#!/bin/bash
# Deployment script for Apex Zombie Killer
# Usage: ./scripts/deploy.sh [heroku|salesforce|all]

set -e

APP_NAME="apex-zombie-killer"
SF_ORG_ALIAS="purple-zombie"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

cd "$REPO_ROOT"

deploy_heroku() {
    echo "=== Deploying to Heroku ==="
    
    # Build
    echo "Building application..."
    mvn -q -DskipTests -f pom.xml clean package
    
    # Ensure buildpack is set
    echo "Setting buildpack..."
    heroku buildpacks:clear -a "$APP_NAME" || true
    heroku buildpacks:add heroku/java -a "$APP_NAME" || true
    
    # Deploy
    echo "Pushing to Heroku..."
    git fetch heroku main || true
    if git push heroku main 2>&1 | grep -q "non-fast-forward"; then
        echo "Warning: Non-fast-forward detected. Use 'git push heroku main --force' if needed."
        read -p "Force push? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            git push heroku main --force
        else
            echo "Deployment cancelled."
            exit 1
        fi
    fi
    
    echo "✓ Heroku deployment complete"
    echo "View logs: heroku logs --tail -a $APP_NAME"
}

deploy_salesforce() {
    echo "=== Deploying to Salesforce ==="
    
    # Check if org is logged in
    if ! sf org list --json | grep -q "\"alias\":\"$SF_ORG_ALIAS\""; then
        echo "Org $SF_ORG_ALIAS not found. Logging in..."
        sf org login web --instance-url "https://$SF_ORG_ALIAS.my.salesforce.com" --alias "$SF_ORG_ALIAS" --set-default
    fi
    
    # Deploy metadata
    echo "Deploying Salesforce metadata..."
    sf project deploy start --source-dir force-app --target-org "$SF_ORG_ALIAS"
    
    # Assign permission set
    echo "Assigning permission set..."
    sf org assign permset --name ManageHerokuAppLink --target-org "$SF_ORG_ALIAS" || echo "Permission set assignment skipped (may already be assigned)"
    
    echo "✓ Salesforce deployment complete"
}

deploy_all() {
    deploy_heroku
    echo ""
    deploy_salesforce
    echo ""
    echo "=== Deployment Summary ==="
    echo "Heroku app: https://$APP_NAME-6f48e437a14e.herokuapp.com"
    echo "Salesforce org: $SF_ORG_ALIAS"
    echo ""
    echo "Next steps:"
    echo "1. Verify Heroku: heroku logs --tail -a $APP_NAME"
    echo "2. Import External Service from: https://$APP_NAME-6f48e437a14e.herokuapp.com/openapi-generated.yaml"
    echo "3. Patch flows: ./scripts/flow/patch-flow-op.sh <name>"
    echo "4. Redeploy flows: sf project deploy start --source-dir force-app --target-org $SF_ORG_ALIAS"
}

case "${1:-all}" in
    heroku)
        deploy_heroku
        ;;
    salesforce)
        deploy_salesforce
        ;;
    all)
        deploy_all
        ;;
    *)
        echo "Usage: $0 [heroku|salesforce|all]"
        exit 1
        ;;
esac

