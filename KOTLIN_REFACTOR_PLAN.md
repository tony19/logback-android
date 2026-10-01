# Kotlin Refactor — Implementation Plan

**Status:** living document · **Last updated:** 2026-07-07
**Tracks:** the modernization effort started in [PR #388](https://github.com/tony19/logback-android/pull/388)
(branch `claude/kotlin-android-refactor-f8cwzg`)

This document describes where the Kotlin refactor stands today, the strategic
decisions that shape the remaining work, a phased roadmap for completing it,
and the per-file conversion rules that keep the library binary- and
behavior-compatible while it happens.

---

## 1. Where we are today

### Codebase inventory (as of `main` @ `ea08778` / PR #388 @ `1cfe2b0`)

| Category | Files | Notes |
|---|---|---|
| Main sources, Java | **403** | `ch.qos.logback.core` (271), `ch.qos.logback.classic` (127), `org.slf4j.impl` (5) |
| Main sources, Kotlin | **8** | The Android-specific layer, converted in PR #388 |
| Test sources, Java | **382** | 993 tests, all still Java (intentionally — see §3.4) |

### What PR #388 already delivered

1. **Build modernization** — Kotlin DSL + version catalog (`gradle/libs.versions.toml`),
   AGP 8.7.3, Gradle 8.14.3, Kotlin **2.1.21 with `explicitApi()` strict mode**,
   compileSdk 35, minSdk 21, Java 17 bytecode.
2. **Android layer in Kotlin** — `LogcatAppender`, `SQLiteAppender`,
   `BasicLogcatConfigurator`, `SQLiteLogCleaner`, `Clock`, `SystemClock`,
   `AndroidContextUtil`, `SystemPropertiesProxy` now live under
   `src/main/kotlin`, with Java/XML-config interop preserved
   (`@JvmStatic`, `@JvmOverloads`, `@JvmName`, `fun interface`).
3. **CI + test modernization** — a new GitHub Actions build workflow
   (assemble + unit tests + lint), CircleCI on JDK 17, Mockito 5,
   Robolectric 4.14.1, JDK 20+ test fixes, de-flaked socket-appender and
   archive-removal suites, a reusable `RetryRule` test utility.

### What is still open on PR #388

| Item | State | Blocked on |
|---|---|---|
| GitHub Actions `Build` | ✅ green on `1cfe2b0` | — |
| CircleCI `build` / `lint` | ✅ green (as of `aa12207`) | — |
| CircleCI `test` | ❌ red; logs unreachable from the dev sandbox | Maintainer pasting failing test names from the CircleCI UI (asked in PR comment) — 4 follow-up commits (`6124eee`…`1cfe2b0`) target the suspected causes but are unconfirmed |
| CircleCI `test_app` | ❌ red | External repo `tony19-sandbox/logback-test-app` must move to AGP 7.4+/JDK 17 to consume Java-17 bytecode — change lives outside this repo |
| Codacy | `action_required` | Maintainer approval in the Codacy UI |
| PR state | Draft | All of the above + review |

---

## 2. The one strategic decision that shapes everything

PR #388 deliberately stopped at the Android layer and kept the
upstream-derived logback core/classic port (~400 files) in Java, "so future
diffs against upstream logback remain reviewable." Continuing the refactor
means deciding, explicitly, how far Kotlin goes:

**Option A — stop here.** Android layer is Kotlin, core stays Java.
Cheapest; preserves upstream diffability. But the project remains 98% Java,
so "Kotlin refactor" is mostly branding, and every future contribution
straddles two languages with two idioms.

**Option B — convert everything (recommended).** Treat 3.x as the fork
point and convert `core`, `classic`, and `org.slf4j.impl` package-by-package.
Rationale: logback-android tracks logback **1.2.x**, which upstream has
EOL'd in favor of the 1.3/1.4/1.5 line (Java 8+/11+, Jakarta, substantially
restructured). Practical line-by-line merges from upstream were already
unrealistic before this refactor; giving up textual diffability costs little,
and a single-language codebase is simpler to maintain, null-safe, and far
friendlier to the Android contributor base.

**Option C — hybrid.** Convert only the packages where logback-android has
already diverged hardest from upstream (rolling/appenders/net) and freeze the
rest. Worst of both worlds operationally (two idioms forever, and the
"diffable" remainder is the part least likely to need upstream merges).
Not recommended.

> **Action:** get the maintainer's sign-off on Option B before starting
> Phase 2 below. Everything in Phases 0–1 is worth doing under any option.

---

## 3. Guiding strategies

These are the rules that make a 400-file conversion safe. They are ordered
by how much pain they prevent.

### 3.1 Behavior preservation is the prime directive

- A conversion commit changes **language, not behavior**. Bug fixes and
  modernizations discovered during conversion go in **separate commits**
  (as PR #388 did with the `SQLiteAppender.stop()` NPE and the
  `PackageInfo.versionCode` deprecation), ideally landed *before* the
  conversion of that file so the Java test suite validates them first.
- Preserve Java null-tolerance. logback swallows nulls all over
  (`OptionHelper`, pattern converters, appender attach/detach). Converting a
  silently-tolerated null into a `NullPointerException` from a non-null
  Kotlin parameter is the #1 way this refactor breaks user apps at runtime.
  Default to **nullable types at every public boundary** and only tighten
  where the Java code already threw or documented non-null. No `!!`, no
  `lateinit` on fields Joran sets reflectively.

### 3.2 Small, stacked, always-green PRs

- One PR per package batch (§4, Phase 2): roughly **15–40 files / ≤1.5k
  changed lines** each. A single mega-PR is unreviewable and unbisectable.
- Every PR: full unit test run on **both variants** (debug + release), lint,
  and the binary-compat check (§3.3) must be green before merge.
- **Two-commit convention per batch:** commit 1 is the mechanical
  conversion (J2K output, minimally corrected, same structure); commit 2 is
  idiomatization (properties, `when`, `use`, scope functions, collection
  operators). Reviewers can verify commit 1 is semantics-preserving almost
  line-by-line, then judge commit 2 on taste. Squash-merge if the repo
  prefers, but keep the split during review.
- Git does not detect `.java → .kt` as a rename. Note the source file's
  final Java SHA in the commit message so archaeology stays possible.

### 3.3 Guardrails before conversion (Phase 1 deliverables)

- **API baseline + gate:** add JetBrains'
  [`binary-compatibility-validator`](https://github.com/Kotlin/binary-compatibility-validator)
  (works on the JVM class output regardless of source language). Dump the
  baseline `.api` file from the current jar *before* Phase 2 starts; every
  batch PR runs `apiCheck`. Any diff to the dump is reviewed as a
  deliberate API decision, not an accident. (`explicitApi()` strict mode,
  already enabled, complements this on the Kotlin side.)
- **A/B regression harness:** PR #388 validated itself by running the
  identical compile+test harness against unmodified `main` and comparing
  (zero-regression rule). Commit that harness as a script (e.g.
  `scripts/ab-test.sh`) so every batch can run
  `full suite @ HEAD` vs `full suite @ merge-base` mechanically.
- **Style gate:** ktlint (or detekt with formatting rules) wired into the
  existing lint CI job, so idiomatization debates are settled by tooling.
- **Conversion checklist** (§5) checked into the repo; every batch PR
  description links it and confirms each item.

### 3.4 Tests stay Java (for now) — they are the interop oracle

The 382 Java test files are the best interop verification we have: as long
as they compile and pass against converted sources, Java consumers of the
library keep working. Converting tests simultaneously with sources would
destroy that signal. Convert tests **last** (Phase 4), or leave them Java
indefinitely — there is no user-facing benefit to converting them.

### 3.5 Bottom-up by dependency, riskiest packages last

Convert leaf/utility packages first and the reflection- and
concurrency-heavy heart (`joran`, `net`, `classic` root) last, so that:
(a) early batches build team/reviewer confidence and calibrate the
checklist; (b) by the time the hard packages convert, everything they
depend on is already Kotlin, minimizing platform-type churn; (c) the
highest-risk interop surfaces get the most-rehearsed process.

---

## 4. Phased roadmap

### Phase 0 — Land PR #388 (in progress, partially blocked)

1. Maintainer: paste CircleCI `test` failures into the PR (or grant log
   access). Fix whatever the 4 speculative de-flake commits didn't catch.
   - *Fallback if CircleCI stays opaque:* since the identical suite is green
     on GitHub Actions, consider making GH Actions the required check and
     demoting/retiring the CircleCI `test` job — running the same suite on
     two CI providers has little marginal value and doubles the flake
     surface.
2. Maintainer: update `tony19-sandbox/logback-test-app` toolchain
   (AGP 7.4+/JDK 17) so `test_app` can consume Java-17 bytecode, or pin
   `test_app` to a compatible branch of that app.
3. Maintainer: clear the Codacy `action_required` gate.
4. Un-draft, review, merge.
5. **Release a `3.1.0-alpha`** (or similar) from the merged state so the
   toolchain/minSdk changes get real-world exposure *before* the core
   conversion starts. minSdk 9→21 and Java 17 bytecode are consumer-visible;
   better to hear about it now than mixed into 400 converted files.

### Phase 1 — Guardrails (1 small PR, no user-visible change)

- Add `binary-compatibility-validator` + committed API baseline + CI
  `apiCheck` job.
- Commit the A/B harness script.
- Add ktlint/detekt to the lint job.
- Commit this plan + the §5 checklist.

### Phase 2 — Core conversion, package-by-package (~12–14 PRs)

Each batch = one PR, following §3.2/§3.3/§5. File counts are current main
counts. Order within tiers can flex; tiers should land in order.

| # | Batch | Files | Key risks to watch |
|---|---|---|---|
| 1 | `core/util` | 26 | Heavy statics (`OptionHelper`, `Loader`, `CachingDateFormatter`) → `object` + `@JvmStatic`; classloader tricks in `Loader` |
| 2 | Small `core` leaves: `helpers`, `hook`, `property`, `boolex`, `filter`, `encoder`, `html`, `read`, `recovery`, `subst` | ~31 | `subst` is the config-variable parser — subtle tokenizer semantics; `boolex` uses runtime compilation hooks |
| 3 | `core/status`, `core/spi` | 29 | `ContextAwareBase`/`ContextAware` are the highest fan-in types in the codebase; `FilterReply`, `LifeCycle` widely implemented from Java tests |
| 4 | `core/pattern` | 27 | Converter reflection: instantiated by class name from parser maps — keep exact FQCNs and public no-arg constructors |
| 5 | `core` root | 17 | `AsyncAppenderBase` (worker thread, `wait/notify`-adjacent, interrupt semantics — recently patched for JDK 19+; convert with extra care), `AppenderBase`/`UnsynchronizedAppenderBase` (synchronization semantics), `CoreConstants` (`const val` + `@JvmField` decisions) |
| 6 | `core/rolling` | 36 | Time-based triggering is timing-sensitive in tests (already flaky-prone); `FileNamePattern` interplay with `pattern` |
| 7 | `core/net` | 44 | **Java serialization**: `*VO` classes and `HardenedObjectInputStream` — keep `serialVersionUID`, `readObject`/`writeObject` exact (§5); SSL config beans are Joran-reflected |
| 8 | `core/joran` + `core/sift` | 61 | **The reflection engine itself** (`PropertySetter`, bean introspection, `valueOf` conversion, adder methods). The good news: once converted, it can be *taught* about any Kotlin quirks; until then, converted classes must look like Java beans to it |
| 9 | `classic/spi`, `classic/util` | 25 | `LoggingEvent`/`ThrowableProxy` are the hot allocation path — no convenience allocations (see §6 performance) |
| 10 | `classic/pattern`, `classic/html` | 33 | Same converter-by-FQCN constraints as batch 4 |
| 11 | `classic/joran`, `classic/turbo`, `classic/db`, `classic/sift`, `classic/selector`, `classic/filter`, `classic/boolex`, `classic/log4j`, `classic/layout`, `classic/encoder` | ~41 | `classic/joran` action classes are reflectively registered |
| 12 | `classic/net` | 20 | Serialization again (`LoggingEventVO` receiver side), SMTP/SSL beans |
| 13 | `classic` root | 7 | `Logger`, `LoggerContext`, `PatternLayout` — hottest path in the library, highest fan-in from user code. Convert only when everything else is proven |
| 14 | `org.slf4j.impl` | 5 | SLF4J discovers these **reflectively by exact FQCN** (`StaticLoggerBinder` for 1.7 clients, `LoggerServiceProvider` via `ServiceLoader` for 2.x). Class names, static members (`getSingleton()`), and `REQUESTED_API_VERSION` field must survive byte-identical in shape |

After each tier (not just each batch), run the A/B harness against the
pre-tier baseline and cut an internal `-alpha` if feasible.

### Phase 3 — Idiomatization + API polish (1–2 PRs)

- Second-pass idiom sweep now that platform types are gone end-to-end:
  tighten nullability where the call graph proves non-null, replace
  builder-ish Java patterns with named/default arguments **behind
  `@JvmOverloads`** (the API dump keeps this honest).
- Review the `.api` dump holistically; decide what (if anything) is worth a
  deliberate breaking change, and batch those into…

### Phase 4 — Release + optional test conversion

- **Release 4.0.0.** minSdk 9→21 + Java 17 bytecode already justify a major
  bump regardless of API changes. Publish migration notes (consumer AGP/JDK
  requirements, any Joran config edge cases found).
- Optionally begin converting tests (JUnit 4 → JUnit 5 or kotlin.test could
  ride along), lowest priority, opportunistic.

---

## 5. Per-file conversion checklist

Every converted file must be checked against this list (batch PRs confirm it):

**Joran / XML-config reflection compatibility** (applies to anything
configurable from `logback.xml` — appenders, encoders, layouts, policies,
filters, actions):
- [ ] Every configurable property compiles to the exact `getX()`/`setX(T)`
      (or `isX()` for booleans) the Java class had. Kotlin `var x: T` does
      this; verify primitives stay primitives (`Int` ≠ `Integer` in
      signatures) and watch `is`-prefixed property names.
- [ ] "Adder" methods (`addAppender`, `addFilter`, …) keep their exact names
      — do not property-ize them.
- [ ] Public **no-arg constructor** preserved (Joran instantiates by
      `Class.newInstance`). Default-arg constructors need `@JvmOverloads`
      only if they replace an explicit no-arg one.
- [ ] Class keeps its exact FQCN (converter maps, action registries, and
      user configs reference classes by string name).

**Java interop surface:**
- [ ] Statics: `companion object` + `@JvmStatic` (methods) / `@JvmField` or
      `const` (fields) so Java call sites (including tests) compile
      unchanged.
- [ ] Java package-private members used by same-package tests: make
      `internal` and, where Kotlin name-mangles, pin with `@JvmName`
      (PR #388's `setClock` hook is the exemplar).
- [ ] Single-method interfaces implemented anonymously from Java → declare
      as `fun interface` (SAM), which keeps both Java anonymous classes and
      Kotlin lambdas working.
- [ ] Checked exceptions declared in the Java signature → `@Throws(...)`.
- [ ] Overridable classes/members: Kotlin is `final` by default; add `open`
      wherever the Java class was subclassed (in-repo, in tests, or
      plausibly by users — appender/layout/converter bases are all
      user-extensible and must stay `open`).

**Serialization (`core/net`, `classic/net`, `classic/spi` VOs):**
- [ ] `serialVersionUID` preserved: `companion object { private const val
      serialVersionUID: Long = <same value> }`.
- [ ] `readObject`/`writeObject`/`readResolve` keep exact private
      signatures (`private fun readObject(in: ObjectInputStream)` with
      `@Throws`).
- [ ] Field order/types unchanged for default serialized form.

**Concurrency:**
- [ ] `synchronized` methods → `@Synchronized`; `synchronized(obj)` blocks →
      `synchronized(obj) { }` (same monitor object!).
- [ ] `volatile` → `@Volatile`.
- [ ] `wait/notify` on `this` does not port cleanly to Kotlin — refactor to
      an explicit lock object *in a separate, pre-conversion Java commit*
      (or convert mechanically via a dedicated `Object` monitor field).
- [ ] Interrupt handling preserved exactly (`AsyncAppenderBase` has
      documented JDK-19+ sensitivities).

**Nullability policy:**
- [ ] Public parameters/returns default to nullable unless Java provably
      rejected null; internal call chains may tighten.
- [ ] No `!!` in converted code; no `lateinit` on Joran-injected properties
      (use nullable with the same lazy/guard behavior the Java had).

**Hygiene:**
- [ ] `explicitApi()` satisfied without weakening visibility (things that
      were package-private don't become `public` for convenience).
- [ ] Header/license comment carried over; KDoc from Javadoc.
- [ ] Commit message records the final Java-side SHA of the replaced file.

---

## 6. Risk register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Joran reflection silently stops setting a property (config parses, value ignored) | Medium | High — user configs misbehave without errors | Checklist §5; keep Java tests; add a Joran round-trip test per converted appender/policy that asserts *every* XML attribute lands on the bean |
| NPE from over-tightened nullability | Medium | High — runtime crash in user apps | §3.1 policy; A/B harness; alpha releases per tier |
| Serialization incompat breaks `SocketAppender` streams between old/new versions | Low | Medium | §5 serialization items; add a golden-bytes test (serialize with old jar, deserialize with new) |
| Hot-path performance regression (`Logger`/`LoggingEvent`: hidden boxing, `List` where array was, defensive copies from idiomatization) | Medium | Medium | Convert hot path last (batches 9, 13); commit-2 idiomatization reviewed with allocations in mind; simple JMH or timing smoke on `Logger.debug` throughput before/after |
| Binary API drift breaks downstream Java consumers | Medium | High | `binary-compatibility-validator` gate (Phase 1) — turns drift from silent to reviewed |
| CircleCI `test`/`test_app` stay opaque and red, stalling Phase 0 | High (currently real) | Medium — blocks everything | Fallback in Phase 0: consolidate on GitHub Actions as the required check |
| Flaky timing tests erode trust in per-batch green-CI rule | Medium | Medium | `RetryRule` pattern from PR #388; keep de-flaking as its own commits |
| Kotlin/AGP toolchain churn mid-refactor | Low | Low | Version catalog pins everything; Renovate PRs held during Phase 2 or merged only between batches |
| Losing upstream logback diffability | Certain (Option B) | Low — upstream 1.2.x is EOL; merges were already impractical | Explicit maintainer sign-off (§2); commit messages record replaced-file SHAs |

---

## 7. Working agreements & logistics

- **Branch naming:** one branch per batch, `kotlin/<batch-name>` (e.g.
  `kotlin/core-util`), PR'd against `main` after PR #388 merges. Do not
  stack more than one unmerged batch — the API dump and A/B baseline both
  assume a merged predecessor.
- **Definition of done per batch:** both-variant unit tests green on GH
  Actions, lint green, `apiCheck` green, A/B harness zero-regression vs
  merge-base, checklist confirmed in the PR description.
- **When conversion reveals a bug:** fix in a separate Java-side commit/PR
  first (tests prove the fix), then convert. Never fold fixes into the
  conversion diff.
- **When a file resists clean conversion** (e.g. `wait/notify`, gnarly
  package-private webs): restructure it *in Java* first as its own reviewed
  commit, then convert the restructured version. Two easy reviews beat one
  impossible one.
- **Maintainer touchpoints:** sign-off on Option B (§2) before Phase 2;
  review of the `.api` baseline (Phase 1); alpha releases per tier; the
  Phase 4 major-version release notes.

## 8. Open questions for the maintainer

1. **Scope sign-off:** proceed with Option B (full conversion) after
   PR #388 lands? (§2)
2. **CircleCI's future:** keep fixing the `test` job, or make GitHub Actions
   the canonical CI and retire the duplicated CircleCI suite? (Phase 0)
3. **`test_app`:** will `tony19-sandbox/logback-test-app` be updated to
   AGP 7.4+/JDK 17, and should its build be a required check during
   Phase 2?
4. **Versioning:** agree 4.0.0 as the landing version for the completed
   refactor, with `3.1.0-alpha`-style pre-releases along the way? (Phases
   0/4)
5. **Test conversion:** any appetite for converting the 382 Java tests, or
   keep them permanently as the interop oracle? (§3.4)
