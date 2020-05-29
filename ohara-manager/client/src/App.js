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
import CssBaseline from '@material-ui/core/CssBaseline';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';

import GlobalStyle from 'theme/globalStyle';
import AppLayout from 'components/Layout';
import { NotFoundPage, NotImplementedPage } from 'components/ErrorPages';
import { DataWindow } from 'components/DevTool';

// We need joint's CSS
import '../node_modules/jointjs/dist/joint.min.css';

const App = () => {
  return (
    <>
      <GlobalStyle />
      <CssBaseline />
      <Router>
        <Switch>
          <Route
            component={NotImplementedPage}
            exact
            path="/501-page-not-implemented"
          />
          <Route
            component={DataWindow}
            exact
            path="/:workspaceName?/:pipelineName?/view"
          />
          <Route
            component={AppLayout}
            exact
            path="/:workspaceName?/:pipelineName?"
          />
          <Route component={NotFoundPage} path="*" />
        </Switch>
      </Router>
    </>
  );
};

export default App;
