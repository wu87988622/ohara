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

import React, { StrictMode } from 'react';
import ReactDOM from 'react-dom';
import { ThemeProvider } from 'styled-components';
import {
  ThemeProvider as MuiThemeProvider,
  StylesProvider,
} from '@material-ui/styles';

import App from './App';
import MuiTheme from './theme/muiTheme';
import { SnackbarProvider } from './context/SnackbarContext';
import { WorkspaceProvider } from './context/WorkspaceConetxt';

ReactDOM.render(
  <StrictMode>
    <StylesProvider injectFirst>
      <MuiThemeProvider theme={MuiTheme}>
        <ThemeProvider theme={MuiTheme}>
          <SnackbarProvider>
            <WorkspaceProvider>
              <App />
            </WorkspaceProvider>
          </SnackbarProvider>
        </ThemeProvider>
      </MuiThemeProvider>
    </StylesProvider>
  </StrictMode>,
  document.getElementById('root'),
);
