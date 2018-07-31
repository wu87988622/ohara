import React from 'react';
import { BrowserRouter, Route, Switch } from 'react-router-dom';

import Header from './components/common/Header';
import Nav from './components/common/Nav';
import HomePage from './components/pages/HomePage';
import Pipeline from './components/pages/PipelinePage';
import Kafka from './components/pages/KafkaPage';
import Configuration from './components/pages/ConfigurationPage';

import TopicsPage from './components/pages/TopicsPage';
import SchemasPage from './components/pages/SchemasPage';
import SchemasDetailsPage from './components/pages/SchemasPage/SchemasDetailsPage';
import LoginPage from './components/pages/LoginPage';
import LogoutPage from './components/pages/LogoutPage';
import NotFoundPage from './components/pages/NotFoundPage';

const App = () => (
  <BrowserRouter>
    <div>
      <Header />
      <Nav />
      <Switch>
        <Route exact path="/" component={HomePage} />
        <Route exact path="/pipeline" component={Pipeline} />
        <Route exact path="/kafka" component={Kafka} />
        <Route exact path="/configuration" component={Configuration} />

        <Route exact path="/topics" component={TopicsPage} />
        <Route exact path="/schemas" component={SchemasPage} />
        <Route exact path="/schemas/:uuid" component={SchemasDetailsPage} />
        <Route exact path="/login" component={LoginPage} />
        <Route exact path="/logout" component={LogoutPage} />
        <Route component={NotFoundPage} />
      </Switch>
    </div>
  </BrowserRouter>
);

export default App;
