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
import { render } from '@testing-library/react';
import { Router } from 'react-router-dom';
import { createMemoryHistory } from 'history';

import { ThemeProvider as MuiThemeProvider } from '@material-ui/styles';
import { ThemeProvider } from 'styled-components';
import SnackbarProvider from 'components/context/Snackbar/SnackbarProvider';
import MuiTheme from 'theme/muiTheme';
import * as CSS_VARS from 'theme/variables';

// this is a handy function that I would utilize for any component
// that relies on the router being in context
export const renderWithRouter = (
  ui,
  {
    route = '/',
    history = createMemoryHistory({ initialEntries: [route] }),
  } = {},
) => {
  return {
    ...render(<Router history={history}>{ui}</Router>),
    // adding `history` to the returned utilities to allow us
    // to reference it in our tests (just try to avoid using
    // this to test implementation details).
    history,
  };
};

export const renderWithTheme = ui => {
  return {
    ...render(
      <MuiThemeProvider theme={MuiTheme}>
        <ThemeProvider theme={{ ...CSS_VARS, ...MuiTheme }}>
          <SnackbarProvider>{ui}</SnackbarProvider>
        </ThemeProvider>
      </MuiThemeProvider>,
    ),
  };
};

export const renderWithProvider = (
  ui,
  {
    route = '/',
    history = createMemoryHistory({ initialEntries: [route] }),
  } = {},
) => {
  return {
    ...render(
      <MuiThemeProvider theme={MuiTheme}>
        <ThemeProvider theme={{ ...CSS_VARS, ...MuiTheme }}>
          <SnackbarProvider>
            <Router history={history}>{ui}</Router>
          </SnackbarProvider>
        </ThemeProvider>
      </MuiThemeProvider>,
    ),
    // adding `history` to the returned utilities to allow us
    // to reference it in our tests (just try to avoid using
    // this to test implementation details).
    history,
  };
};

export const getTestById = id => `[data-testid="${id}"]`;
