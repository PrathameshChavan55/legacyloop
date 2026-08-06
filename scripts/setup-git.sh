#!/usr/bin/env bash
# First push. Run once, from the project root, after creating an empty repo on GitHub.
#
#   ./scripts/setup-git.sh https://github.com/<user>/legacyloop.git
set -euo pipefail

REMOTE="${1:?usage: setup-git.sh <repo-url>}"

git init
git add .
git commit -m "chore: project skeleton — build, shared code, module stubs"
git branch -M main
git remote add origin "$REMOTE"
git push -u origin main

# develop is where everyone's feature branches merge; main only gets demo-ready code
git checkout -b develop
git push -u origin develop

echo
echo "Done. Now, on GitHub:"
echo "  Settings > Branches > add a rule for main and develop:"
echo "    - require a pull request before merging"
echo "    - require 1 approval"
echo "    - do not allow force pushes"
echo "  Settings > Collaborators > invite the other five members"
