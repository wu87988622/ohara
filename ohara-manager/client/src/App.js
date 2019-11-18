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
import styled from 'styled-components';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';

import GlobalStyle from 'theme/globalStyle';
import AppBar from 'components/Layout/AppBar';
import { DevToolDialog, TopicDataWindow } from 'components/Developer';
import NotFoundPage from 'components/NotFoundPage';
import { Pipeline, Navigator } from 'components/Pipeline';

// We need joint's CSS
import '../node_modules/jointjs/dist/joint.min.css';

const Container = styled.div`
  display: flex;

  /* 
    Make AppBar can stretch to the bottom of the browser view even if the content
    height is not enough to do so
  */
  min-height: 100vh;

  /* Resolution under 1024px is not supported, display a scrollbar */
  min-width: 1024px;
`;

const Main = styled.main`
  width: 100%;
`;

const App = () => {
  return (
    <Router>
      <Switch>
        <Route exact path="/:workspaceName/view" component={TopicDataWindow} />
        <Container className="container">
          <GlobalStyle />
          <AppBar />
          <Route
            exact
            path="/:workspaceName?/:pipelineName?"
            component={DevToolDialog}
          />
          <Route
            exact
            path="/:workspaceName/:pipelineName?"
            component={Navigator}
          />
          <Main>
            <Switch>
              <Route
                exact
                path="/:workspaceName?/:pipelineName?"
                component={Pipeline}
              />
              <Route path="*" component={NotFoundPage} />
            </Switch>
          </Main>
        </Container>
      </Switch>
    </Router>
  );
};

export default App;
