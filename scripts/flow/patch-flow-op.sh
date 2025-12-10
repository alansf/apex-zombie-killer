#!/bin/bash
# Patch Flow templates to replace placeholder text with exec_<name>
# Note: This updates placeholder text only. You must still add the External Service
# action in Flow Builder after importing the External Service.
# Usage: ./scripts/flow/patch-flow-op.sh <name>
# Example: ./scripts/flow/patch-flow-op.sh ConvertedFromApex

set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <name>"
    echo "Example: $0 ConvertedFromApex"
    exit 1
fi

NAME="$1"
OP_ID="exec_${NAME}"
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
FLOW_DIR="${REPO_ROOT}/force-app/main/default/flows"

if [ ! -d "$FLOW_DIR" ]; then
    echo "Error: Flow directory not found at $FLOW_DIR"
    exit 1
fi

echo "Patching flows to use operationId: $OP_ID"

for flow_file in "${FLOW_DIR}"/*.flow-meta.xml; do
    if [ -f "$flow_file" ]; then
        echo "  Processing $(basename "$flow_file")..."
        sed -i.bak "s/exec__PLACEHOLDER__/$OP_ID/g" "$flow_file"
        rm -f "${flow_file}.bak"
    fi
done

echo "Done. Flows patched (placeholder text updated)."
echo ""
echo "Next steps:"
echo "1. Import External Service from /openapi-generated.yaml (if not already done)"
echo "2. Open flows in Flow Builder and add External Service action 'exec_${NAME}'"
echo "3. Deploy: sf project deploy start --source-dir force-app --target-org <org>"

