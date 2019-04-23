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

import Header from 'components/common/Header';
import HomePage from 'components/pages/HomePage';
import PipelinePage from 'components/pages/PipelinePage';
import PipelineNewPage from 'components/pages/PipelinePage/PipelineNewPage';
import NodesPage from 'components/pages/NodePage/NodeListPage';
import ServicesPage from 'components/pages/Services';
import MonitoringPage from 'components/pages/MonitoringPage';
import LogsPage from 'components/pages/LogsPage';
import LoginPage from 'components/pages/LoginPage';
import LogoutPage from 'components/pages/LogoutPage';
import NotFoundPage from 'components/pages/NotFoundPage';
import { getUserKey } from 'utils/authUtils';

class App extends React.Component {
  state = {
    isLogin: false,
  };

  componentDidMount() {
    const key = getUserKey();
    if (key) {
      this.setState({ isLogin: true });
    }
  }

  updateLoginState = state => {
    this.setState({ isLogin: state });
  };

  render() {
    const { isLogin } = this.state;

    return (
      <Router>
        <React.Fragment>
          <Header isLogin={isLogin} />
          <Switch>
            <Route
              path="/pipelines/new/:page?/:pipelineId/:connectorId?"
              component={PipelineNewPage}
              data-testid="pipeline-new-page"
            />
            <Route
              path="/pipelines/edit/:page?/:pipelineId/:connectorId?"
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
              path="/services/:serviceName?/:clusterName?"
              component={ServicesPage}
              data-testid="services-page"
            />
            <Route
              path="/monitoring"
              component={MonitoringPage}
              data-testid="monitoring-page"
            />
            <Route
              path="/logs/:serviceName/:clusterName"
              component={LogsPage}
              data-testid="logs-page"
            />
            <Route
              path="/login"
              data-testid="login-page"
              render={props => (
                <LoginPage
                  updateLoginState={this.updateLoginState}
                  {...props}
                />
              )}
            />
            <Route
              path="/logout"
              data-testid="logout-page"
              render={props => (
                <LogoutPage
                  updateLoginState={this.updateLoginState}
                  {...props}
                />
              )}
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
    );
  }
}

export default App;
