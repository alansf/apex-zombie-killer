# Iframe Content Blocked - Troubleshooting Guide

## Issue: Content Blocked in Lightning Page

If you see a "broken file" icon or content blocked error in the Lightning page, follow these steps:

## Quick Fixes

### 1. Verify CSP Trusted Site is Active
- Setup → Security → CSP Trusted Sites
- Find `HerokuZombieTrustedSite`
- Ensure it's **Active** and URL matches: `https://apex-zombie-killer-6f48e437a14e.herokuapp.com`
- Context should be set to **All**

### 2. Check LWC Component URL
- Verify the `herokuAppContainer` component uses the correct URL
- Should be: `https://apex-zombie-killer-6f48e437a14e.herokuapp.com/`
- Check if `appUrl` property is set correctly on the Lightning Page

### 3. Redeploy CSP Trusted Site
```bash
cd /Users/alan.scott/Development/apex-zombie-killer
sf project deploy start --source-dir force-app/main/default/cspTrustedSites --target-org purple-zombie
```

### 4. Clear Browser Cache
- Hard refresh: Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows)
- Or clear browser cache and cookies for Salesforce

## Common Causes

### Cause 1: CSP Trusted Site Not Active
**Solution**: Activate in Setup → Security → CSP Trusted Sites

### Cause 2: URL Mismatch
**Solution**: Ensure CSP URL exactly matches the iframe src URL

### Cause 3: Context Not Set to "All"
**Solution**: Update CSP Trusted Site context to "All" (covers Lightning Experience, Visualforce, etc.)

### Cause 4: Heroku App Not Responding
**Solution**: 
```bash
# Check Heroku app status
heroku ps -a apex-zombie-killer

# Test the URL directly
curl https://apex-zombie-killer-6f48e437a14e.herokuapp.com/

# Check logs
heroku logs --tail -a apex-zombie-killer
```

### Cause 5: X-Frame-Options Header
**Solution**: Ensure Heroku app doesn't send `X-Frame-Options: DENY` header. Check Spring Boot CORS config.

### Cause 6: Content Security Policy Headers
**Solution**: Heroku app should allow framing. Check for CSP headers blocking iframe embedding.

## Verification Steps

### Step 1: Test Direct URL Access
Open in browser: `https://apex-zombie-killer-6f48e437a14e.herokuapp.com/`
- Should load the Heroku Transformer UI
- If not, fix Heroku app first

### Step 2: Test CSP Trusted Site
1. Setup → Security → CSP Trusted Sites
2. Click "Test Connection" (if available)
3. Or manually verify URL matches exactly

### Step 3: Check LWC Component
1. Setup → Custom Code → Lightning Components
2. Find `herokuAppContainer`
3. Verify it's deployed and active
4. Check the component's default URL

### Step 4: Verify Lightning Page Configuration
1. Setup → App Builder → Edit your Lightning Page
2. Check `herokuAppContainer` component properties
3. Verify `appUrl` is set correctly (or leave blank to use default)

## Advanced Troubleshooting

### Check Browser Console
1. Open browser Developer Tools (F12)
2. Check Console tab for CSP errors
3. Look for messages like:
   - "Refused to frame..."
   - "Content Security Policy..."
   - "X-Frame-Options..."

### Check Network Tab
1. Open Network tab in Developer Tools
2. Reload the Lightning page
3. Look for failed requests to Heroku URL
4. Check response headers for blocking headers

### Verify Heroku App Headers
```bash
# Check response headers
curl -I https://apex-zombie-killer-6f48e437a14e.herokuapp.com/

# Look for:
# - X-Frame-Options (should be absent or SAMEORIGIN)
# - Content-Security-Policy (should allow framing)
# - Access-Control-Allow-Origin (for CORS)
```

## Spring Boot CORS Configuration

If Heroku app is blocking iframe embedding, check `CorsConfig.java`:

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("*") // Or specific Salesforce domains
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*");
            }
        };
    }
}
```

**Important**: Don't set `X-Frame-Options: DENY` in Spring Boot security config.

## Alternative: Use AppLink SSO (If CSP Still Blocks)

If CSP continues to block, you can use Heroku AppLink with SSO:

1. Enable AppLink Service Mesh in `Procfile`
2. Use AppLink-authenticated URL in LWC
3. This requires AppLink addon and SSO configuration

## Quick Deploy Fix

If you've made changes, redeploy:

```bash
cd /Users/alan.scott/Development/apex-zombie-killer

# Deploy CSP and LWC
sf project deploy start \
  --source-dir force-app/main/default/cspTrustedSites \
  --source-dir force-app/main/default/lwc \
  --target-org purple-zombie

# Or deploy everything
sf project deploy start --source-dir force-app --target-org purple-zombie
```

## Still Not Working?

1. **Check Salesforce Release Notes**: CSP behavior may change with releases
2. **Try Different Context**: Change CSP context from "All" to "LightningExperience"
3. **Check Org Security Settings**: Setup → Security → Session Settings
4. **Verify Named Credential**: Ensure `HerokuJobs` Named Credential is configured
5. **Test in Different Browser**: Rule out browser-specific issues

## Expected Behavior

When working correctly:
- Purple banner with "Heroku Transformer" title
- "Open in New Tab" button visible
- Heroku app UI loads in iframe below banner
- No broken file icons or error messages

