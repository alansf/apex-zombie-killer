# Flow Setup Guide: Adding Screens and External Service Actions

## Why Screens Are Needed

### Screen Flow vs Autolaunched Flow

**Screen Flow (`ExecByName_Screen`):**
- **Purpose**: Interactive user-facing flow that collects input and displays results
- **Why screens**: Users need a UI to:
  - Enter input (payload JSON)
  - See execution results
  - Navigate through the flow
- **Use cases**: 
  - Launch from App Launcher
  - Add to Record Pages
  - Call from other Flows or Apex
  - User-initiated actions

**Autolaunched Flow (`ExecByName_Auto`):**
- **Purpose**: Background automation that runs without user interaction
- **Why screens**: Not needed - runs programmatically
- **Use cases**:
  - Called from Apex: `Flow.Interview.ExecByName_Auto.start(...)`
  - Subflow in other Flows
  - Scheduled or triggered automation

## How to Add Screens in Flow Builder

### Step-by-Step: Screen Flow Setup

#### 1. Open Flow Builder
- Setup → Flows → Find `ExecByName_Screen` → Click Edit

#### 2. Add Input Screen
**Why**: Collect the payload JSON from the user

**Steps**:
1. Click **"+"** button (or drag Screen element from palette)
2. Select **"Screen"** element
3. Configure the screen:
   - **Label**: "Enter Payload"
   - **API Name**: `Screen_Input` (or keep default)
4. Add a field to the screen:
   - Click **"Add a Field"** in the screen
   - Select **"Variable"** → Choose `PayloadJSON`
   - Configure field:
     - **Label**: "Payload (JSON)"
     - **Required**: No (or Yes if needed)
     - **Help Text**: "Enter JSON payload for the execution"
5. Connect the screen:
   - From Start element → Connect to `Screen_Input`
   - From `Screen_Input` → Connect to External Service action (next step)

#### 3. Add External Service Action
**Why**: Call your Heroku endpoint to execute the code

**Steps**:
1. Click **"+"** after `Screen_Input`
2. Select **"Action"** → **"External Service"**
3. Select your External Service (imported from `/openapi-generated.yaml`)
4. Choose the action: `exec_ConvertedFromApex` (or your code name)
5. Map inputs:
   - **payload**: Select `{!PayloadJSON}` (from the screen input)
6. Map outputs to variables:
   - **status** → `{!ResultStatus}`
   - **error** → `{!ResultError}` (if available)
7. Handle errors:
   - Add fault path if action fails
   - Set `ResultError` variable on fault

#### 4. Add Result Screen
**Why**: Display the execution results to the user

**Steps**:
1. Click **"+"** after External Service action
2. Select **"Screen"** element
3. Configure the screen:
   - **Label**: "Execution Result"
   - **API Name**: `Screen_Result`
4. Add display fields:
   - Click **"Add a Field"** → **"Display Text"**
   - **Label**: "Status"
   - **Value**: `{!ResultStatus}`
   - Add another display field for `{!ResultError}` if needed
5. Add navigation:
   - **Finish Button**: "Done" (default)
   - **Back Button**: Optional (allows going back to edit input)

#### 5. Connect Everything
```
Start → Screen_Input → External Service Action → Screen_Result → End
```

#### 6. Save and Activate
- Click **"Save"**
- Click **"Activate"** (or "Save for Later" to test first)

### Step-by-Step: Autolaunched Flow Setup

#### 1. Open Flow Builder
- Setup → Flows → Find `ExecByName_Auto` → Click Edit

#### 2. Replace Placeholder with External Service Action
**Why**: Autolaunched flows don't need screens - they run programmatically

**Steps**:
1. Delete the "Placeholder Assignment" element
2. Click **"+"** after Start
3. Select **"Action"** → **"External Service"**
4. Select your External Service
5. Choose action: `exec_ConvertedFromApex`
6. Map inputs:
   - **payload**: `{}` (empty object) or use a variable if you have input
7. Map outputs:
   - **status** → `{!ResultStatus}`
8. Handle errors (optional):
   - Add fault path
   - Log errors or set error variables

#### 3. Save and Activate
- Click **"Save"**
- Click **"Activate"**

## Visual Flow Structure

### Screen Flow (Complete)
```
┌─────────┐
│  Start  │
└────┬────┘
     │
     ▼
┌─────────────────┐
│  Screen_Input   │  ← User enters PayloadJSON
│  - PayloadJSON  │
└────┬────────────┘
     │
     ▼
┌──────────────────────────┐
│ External Service Action  │  ← Calls exec_ConvertedFromApex
│ exec_ConvertedFromApex   │
└────┬─────────────────────┘
     │
     ▼
┌─────────────────┐
│ Screen_Result   │  ← Shows ResultStatus
│ - Status        │
│ - Error         │
└────┬────────────┘
     │
     ▼
┌─────────┐
│   End   │
└─────────┘
```

### Autolaunched Flow (Complete)
```
┌─────────┐
│  Start  │
└────┬────┘
     │
     ▼
┌──────────────────────────┐
│ External Service Action  │  ← Calls exec_ConvertedFromApex
│ exec_ConvertedFromApex   │     (no user input needed)
└────┬─────────────────────┘
     │
     ▼
┌─────────┐
│   End   │
└─────────┘
```

## Field Types in Flow Builder

When adding fields to screens in Flow Builder, you'll see these options:

### Input Fields
- **Text**: Single-line text input
- **Text Area**: Multi-line text input (good for JSON)
- **Number**: Numeric input
- **Currency**: Currency input
- **Date/DateTime**: Date/time pickers
- **Picklist**: Dropdown selection
- **Checkbox**: Boolean toggle
- **Radio Buttons**: Single selection from options

### Display Fields
- **Display Text**: Shows variable values or formulas
- **Rich Text**: Formatted text display
- **Rich Text Display**: HTML-formatted content

**Note**: Flow Builder automatically handles field types correctly - you don't need to worry about enum values like we had in the metadata XML.

## Example: Complete Screen Flow Configuration

### Screen 1: Input
```
Screen Label: "Execute Code"
Fields:
  - PayloadJSON (Text Area, Required: No)
    Label: "Payload (JSON)"
    Help Text: "Enter JSON payload, e.g., {\"key\":\"value\"}"
```

### Action: External Service
```
Service: HerokuAPI (your External Service name)
Action: exec_ConvertedFromApex
Inputs:
  - payload: {!PayloadJSON}
Outputs:
  - status → {!ResultStatus}
  - error → {!ResultError}
```

### Screen 2: Result
```
Screen Label: "Execution Complete"
Fields:
  - Display Text
    Label: "Status"
    Value: {!ResultStatus}
  - Display Text (if error exists)
    Label: "Error"
    Value: {!ResultError}
```

## Testing Your Flows

### Test Screen Flow
1. **From App Launcher**:
   - Search for "Execute Code by Name"
   - Launch the flow
   - Enter payload: `{"test":"data"}`
   - Click Next → See results

2. **From Record Page**:
   - Add flow to Opportunity/Account page
   - Flow launches when user clicks button
   - User enters payload → sees results

### Test Autolaunched Flow
1. **From Apex**:
```apex
Map<String, Object> inputs = new Map<String, Object>();
Flow.Interview.ExecByName_Auto flow = new Flow.Interview.ExecByName_Auto(inputs);
flow.start();
```

2. **From Another Flow**:
   - Add "Subflow" element
   - Select `ExecByName_Auto`
   - Pass inputs if needed

## Common Issues and Solutions

### Issue: "External Service action not found"
**Solution**: 
- Ensure External Service is imported
- Check action name matches exactly (case-sensitive)
- Verify Named Credential is configured

### Issue: "Variable not found"
**Solution**:
- Ensure variable exists in flow
- Check variable API name spelling
- Verify variable is accessible in current scope

### Issue: "Screen field not displaying"
**Solution**:
- Check field visibility settings
- Verify variable has a value
- Ensure field is added to correct screen

### Issue: "Flow activation fails"
**Solution**:
- Check all required fields are mapped
- Verify External Service is active
- Ensure no circular references in flow

## Best Practices

1. **Screen Flow**:
   - Always provide clear labels and help text
   - Validate input before calling External Service
   - Show meaningful error messages
   - Use appropriate field types (Text Area for JSON)

2. **Autolaunched Flow**:
   - Handle errors gracefully
   - Log execution results
   - Use variables for all inputs/outputs
   - Test with different payloads

3. **Both**:
   - Use descriptive variable names
   - Document your flow with descriptions
   - Test thoroughly before activating
   - Consider adding decision elements for error handling

## Summary

- **Screens** provide the UI for Screen Flows (user interaction)
- **Autolaunched Flows** don't need screens (run programmatically)
- **Flow Builder** handles field types automatically (no enum issues)
- **External Service actions** connect your flow to Heroku endpoints
- **Variables** store input/output data throughout the flow

The minimal flows we deploy are just starting points - Flow Builder makes it easy to add screens, actions, and logic visually without dealing with metadata XML complexities.

