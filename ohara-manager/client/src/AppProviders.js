/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';
import {
  ThemeProvider as MuiThemeProvider,
  StylesProvider,
} from '@material-ui/styles';

import MuiTheme from './theme/muiTheme';
import { ErrorBoundary } from 'components/ErrorPages';
import {
  ApiProvider,
  AppProvider,
  BrokerProvider,
  DialogProvider,
  FileProvider,
  NodeProvider,
  TopicProvider,
  WorkerProvider,
  WorkspaceProvider,
  EditWorkspaceProvider,
  ZookeeperProvider,
  PipelineProvider,
  ConfiguratorProvider,
  StreamProvider,
} from './context';
import { SnackbarProvider } from './context/SnackbarContext';
import { NewWorkspaceProvider } from './context/NewWorkspaceContext';
import { NodeDialogProvider } from './context/NodeDialogContext';

const ContextProviderComposer = ({ contextProviders, children }) => {
  return contextProviders.reduceRight(
    (children, parent) => React.cloneElement(parent, { children }),
    children,
  );
};

// Combine multiple providers together, the solution is coming from here:
// https://github.com/facebook/react/issues/14520#issuecomment-451077601
const AppProviders = ({ children }) => {
  return (
    <ContextProviderComposer
      contextProviders={[
        <StylesProvider injectFirst children={children} />,
        <MuiThemeProvider theme={MuiTheme} children={children} />,
        <ThemeProvider theme={MuiTheme} children={children} />,
        <ErrorBoundary children={children} />,
        <AppProvider children={children} />,
        <DialogProvider children={children} />,
        <SnackbarProvider children={children} />,
        <ApiProvider children={children} />,
        <ConfiguratorProvider children={children} />,
        <NodeProvider children={children} />,
        <ZookeeperProvider children={children} />,
        <BrokerProvider children={children} />,
        <WorkerProvider children={children} />,
        <WorkspaceProvider children={children} />,
        <NewWorkspaceProvider children={children} />,
        <EditWorkspaceProvider children={children} />,
        <PipelineProvider children={children} />,
        <TopicProvider children={children} />,
        <FileProvider children={children} />,
        <StreamProvider children={children} />,
        <NodeDialogProvider children={children} />,
      ]}
    >
      {children}
    </ContextProviderComposer>
  );
};

AppProviders.propTypes = {
  children: PropTypes.node.isRequired,
};

export default AppProviders;
