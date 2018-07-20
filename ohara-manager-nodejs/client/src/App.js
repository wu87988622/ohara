import React from 'react';
import styled from 'styled-components';
import { BrowserRouter, Route, Switch } from 'react-router-dom';

import Header from './components/common/Header';
import Nav from './components/common/Nav';
import HomePage from './components/pages/HomePage';
import JobsPage from './components/pages/JobsPage';
import AccountPage from './components/pages/AccountPage';
import MonitorPage from './components/pages/MonitorPage';
import DashboardPage from './components/pages/DashboardPage';
import TopicsPage from './components/pages/TopicsPage';
import SchemasPage from './components/pages/SchemasPage';
import SchemasDetailsPage from './components/pages/SchemasPage/SchemasDetailsPage';
import LoginPage from './components/pages/LoginPage';
import LogoutPage from './components/pages/LogoutPage';
import NotFoundPage from './components/pages/NotFoundPage';

const Container = styled.div`
  height: calc(100vh - 200px);
  margin: 60px;
`;

const App = () => (
  <BrowserRouter>
    <Container className="container">
      <Header />
      <Nav />
      <Switch>
        <Route exact path="/" component={HomePage} />
        <Route exact path="/jobs" component={JobsPage} />
        <Route exact path="/account" component={AccountPage} />
        <Route exact path="/monitor" component={MonitorPage} />
        <Route exact path="/dashboard" component={DashboardPage} />
        <Route exact path="/topics" component={TopicsPage} />
        <Route exact path="/schemas" component={SchemasPage} />
        <Route exact path="/schemas/:uuid" component={SchemasDetailsPage} />
        <Route exact path="/login" component={LoginPage} />
        <Route exact path="/logout" component={LogoutPage} />
        <Route component={NotFoundPage} />
      </Switch>
    </Container>
  </BrowserRouter>
);

export default App;
