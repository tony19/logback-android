# Design specs

One directory per feature that needs deciding before it is built:
`docs/design/<feature>/SPEC.md`, starting from [`TEMPLATE.md`](TEMPLATE.md).

A spec is written — and merged — before the implementation. Once merged it is the requirement
for the milestone that builds it; the milestone line in [`docs/PLAN.md`](../PLAN.md) is a
summary of it, not a replacement. Its "Decisions already made" section exists so those
decisions are not re-litigated during implementation. Questions the design left open go in its
"Open questions" section and in the design PR body, and are answered before anyone builds past
them.

Bug fixes and small, self-contained changes don't need a spec. New appenders, new
configuration syntax, public API changes, and anything that diverges from upstream logback do.
