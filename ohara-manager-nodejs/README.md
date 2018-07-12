# Ohara Manager

This repository contains Ohara manager itself (A HTTP server built with node.js) and Ohara manager client (Ohara fastdata UIs built with React.js via create-react-app). In the following docs, we refer **server** as Ohara manager and **client** as Ohara manager client.

## Requirements

- [Node.js](https://nodejs.org/en/) v8.11.2
- [Yarn](https://yarnpkg.com/lang/en/) v1.7.0

## Install dependencies

Use `node -v` and `yarn -v` to see if you have the correct version of node.js and yarn (see the requirements section) installed in your machine.

Install all the dependencies for the server

Make sure you're in the root level of the project, and use the following command to start the dev server

```sh
yarn
```

Install all the dependencies for the client

Make sure you're in the **client** directory before using these commands.

```sh
yarn
```

## Development

Start the development server:

Make sure you're in the root level of the project, and use the following command to start the dev server

```sh
yarn start
```

## Client side development

Make sure you're in the **client** directory before using these commands.

Start the development server:

```sh
yarn start
```

Run all unit test suites and stay in the watch mode. Note that a junit like report will be generated every time when you run this command:

```sh
yarn test
```

Generate a coverage report:

```sh
yarn test:coverage
```

You can run End-to-End test with the following command. If this is your first time running this, you may need some extra setups in order to run it.

```sh
yarn cypress
```

## Build

You can get the production-ready static files by using the following command:

These static files will be build and put into the **/build** directory.

```sh
yarn build
```
