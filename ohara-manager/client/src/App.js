import React from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';

import Header from './components/common/Header';
import Nav from './components/common/Nav';
import HomePage from './components/pages/HomePage';
import Pipeline from './components/pages/PipelinePage';
import PipelineNew from './components/pages/PipelinePage/PipelineNewPage';
import Kafka from './components/pages/KafkaPage';
import Configuration from './components/pages/ConfigurationPage';
import LoginPage from './components/pages/LoginPage';
import LogoutPage from './components/pages/LogoutPage';
import NotFoundPage from './components/pages/NotFoundPage';

import { getUserKey } from './utils/authHelpers';

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
          <Nav />
          <Switch>
            <Route exact path="/" component={HomePage} />
            <Route exact path="/pipeline" component={Pipeline} />
            <Route
              exact
              path="/pipeline/new/:page/:pipelineId/:topicId"
              component={PipelineNew}
            />
            <Route exact path="/kafka" component={Kafka} />
            <Route exact path="/configuration" component={Configuration} />
            <Route
              exact
              path="/login"
              render={props => (
                <LoginPage
                  updateLoginState={this.updateLoginState}
                  {...props}
                />
              )}
            />
            <Route
              exact
              path="/logout"
              render={props => (
                <LogoutPage
                  updateLoginState={this.updateLoginState}
                  {...props}
                />
              )}
            />
            <Route component={NotFoundPage} />
          </Switch>
        </React.Fragment>
      </Router>
    );
  }
}

export default App;
