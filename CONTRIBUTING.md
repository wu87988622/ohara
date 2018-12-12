# Guidelines for Pull Request

All we love is only Pull Request so we have some rules used to make your PR lovely for reviewers.

## Pull request commands:

These commands are handy when you want to test your PR on our QA(CI server).

To start a QA run, you can simply leave a comment with one of the following commands in the PR:

> Note that the comment should contain the exact command as listed below, comments like **Please retry my PR** or **Bot, retry -fae** won't work:

- **retry**: execute a full QA
- **retry -fae**: execute a full QA even when some fail
- **retry {moduleName}**: execute a QA for a specific module. If a module is named **ohara-awesome**, you can enter **retry awesome** to run the QA against this specific module. Note that module prefix **ohara-** is not needed.
- **run**: execute both the Configurator and the Manager on jenkins server. If the PR makes some changes to UI, you can run this command to see the changes

After a command is successfully executed, Ohara-bot will leave a new comment in your PR's comment thread. And this comment will be updated after the command is finish running.

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
