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
import { SnackbarProvider } from './context/SnackbarContext';
import { WorkspaceProvider } from './context/WorkspaceContext';
import { PipelineProvider } from './context/PipelineContext';
import { NewWorkspaceProvider } from './context/NewWorkspaceContext';
import { AddTopicProvider } from './context/AddTopicContext';
import { ViewTopicProvider } from './context/ViewTopicContext';
import { EditWorkspaceProvider } from './context/EditWorkspaceContext';
import { NodeDialogProvider } from './context/NodeDialogContext';
import { TopicProvider } from './context/TopicContext';
import { DevToolDialogProvider } from './context/DevToolDialogContext';

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
        <SnackbarProvider children={children} />,
        <NewWorkspaceProvider children={children} />,
        <WorkspaceProvider children={children} />,
        <EditWorkspaceProvider children={children} />,
        <PipelineProvider children={children} />,
        <TopicProvider children={children} />,
        <AddTopicProvider children={children} />,
        <ViewTopicProvider children={children} />,
        <NodeDialogProvider children={children} />,
        <DevToolDialogProvider children={children} />,
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
