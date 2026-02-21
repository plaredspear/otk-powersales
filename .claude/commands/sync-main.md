---
description: Sync current branch with local main branch (rebase with conflict handling)
runOn: project
---

# Git Sync with Main Branch

**IMPORTANT**: This command synchronizes the current working branch with the local `main` branch.

## Workflow

Execute the following steps in order:

### 1. Attempt to rebase on main
```bash
git rebase main
```

### 2. Handle rebase result

**If rebase succeeds:**
- Display success message
- Show current branch status
- Continue with work

**If rebase fails (conflicts detected):**
- **DO NOT attempt to resolve conflicts automatically**
- Immediately execute: `git rebase --abort`
- **STOP all work and report to user:**

  ```
  ⚠️ REBASE CONFLICT DETECTED - Work Stopped

  Conflicts occurred while rebasing on main.

  Conflicting files:
  [List all conflicting files]

  Reason:
  Your current branch has changes that conflict with main.

  Required action:
  Please resolve conflicts manually before continuing work.

  Steps to resolve:
  1. Run: git rebase main
  2. Resolve conflicts in the listed files
  3. Run: git add <resolved-files>
  4. Run: git rebase --continue
  5. Restart your task after successful rebase
  ```

- Do NOT proceed with any further tasks until user resolves conflicts

## Usage

Run this command:
- At the start of every work session
- Before starting each new Story/Task
- After any PR is merged to main
- Before creating a Pull Request

## Safety Rules

1. **Never auto-resolve conflicts** - Always abort and notify user
2. **Stop all automation** - No commits, no PRs, no file changes after conflict
3. **Clear communication** - Provide detailed conflict information to user
