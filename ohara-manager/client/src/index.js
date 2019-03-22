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
import App from 'App';
import { ThemeProvider } from 'styled-components';

import { create } from 'jss';
import JssProvider from 'react-jss/lib/JssProvider';

import { createGenerateClassName, jssPreset } from '@material-ui/styles';

import * as CSS_VARS from './theme/variables';
import 'theme/globalStyle';
import 'toastrConfigs';

const generateClassName = createGenerateClassName();
const jss = create({
  ...jssPreset(),
  insertionPoint: 'insertion-point-jss',
});

ReactDOM.render(
  <JssProvider jss={jss} generateClassName={generateClassName}>
    <ThemeProvider theme={CSS_VARS}>
      <App />
    </ThemeProvider>
  </JssProvider>,
  document.getElementById('root'),
);
