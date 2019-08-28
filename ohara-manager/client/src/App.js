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
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import { create } from 'jss';
import { StylesProvider, jssPreset } from '@material-ui/styles';

import GlobalStyle from 'theme/globalStyle';

import Header from 'components/common/Header';
import HomePage from 'components/pages/HomePage';
import PipelinePage from 'components/pages/PipelinePage';
import PipelineNewPage from 'components/pages/PipelinePage/PipelineNewPage';
import NodesPage from 'components/pages/NodePage/NodeListPage';
import WorkspacesPage from 'components/pages/WorkspacesPage';
import WorkspacesDetailPage from 'components/pages/WorkspacesPage/WorkspacesDetailPage';
import LogsPage from 'components/pages/LogsPage';
import NotFoundPage from 'components/pages/NotFoundPage';
import SnackbarProvider from 'components/context/Snackbar/SnackbarProvider';

//We need to override Mui styles with our custom styles. See Mui docs for more info:https://material-ui.com/customization/css-in-js/#other-html-element
const jss = create({
  ...jssPreset(),
  insertionPoint: document.getElementById('jss-insertion-point'),
});

class App extends React.Component {
  render() {
    return (
      <StylesProvider jss={jss}>
        <SnackbarProvider>
          <Router>
            <React.Fragment>
              <GlobalStyle />
              <Header />
              <Switch>
                <Route
                  path="/pipelines/new/:page?/:pipelineName/:connectorName?"
                  component={PipelineNewPage}
                  data-testid="pipeline-new-page"
                />
                <Route
                  path="/pipelines/edit/:page?/:pipelineName/:connectorName?"
                  component={PipelineNewPage}
                  data-testid="pipeline-edit-page"
                />
                <Route
                  path="/pipelines"
                  component={PipelinePage}
                  data-testid="pipeline-page"
                />
                <Route
                  path="/nodes"
                  component={NodesPage}
                  data-testid="nodes-page"
                />
                <Route
                  path="/workspaces/:workspaceName/:serviceName?"
                  component={WorkspacesDetailPage}
                  data-testid="workspace-page"
                />
                <Route
                  path="/workspaces"
                  component={WorkspacesPage}
                  data-testid="workspaces-page"
                />
                <Route
                  path="/logs/:serviceName/:clusterName"
                  component={LogsPage}
                  data-testid="logs-page"
                />
                <Route
                  exact
                  path="/"
                  data-testid="home-page"
                  component={HomePage}
                />
                <Route component={NotFoundPage} data-testid="not-found-page" />
              </Switch>
            </React.Fragment>
          </Router>
        </SnackbarProvider>
      </StylesProvider>
    );
  }
}

export default App;
