# ExecPlan standard

Use an ExecPlan for work that crosses system layers, changes architecture or
security, contains important uncertainty, or needs multiple verifiable
milestones. Active plans live in `.agent/plans/active/`; move a completed plan
to `.agent/plans/completed/` without rewriting its history.

An ExecPlan is a living, self-contained handoff. Keep it concise and update it
as evidence changes. A new contributor should be able to continue using only
the plan and the repository files it links.

## Required sections

1. **Outcome and boundaries** — intended result, in-scope questions, and
   explicit exclusions.
2. **Inputs and constraints** — task acceptance criteria, governing project
   documents, and assumptions that must not be treated as facts.
3. **Decision questions and evidence** — each question, primary sources to
   consult, evidence to capture, and an objective acceptance rule.
4. **Milestones** — ordered, independently verifiable work with an observable
   completion condition.
5. **Validation** — commands or review checks and the expected result.
6. **Risks and escalation** — likely failure modes and choices requiring owner
   approval.
7. **Progress and findings** — current checklist, blockers, and concise links or
   notes sufficient to reproduce conclusions.
8. **Decision and requirement updates** — records that must be changed when a
   conclusion is accepted.

## Execution rules

- Prefer current first-party specifications, compatibility matrices, product
  documentation, pricing/limit pages, and licensing terms. Record document URL
  and access date; use release/version-specific material where available.
- Distinguish documented facts, measurements, and inferences. Do not convert a
  proposal into an accepted choice without meeting its acceptance rule.
- Record durable accepted choices in `.agent/DECISIONS.md`; update BRD/SRS when
  they conflict with an accepted choice. Ask the product owner before changing
  cost, latency, provider, platform, or security commitments.
- Keep `.agent/TASKS.md` and `.agent/STATUS.md` aligned with actual progress.
- Do not mark a plan complete while required evidence, approval, or validation
  is missing. State residual uncertainty explicitly.
