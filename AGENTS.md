# Agent instructions for logback-android

Project facts and review focus areas are in
[`.github/copilot-instructions.md`](.github/copilot-instructions.md).

## Pull requests

- Open every pull request as a **draft**, and keep it a draft until the change
  is complete and ready for a human to review. Only then mark it ready for
  review.
- CI (Build, Static Analysis, CodeQL) does not run on draft PRs. Marking the PR
  ready for review starts it.
- To run CI on a draft (e.g. to verify a fix before it's ready for review), add
  the `run-ci-on-drafts` label. CI then runs on every push to that draft; remove
  the label once it's no longer needed.
- PR titles must follow [Conventional Commits](https://www.conventionalcommits.org/)
  (e.g. `fix: ...`, `ci: ...`, `docs: ...`); this is enforced by the Semantic
  Pull Request check.

## Skills

- Before writing code for a fix or feature, follow
  [`.claude/skills/task-start/SKILL.md`](.claude/skills/task-start/SKILL.md): it
  checks open issues, upstream logback, and both build flavors first.
