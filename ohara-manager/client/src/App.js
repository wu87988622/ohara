import React from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';

import Header from 'components/common/Header';
import Nav from 'components/common/Nav';
import HomePage from 'components/pages/HomePage';
import Pipeline from 'components/pages/PipelinePage';
import PipelineNew from 'components/pages/PipelinePage/PipelineNewPage';
import Kafka from 'components/pages/KafkaPage';
import Configuration from 'components/pages/ConfigurationPage';
import LoginPage from 'components/pages/LoginPage';
import LogoutPage from 'components/pages/LogoutPage';
import NotFoundPage from 'components/pages/NotFoundPage';
import { getUserKey } from 'utils/authHelpers';

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
            <Route
              path="/pipeline/new/:page/:pipelineId/:topicId/:sourceId?/:sinkId?"
              component={PipelineNew}
            />
            <Route
              path="/pipeline/edit/:page/:pipelineId/:topicId/:sourceId?/:sinkId?"
              component={PipelineNew}
            />
            <Route path="/pipeline" component={Pipeline} />
            <Route path="/kafka" component={Kafka} />
            <Route path="/configuration" component={Configuration} />
            <Route
              path="/login"
              render={props => (
                <LoginPage
                  updateLoginState={this.updateLoginState}
                  {...props}
                />
              )}
            />
            <Route
              path="/logout"
              render={props => (
                <LogoutPage
                  updateLoginState={this.updateLoginState}
                  {...props}
                />
              )}
            />
            <Route exact path="/" component={HomePage} />
            <Route component={NotFoundPage} />
          </Switch>
        </React.Fragment>
      </Router>
    );
  }
}

export default App;
