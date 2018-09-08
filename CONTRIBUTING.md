# Guidelines for Pull Request

All we love is only Pull Request so we have some rules used to make your PR lovely for reviewers.

A pull request MUST:

- pass all tests
- pass checkstyle (enter "gradle spotlessApply" to auto-apply checkstyle fix)
- have a name starting with "OHARA-XXX" or "Minor:"

It SHOULD:

- be as small in scope as possible. Please don't file a PR with 10000000 changes. No one can do a great review for large PR.
- add new tests

It SHOULD NOT:

- bring in new libraries (or updating libraries to new version) without discussion first
- bring in new module without discussion first
- bring in new APIs for Configurator without discussion first