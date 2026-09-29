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

## Plan and architecture

- [`docs/PLAN.md`](docs/PLAN.md) is the plan: constraints, milestones, and requirements
  traceability. It is not the whole backlog — open issues and `docs/design/*/SPEC.md` are too.
- [`docs/architecture/`](docs/architecture/README.md) is the map of the code. It changes in the
  same commit as a change to the library's shape; `./scripts/check-architecture-docs.sh` is the
  gate.

## Skills

- Before writing code for a milestone, fix or feature, follow
  [`.claude/skills/phase-start/SKILL.md`](.claude/skills/phase-start/SKILL.md).
- When a change alters the library's shape (packages, dependency direction, published outputs),
  follow [`.claude/skills/architecture-sync/SKILL.md`](.claude/skills/architecture-sync/SKILL.md).
