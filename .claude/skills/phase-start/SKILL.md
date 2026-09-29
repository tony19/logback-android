---
name: phase-start
description: Read this BEFORE writing code for a milestone in docs/PLAN.md §3 — a new phase, the next milestone, "start M1.2", "implement the next phase" — or for any other fix or feature in logback-android ("fix issue #N", "add support for X"). Gathers the deferred work that PLAN.md does not know about — open GitHub issues, design decisions parked in docs/design/*/SPEC.md, and follow-ups recorded in merged PR bodies — plus upstream logback and the jdk8/minSdk 21 floor, so a change does not ship having silently skipped a known defect.
---

# Before implementing a phase

`docs/PLAN.md` is the plan. It is **not** the whole backlog.

Work gets deferred in three other places, and none of them are visible from the milestone
list. A phase that reads only PLAN.md will re-touch a file that has a known open defect in it
and leave the defect there — which is how a small fix becomes a stale one.

The same applies to a one-off fix or feature that is not a milestone: do every step, and in
step 3 find the requirement row it serves instead of a milestone row.

Do this before writing code, not after.

## 0. Get oriented

If you have not read `docs/architecture/README.md` in this session, read it now. It is the
map — what gets published, the path of a log call, the dependency direction, and which package
a given kind of change belongs in — and every step below assumes you can place what you are
reading. `.github/copilot-instructions.md` has the review focus areas (thread safety, resource
handling, hot-path cost, public API compatibility) the work will be held to.

## 1. Read the open issues

```
mcp__github__list_issues  owner: tony19  repo: logback-android  state: OPEN
```

For each one, decide and say which:

- **In scope** — the phase already touches that class or package. Fold the fix in and close
  the issue from the PR (`Fixes #N` in the body). A phase that edits `RollingFileAppender`
  and leaves an open rollover bug in `RollingFileAppender` has made the problem older, not
  smaller.
- **Adjacent** — near the work but a different change (different appender, different risk,
  its own tests). Say so in the PR body and leave the issue open. Do not widen the phase on
  your own judgement; ask if it looks like it should move.
- **Unrelated** — leave it, silently.

Read the body and comments, not just the title: the reasoning for a deferral, or a rejected
approach, is usually what tells you whether this phase is the right home for it. Many open
issues here predate a merged fix in the same area — reproduce the report with a test against
current code before deciding it is fixed or still broken. `pending triage` and
`need reproduction` labels mean the report itself is unconfirmed.

## 2. Read the design spec for what this phase builds

`docs/design/<feature>/SPEC.md` is authoritative for the feature it describes, and its
"Decisions already made" section exists so they are not re-litigated. If the phase implements
a designed feature, the spec is the requirement — the milestone line in PLAN.md is a summary
of it, not a replacement.

Where a design shipped with open questions rather than answers, they are in the spec's
"Open questions" and in the merged PR body for that design. Those are decisions someone still
owes you; surface them before you build past them rather than choosing silently.

If the phase is larger than a fix and has no spec — a new appender, new `logback.xml` syntax,
a public API change, a divergence from upstream — say so and ask whether to write one first
(`docs/design/TEMPLATE.md`).

## 3. Check the traceability row

`docs/PLAN.md` §4 maps requirements to milestones and to the tests that prove them. The row is
what the phase has to satisfy, and it is easier to read before the work than to
reverse-engineer afterwards. If the phase leaves a requirement's proof weaker than it found it,
that is a regression.

## 4. Check upstream and both floors

Most of `logback-android/src/main/java` is a port of
[logback](https://github.com/qos-ch/logback). Before changing a ported class, look at the same
class upstream: if upstream already fixed it, port that fix in the same shape and names; if
this change diverges on purpose, say why in a comment at the site and in the PR body.

Both artifacts build from one tree, so code must hold on the **lower** floor — Java 8 language
and library APIs, Android API 21 (no newer API without an `SDK_INT` guard). CI tests the two
flavors separately.

## Then start

Write the failing test first where the phase fixes a bug. Before pushing, run what CI runs:

```
./gradlew lint testJdk11DebugUnitTest testJdk8DebugUnitTest
./scripts/check-architecture-docs.sh
```

Open the PR as a draft (see `AGENTS.md`), with a Conventional Commits title and a short
"deferred work considered" note naming what you folded in and what you deliberately left. Two
lines. It is the record that the check happened, and it is what lets the next phase trust that
the issues still open are still open on purpose. When the milestone ships, tick it in
`docs/PLAN.md` §3 and its §4 rows in the same PR.

And if the phase changed the shape of the system — a package added, removed or moved, a
responsibility moved, a dependency direction changed, a published output or runtime
dependency added — `docs/architecture/` changes in the same commit.
`.claude/skills/architecture-sync` is the walkthrough, and
`./scripts/check-architecture-docs.sh` is the gate that will find you if it does not.
