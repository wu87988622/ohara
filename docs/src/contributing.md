# Contributing

All we love is only pull request so we have some rules used to make your PR looks good for reviewers.

> Note that you should file a new issue in our jira board to discuss the PR detail before submitting a PR.

## Quick start

- Fork and clone the repo
- Install dependencies. See our [how_to_build](how_to_build.md#gradle-commands) for development machine setup
- Create a branch with your PR with `git checkout -b ${your-branch-name}`
- Push your PR to remote: `git push origin ${your-branch-name}`
- Create the PR with GitHub web UI and wait for reviews

## Pull request commands:

These commands will come in handy when you want to test your PR on our QA(CI server).

To start a QA run, you can simply leave a comment with one of the following commands in the PR:

> Note that the comment should contain the exact command as listed below, comments like **Please retry my PR** or **Bot, retry -fae** won't work:

- **retry**: execute a full QA
- **retry -fae**: execute a full QA even when some fail
- **retry \${moduleName}**: execute a QA for a specific module. If a module is named **ohara-awesome**, you can enter **retry awesome** to run the QA against this specific module. Note that module prefix **ohara-** is not needed.
- **run**: execute both the Configurator and the Manager on jenkins server. If the PR makes some changes to UI, you can run this command to see the changes

The build status can be seen at the bottom of your PR.

## A pull request must:

#### Pass all tests

- Your PR should not make ohara unstable, if it does. It should be reverted ASAP.
- You can either run these tests on your local (see our [how_to_build](how_to_build.md) for more info on how to run tests) or by opening the PR on our repo. These tests will be running on your CI server.

#### Pass code style check

You can automatically fix these issues with a single command:

```sh
gradle spotlessApply
```

#### Address all reviewers' comments

## A pull request should:

#### Be as small in scope as possible.

Large PR is often hard to review.

#### Add new tests

## A pull request should not:

#### Bring in new libraries (or updating libraries to new version) without prior discussion

Do not update the dependencies unless you have a good reason.

#### Bring in new module without prior discussion

#### Bring in new APIs for Configurator without prior discussion
