---
name: architecture-sync
description: Read this WHEN A CHANGE ALTERS THE SHAPE OF THE LIBRARY — adding, removing, renaming or moving a Java or Kotlin package under logback-android/src/main/java or src/main/kotlin; moving a responsibility between packages; adding an android.* import to a new package or any core→classic import; adding a published output (artifact, flavor, the XSD, consumer R8 rules) or a runtime dependency; or changing how configuration is discovered or a log call is dispatched. Also read it before opening the PR for such a change, and when asked to check whether docs/architecture is still accurate.
---

# Keeping the architecture docs true

`docs/architecture/` is the map an agent uses to find anything in this repository. Its failure
mode is not being incomplete — it is being confidently wrong, which sends the next reader to a
class that no longer does what the map says it does, and costs them more than no map would
have.

So the rule is: **the map changes in the same commit as the territory.** A follow-up commit is
a commit that does not happen.

## 1. Decide whether this change is architectural

It is, if any of these is true:

- a Java package is **added, removed, renamed or moved**;
- a **dependency edge** changes: `core` importing `classic`, `android.*` imported from a
  package that did not before, a new runtime dependency in `logback-android/build.gradle`;
- a **published output** appears, disappears or changes: an artifact or flavor, a
  bytecode/minSdk floor, `logback.xsd`'s namespace or location, `consumer-rules.pro`'s scope;
- a **responsibility moves** between packages — e.g. property resolution leaving
  `core/android`, or a converter moving between `core` and `classic`;
- **startup or dispatch** changes: how `logback.xml` is found, what happens with no
  configuration, or the order of filters/appenders on a log call.

It is not, if the change is a new class, appender, converter or Joran action inside an
existing package. That is the library doing its job — though a new `logback.xml` element still
needs `logback.xsd`.

Two of these deserve a pause rather than a doc edit: `core` depending on `classic`, and a new
runtime dependency. Both invert something `docs/architecture/README.md` §4 says is fixed. That
is a design question, not a documentation question — ask before writing it down.

## 2. Update the catalogue

`docs/architecture/components.md` has one heading per package, shaped:

```
### `ch/qos/logback/core/rolling` — `ch.qos.logback.core.rolling`
```

Add, remove or rename the entry to match. The backticked directory path (relative to
`logback-android/src/main/java` or `logback-android/src/main/kotlin`) is what the gate matches on, so it has to be exact. Mark
Android-only packages as such.

Then read the entries **either side of the change**, not just the one you added. A
responsibility that moved leaves the old entry describing something it no longer does, and
that half of the edit is the one that gets skipped.

## 3. Update the map, where the change is visible from it

`docs/architecture/README.md` — only the sections the change actually reaches:

- §2 What gets published — an artifact, flavor, floor, the XSD, or R8 rules changed.
- §3 The shape of a log call — startup/configuration discovery, dispatch, or failure handling.
- §4 Dependency direction — a new edge, or a package added to the `android.*` allow-list
  (update `ANDROID_PACKAGES` in `scripts/check-architecture-docs.sh` in the same commit).
- §5 Where a change goes — a new place changes are made, or a row now pointing at the wrong
  package.

Leave the rest alone. A diff that rewrites the whole file to add one package is a diff nobody
reviews.

If the change also moves a requirement or a milestone, update `docs/PLAN.md` §3/§4 too.

## 4. Run the gate

```
./scripts/check-architecture-docs.sh
```

It fails if a package has no entry, an entry names a package that is gone, `core` imports
`classic`, or `android.*` is imported outside the allow-list. CI runs it in the Static Analysis
workflow.

It cannot check whether the prose is still true. That is the review, and it is why you read the
neighbouring entries in step 2.

## Then open the PR

Say in the PR body what shape changed and which architecture docs moved with it. One line. It
is what tells a reviewer the map was considered rather than missed.
