# Public Repository Strategy

## Branch Visibility

**Important**: In Git, branches are NOT hidden. If you push a branch to a public repository, anyone can see it.

## Recommended Approaches

### Option 1: Local Backup, Public Clean Branch (Recommended)

**Keep backup branch LOCAL ONLY:**

```bash
# Backup branch stays local (never push it)
git checkout backup/pre-public-cleanup-20251211
# This branch is only on your machine

# Only push the public branch
git checkout public/demo-clean
git push origin public/demo-clean

# Or push public branch as 'main' to a separate public repo
git remote add public <public-repo-url>
git push public public/demo-clean:main
```

**Pros:**
- Backup stays private on your machine
- Public repo only has clean version
- Simple and secure

**Cons:**
- Backup only exists locally (make sure to backup your machine!)

### Option 2: Separate Repositories (Best for Public Sharing)

**Create a completely separate public repository:**

```bash
# On your local machine
cd ..
git clone <your-private-repo-url> apex-zombie-killer-public
cd apex-zombie-killer-public

# Checkout the clean branch
git checkout public/demo-clean

# Push to separate public repo
git remote set-url origin <public-repo-url>
git push origin public/demo-clean:main
```

**Pros:**
- Complete separation between private and public
- Public repo has no history of internal files
- Can use different access controls

**Cons:**
- Need to maintain two repositories
- More complex sync process

### Option 3: Private Main Repo, Public Fork

**Keep your repo private, create public fork:**

1. Keep your main repository **private**
2. Create a **public fork** on GitHub/GitLab
3. Only push `public/demo-clean` branch to the fork
4. Set the fork's default branch to `public/demo-clean`

**Pros:**
- Easy to maintain
- Clear separation
- Can update public fork easily

**Cons:**
- Requires private repo (if free tier allows)

## What NOT to Do

❌ **Don't push backup branch to public repo** - Contains internal files, planning docs, etc.

❌ **Don't push main branch if it has sensitive info** - Only push clean public branch

## Recommended Workflow

### For Your Private Repository

```bash
# Keep all branches local or in private repo
main                          # Your working branch
backup/pre-public-cleanup-*   # Local backup (never push)
public/demo-clean             # Clean version (can push to public)
```

### For Public Repository

```bash
# Only push the clean branch
main (from public/demo-clean)  # Only this branch exists publicly
```

## Step-by-Step: Publishing to Public

### If Using Same Repo (Make Repo Public)

```bash
# 1. Ensure backup branch is NOT pushed
git branch -r  # Check remote branches
# If backup branch is listed, delete it:
git push origin --delete backup/pre-public-cleanup-20251211

# 2. Push only public branch
git checkout public/demo-clean
git push origin public/demo-clean

# 3. On GitHub/GitLab, set public/demo-clean as default branch
# Settings → Branches → Default branch → public/demo-clean
```

### If Using Separate Public Repo

```bash
# 1. Create new public repository on GitHub/GitLab

# 2. Add it as remote
git remote add public <public-repo-url>

# 3. Push only the clean branch
git checkout public/demo-clean
git push public public/demo-clean:main

# 4. Future updates
git checkout public/demo-clean
git merge main  # Bring in changes
# Review and clean up
git push public public/demo-clean:main
```

## Summary

**Best Practice:**
- ✅ Keep `backup/` branches **LOCAL ONLY** (never push)
- ✅ Push only `public/demo-clean` to public repository
- ✅ Use separate repository for public version if possible
- ✅ Keep your main working repository private

**Your Current Setup:**
- `backup/pre-public-cleanup-20251211` → **Keep local, don't push**
- `public/demo-clean` → **This is what you publish**
- `main` → **Your working branch (keep private)**

