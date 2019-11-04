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
import ReactDOM from 'react-dom';
import { ThemeProvider } from 'styled-components';
import {
  ThemeProvider as MuiThemeProvider,
  StylesProvider,
} from '@material-ui/styles';

import App from './App';
import MuiTheme from './theme/muiTheme';
import { SnackbarProvider } from './context/SnackbarContext';
import { WorkspaceProvider } from './context/WorkspaceContext';
import { PipelineProvider } from './context/PipelineContext';
import { NewWorkspaceProvider } from './context/NewWorkspaceContext';
import { NodeDialogProvider } from './context/NodeDialogContext';

ReactDOM.render(
  <StylesProvider injectFirst>
    <MuiThemeProvider theme={MuiTheme}>
      <ThemeProvider theme={MuiTheme}>
        <SnackbarProvider>
          <NewWorkspaceProvider>
            <WorkspaceProvider>
              <PipelineProvider>
                <NodeDialogProvider>
                  <App />
                </NodeDialogProvider>
              </PipelineProvider>
            </WorkspaceProvider>
          </NewWorkspaceProvider>
        </SnackbarProvider>
      </ThemeProvider>
    </MuiThemeProvider>
  </StylesProvider>,
  document.getElementById('root'),
);
