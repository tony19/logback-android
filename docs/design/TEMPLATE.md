# <Feature> — design

Status: draft | accepted | implemented (M<x.y>, #<PR>)
Requirements served: R<n>, …
Milestone: M<x.y>

## 1. Problem

What a user can't do today, or what goes wrong. Link the issues.

## 2. Upstream

How upstream logback handles this, if at all, and whether to port that or diverge (and why).

## 3. Design

Public API and `logback.xml` syntax (with the `logback.xsd` change), behavior, threading, and
failure handling (what status message, never an exception into the app).

## 4. Compatibility

Both artifacts (Java 8 / minSdk 21 and Java 11 / minSdk 26), existing configurations, R8
keep rules, and whether anything is a breaking change.

## 5. Decisions already made

Numbered, each with its reason. Not re-opened during implementation without the owner.

## 6. Open questions

Each with who owes the answer. Resolve before building past it.

## 7. Tests

What proves it, by test class name, so the milestone's traceability row in `docs/PLAN.md` §4
can cite them.
