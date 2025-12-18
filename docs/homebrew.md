# Homebrew Distribution

Reference for maintaining the Homebrew formula.

## Architecture

- **Source repo:** `WilliamAGH/brief` - contains source code and release workflow
- **Tap repo:** `WilliamAGH/homebrew-tap` - contains `Formula/brief.rb`
- **Install command:** `brew install williamagh/tap/brief`

## What Happens Automatically

When you push a version tag (`v*`) to the brief repo, the release workflow:

1. Builds the distribution zip
2. Calculates SHA256 checksum
3. Creates a GitHub Release with the zip attached
4. Clones `homebrew-tap`, updates the formula with version/SHA
5. Pushes the updated formula to `homebrew-tap`

## Setup Required

### TAP_GITHUB_TOKEN Secret

The workflow needs a Personal Access Token to push to the tap repo.

1. Create a PAT at https://github.com/settings/tokens
   - Select "Fine-grained tokens"
   - Repository access: Select `WilliamAGH/homebrew-tap`
   - Permissions: Contents (Read and write)

2. Add the token as a secret in the brief repo:
   - Go to Settings → Secrets and variables → Actions
   - Create secret named `TAP_GITHUB_TOKEN`

## Commands Reference

### Creating a Release

```bash
# Test the release build first
make release-test V=0.1.0

# If it looks good, push the release
make release V=0.1.0
```

### User Commands

```bash
# Install stable
brew install williamagh/tap/brief

# Install nightly
brew install --head williamagh/tap/brief

# Upgrade stable
brew upgrade brief

# Upgrade nightly
brew upgrade --fetch-HEAD brief
```

### Testing Locally

**1. Test the build (in `brief` repo):**

```bash
make build    # Build and install locally
make run      # Run the app
make dist     # Build distribution zip
```

**2. Test a release before pushing:**

```bash
make release-test V=0.1.0
# Shows: zip path, SHA256, and next command
```

**3. Test the formula (in `homebrew-tap` repo):**

```bash
cd ~/Developer/git/williamagh-homebrew-tap

brew audit --strict Formula/brief.rb      # Check for issues
brew install --HEAD Formula/brief.rb      # Test head install
brew uninstall brief                       # Cleanup
```

## Versioning

- Tags must match `v*` pattern (e.g., `v0.1.0`)
- The workflow strips the `v` prefix for the version number
- Development builds use `0.0.1-SNAPSHOT`

## Troubleshooting

**Workflow fails at "Commit and push to tap":**
Check that `TAP_GITHUB_TOKEN` secret is set with correct permissions.

**SHA256 mismatch for users:**
The release zip may have been modified. Re-run the release or manually update the formula in homebrew-tap.

**Head install fails:**
Ensure `./gradlew` builds successfully locally with `./gradlew installDist`.
