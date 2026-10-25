# Workspace Rules

## Git, Branching & Pull Requests
- **`dev` is the base integration branch**: All new work must branch from `dev` and target `dev`.
- **Always use feature branches**: Create a descriptive branch from `dev` (e.g., `feat/<topic>`, `fix/<topic>`, `chore/<topic>`) for all code, configuration, or documentation changes.
- **No direct pushes to `dev` or `main`**: Never commit or push directly to `dev` or `main`.
- **Pull Requests required for `dev`**: All changes must be merged into `dev` via a GitHub Pull Request (`gh pr create --base dev`) after passing the required `Unit Tests` status check.
- **Never touch the `main` branch**: Do not checkout, commit, merge, rebase, push, or open PRs targeting `main` without explicit user approval.
