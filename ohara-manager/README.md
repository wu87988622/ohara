# Ohara Manager

This repository contains Ohara manager itself (an HTTP server built with node.js) and Ohara manager client (Ohara UIs built with React.js ). In the following docs, we refer **Server** as Ohara manager and **Client** as Ohara manager client.

## Initial machine setup

1.  Install [Node.js](https://nodejs.org/en/) 8.12.0 (we're using ^8.12.0 in the engines field of package.json. This means you can install node.js >=8.12.0 and < 9.0.0 see the [npm docs](https://docs.npmjs.com/misc/semver#caret-ranges-123-025-004) for more info.)

2.  Install [Yarn](https://yarnpkg.com/lang/en/) 1.7.0 (Note that you must install the exact version of yarn **1.7.0** as listed here or the **yarn.lock** file in Ohara manager could change when building on different machines)

3.  Make sure you're in the ohara-manager root and use this command to setup the app: `yarn setup`. This will install all the dependencies for both the **Server** and the **Client** as well as creating a production build for the client.

4.  **Optional**: If you're using Visual Studio Code as your editor, have a look at our Editors section.

### Linux

install these dependencies for cypress:

```sh
yum install -y xorg-x11-server-Xvfb gtk2-2.24* libXtst* libXScrnSaver* GConf2* alsa-lib*
```

> Have issues while setting up? Try the **Having issues** section to troubleshoot.

## Development

**If this is your first time running this project, you need to complete the _Initial machine setup_ section above** 👆

#### Quick start guide:

Make sure you're at the Ohara-manager root, then start Ohara manager with:

```sh
yarn start --configurator http://host:port/v0
```

> Note that configurator option is required, and you should have configurator running before starting Ohara manager.

Open another terminal tab, and start the **client**:

```
yarn start:client
```

Now, go to http://localhost:3000 and start your development, happy coding 😎

#### Full development guide:

In development, you need to start both **Server** and **Client** servers before you can start your development. Follow the instructions below:

**Server:**

Make sure you're at the ohara-manager root:

```sh
yarn start --configurator http://host:port/v0
```

> Note that the `--configurator` argument is required, you should pass in
> Ohara configurator API URL.

You can also override the default port `5050` by passing in `--port` like the following:

```sh
yarn start --configurator http://host:port/v0 --port 1234
```

After starting the server, visit `http://localhost:${PORT}` in your browser.

> Double check the configurator spelling and API URL, the URL should contain the API version number: `/v0`

**Client**:

Start the **Client** development server with:

```sh
yarn start:client
```

After starting the dev server, visit `http://localhost:3000` in your browser and start you development.

You can override the default port `3000` by passing in a environment variable:

```sh
PORT=7777 yarn start:client
```

The dev server will then start at `http://localhost:7777`

## Test

You can run all tests including **Server** and **Client** unit tests as well as **Client** End-to-End test with one single npm script:

```sh
yarn test
```

You can also run them separately as:

**Server:**

Make sure you're in the ohara-manager root, and use the following commands:

Run the test and stay in Jest watch mode

```sh
yarn test:watch
```

Generate a test coverage report

> The coverage reports can be found in `ohara-manager/coverage/`

```sh
yarn test:coverage
```

**Client:**

Run the tests and stay in Jest watch mode

```sh
yarn test:client:watch
```

Generate test coverage reports

> The coverage reports can be found in `ohara-manager/client/coverage/`

```sh
yarn test:client:coverage
```

**Client** also has End-to-End tests, you can run them via the following command:

```sh
yarn test:client:cypress
```

This will open cypress test runner, you can then run your test manually through the UIs.

## Linting

We use [ESLint](https://github.com/eslint/eslint) to lint all the JavaScript:

**Server:**

```sh
yarn lint:server
```

It's usually helpful to run linting while developing and that's included in `yarn start` command:

```sh
yarn start --configurator http://host:port/v0
```

This will start the server with `nodemon` and run the linting script whenever nodemon reloads.

**Client:**

Since our client is bootstrapped with create-react-app, so the linting part is already taken care. When starting the **Client** dev server with `yarn start:client`, the linting will be starting automatically.

Note that due to create-react-app doesn't support custom eslint rules. You need to use your text editor plugin to display the custom linting rule warnings or errors. For more info about this, please take a look at the create-react-app [docs](https://facebook.github.io/create-react-app/docs/setting-up-your-editor#displaying-lint-output-in-the-editor)

## Format

We use [Prettier](https://github.com/prettier/prettier) to format our code. You can format all `.js` files with:

```sh
yarn format
```

- You can ignore files or folders when running `yarn format` by editing the `.prettierignore` in the Ohara-manager root.

> Note that `node_modules` is ignore by default so you don't need to add that in the `.prettierignore`

## Build

**Note that this step is only required for the Client _NOT THE SERVER_**

You can get the production-ready static files by using the following command:

```sh
yarn build:client
```

> These static files will be build and put into the **/ohara-manager/client/build** directory.

## Ohara manager image

Run the following command to get the production ready build of both **Server** and **Client**. This command will only install **production needed dependencies**.

```sh
yarn setup:prod
```

After the build, copy/use these files and directories to the destination directory (this step is automatically done by gradle):

- index.js
- config.js
- package.json
- .yarnrc
- client -- only node_modules and build directories are needed
  - node_modules
  - build
- constants
- node_modules
- routes
- utils

> Note that if you add new files or dirs to the **Server** or the **Client** and these files and dirs are required for production build, please list that file in the above list as well as editing the gradle file under `ohara/ohara-assembly/build.gradle`. **Skipping this step will cause production build failed!**

**From the Ohara manager project root**, use the following command to start the manager:

```sh
 yarn start:prod --configurator http://host:port/v0
```

## CI server integration

In order to work with Graddle on Jenkins, Ohara manager provides a few npm scripts as the following:

Run tests on CI:

```sh
yarn test
```

- Run all tests including **Server** and **Client** unit tests as well as **Client** End-to-End tests. The test reports can be found in `ohara-manager/test-reports/`

- Note you should run `yarn setup` to ensure that all necessary packages are correctly installed prior to running tests.

## Clean

Clean up all running processes as well as `test-reports/` on the **Server**, `node_moduels/` on both **Server** and **Client** directories:

```sh
yarn clean
```

Clean all running processes started with node.js

```sh
yarn clean:process
```

This is useful when you want to kill all node.js processes

## Prepush

We also provide a npm script to run all the tests(both client and server unit tests and e2e tests) lint, and format all the JS files with. **Ideally, you'd run this before pushing your code to the remote repo:**

```sh
yarn prepush
```

## Editors

We would highly recommend that you use [Visual Studio Code](https://code.visualstudio.com/) (or vscode for short) to edit and author Ohara manager code.

#### Recommend vscode settings:

```json
{
  "editor.tabSize": 2,
  "editor.formatOnSave": true,
  "editor.formatOnSaveTimeout": 2000,
  "editor.tabCompletion": true,
  "emmet.triggerExpansionOnTab": true,
  "emmet.includeLanguages": {
    "javascript": "javascriptreact",
    "markdown": "html"
  },
  "prettier.printWidth": 80,
  "prettier.singleQuote": true,
  "prettier.trailingComma": "all",
  "prettier.semi": true,
  "search.exclude": {
    "**/node_modules": true,
    "**/bower_components": true,
    "**/coverage": true
  },
  "prettier.eslintIntegration": true,
  "javascript.updateImportsOnFileMove.enabled": "always"
}
```

#### Recommend extensions:

- [ESLint](https://marketplace.visualstudio.com/items?itemName=dbaeumer.vscode-eslint) - install this so vscode can display linting errors right in the editor
- [vscode-styled-components](https://marketplace.visualstudio.com/items?itemName=jpoissonnier.vscode-styled-components) - syntax highlight support for [styled component](https://github.com/styled-components/styled-components)
- [Prettier - Code formatter](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode) - code formatter, please use the recommend the settings above to ensure that our format rules are aligned

## Having issues?

- **Got an error while starting up the server: Error: Cannot find module ${module-name}**

  If you're running into this, it's probably that this module is not correctly installed on your machine. You can fix this by simply run:

  ```sh
   yarn # If this doesn't work, try `yarn add ${module-name}`
  ```

After the installation is completed, start the server again.

- **Got an error while starting up the server or client on a Linux machine: ENOSPC**

  You can run this command to increase the limit on the number of files Linux will watch. Read more [here](https://github.com/guard/listen/wiki/Increasing-the-amount-of-inotify-watchers#the-technical-details).

  ```sh
  echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf && sudo sysctl -p.
  ```

- **Node.js processes cannot be stopped even after using kill -9**

  We're using `forever` to start our node.js servers on CI, and `nodemon` while in development, so you need to use the following commands to kill them. `kill -9` or `fuser` might not work as you expected.

  use `yarn clean:process` command or `pkill node` to kill all the node.js processes
