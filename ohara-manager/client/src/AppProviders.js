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
import { EditWorkspaceProvider } from './context/EditWorkspaceContext';
import { NodeDialogProvider } from './context/NodeDialogContext';
import { TopicProvider } from './context/TopicContext';

const AppProviders = ({ children }) => {
  return (
    <StylesProvider injectFirst>
      <MuiThemeProvider theme={MuiTheme}>
        <ThemeProvider theme={MuiTheme}>
          <SnackbarProvider>
            <NewWorkspaceProvider>
              <WorkspaceProvider>
                <EditWorkspaceProvider>
                  <PipelineProvider>
                    <TopicProvider>
                      <NodeDialogProvider>{children}</NodeDialogProvider>
                    </TopicProvider>
                  </PipelineProvider>
                </EditWorkspaceProvider>
              </WorkspaceProvider>
            </NewWorkspaceProvider>
          </SnackbarProvider>
        </ThemeProvider>
      </MuiThemeProvider>
    </StylesProvider>
  );
};

AppProviders.propTypes = {
  children: PropTypes.any.isRequired,
};

export default AppProviders;
