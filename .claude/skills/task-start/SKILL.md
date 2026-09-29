---
name: task-start
description: Read this BEFORE writing code for a fix or feature in logback-android — "fix issue #N", "implement X", "add support for Y", or picking up any change to the library under logback-android/. Gathers the deferred work and constraints that the request itself does not mention — open GitHub issues touching the same code, upstream logback behavior, and the JDK 8 / minSdk 21 build — so a change does not ship having silently skipped a known defect or broken the jdk8 artifact.
---

# Before implementing a change

The request is what to build. It is **not** the whole backlog, and it does not list the
constraints.

Known defects live in open issues, the reference behavior lives upstream in logback, and the
compatibility floor lives in the Gradle build. None of that is visible from a one-line task.
A change that reads only the request will re-touch a file that has a known open defect in it
and leave the defect there — which is how a small fix becomes a stale one.

Do this before writing code, not after.

## 0. Get oriented

If you have not read `.github/copilot-instructions.md` in this session, read it now. It has
the project facts (module layout, test locations) and the review focus areas — thread safety,
resource handling, hot-path performance, public API compatibility — that every step below
assumes.

## 1. Read the open issues

```
mcp__github__list_issues  owner: tony19  repo: logback-android  state: OPEN
```

For each one, decide and say which:

- **In scope** — the change already touches that class, appender, or subsystem. Fold the fix
  in and close the issue from the PR (`Fixes #N` in the body). A change that edits
  `RollingFileAppender` and leaves an open rollover bug in `RollingFileAppender` has made the
  problem older, not smaller.
- **Adjacent** — near the work but a different change (different appender, different risk,
  its own tests). Say so in the PR body and leave the issue open. Do not widen the change on
  your own judgement; ask if it looks like it should move.
- **Unrelated** — leave it, silently.

Read the body and comments, not just the title. Labels such as `need reproduction` or
`pending triage` mean the report is not yet confirmed — reproduce it with a test before
"fixing" it. Comments are also where an earlier discussion parked a decision or a rejected
approach; that is usually what tells you whether this change is the right home for it.

## 2. Check upstream logback

Much of `logback-android/src/main/java` is a port of [logback](https://github.com/qos-ch/logback).
Before changing a ported class, look at the same class upstream:

- If upstream already fixed the bug, port that fix (same shape, same names) rather than
  writing a new one — it keeps the port easy to sync.
- If the change would diverge from upstream behavior on purpose (an Android constraint, a
  removed desktop-only API), say so in a code comment and in the PR body.

## 3. Check both build flavors

The library ships two artifacts from one source tree (`logback-android/build.gradle`):

| Flavor | Artifact | Bytecode | minSdk |
|---|---|---|---|
| `jdk11` | `logback-android` | Java 11 | 26 |
| `jdk8` | `logback-android-jdk8` | Java 8 | 21 |

Code must compile and behave on the **lower** floor: no Java 9+ language features or library
APIs, and no Android APIs above 21 without an `SDK_INT` guard. Plan the tests for both —
CI runs `testJdk11DebugUnitTest` and `testJdk8DebugUnitTest` separately.

If the change touches public API or `logback.xml` configuration, check `logback.xsd` and the
README/wiki examples too; a new config element that the schema rejects is a bug.

## Then start

Write the failing test first where the change is a bug fix. Before pushing, run what CI runs:

```
./gradlew lint testJdk11DebugUnitTest testJdk8DebugUnitTest
```

Open the PR as a draft (see `AGENTS.md`) with a Conventional Commits title and a short
"deferred work considered" note naming the issues you folded in and the ones you deliberately
left. Two lines. It is the record that the check happened, and it is what lets the next change
trust that the issues still open are still open on purpose.
