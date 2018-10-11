# Guidelines for Pull Request

All we love is only Pull Request so we have some rules used to make your PR lovely for reviewers.

## pull request command
* retry: trigger QA.
* run: make jenkins execute a manager and a configurator. The UI link will be attached to PR if jenkins receive the command successfully.

# A pull request MUST:

## pass all tests
Any PR making OHARA unstable should be reverted ASAP. A software engineer who can't stabilize tests should be fired ASAP. 


## pass checkstyle (enter "gradle spotlessApply" to auto-apply checkstyle fix)
Don't make you code style same with your bad-looking face

## have a name starting with "OHARA-XXX" or "Minor:"
If your PR has only a bit changes, naming you PR with "Minor:" is ok. Otherwise, file a JIRA first.

## address all reviewers' comments
Please don't be an asshole.

# A pull request SHOULD:

## be as small in scope as possible.
Please don't file a PR with 10000000 changes. No one can do a great review for large PR.

## add new tests
Please add some tests to prove your are a valid software engineer.

# A pull request SHOULD NOT:

## bring in new libraries (or updating libraries to new version) without discussion first
Updating the dependencies unless you have a good reason.

## bring in new module without discussion first

## bring in new APIs for Configurator without discussion first

