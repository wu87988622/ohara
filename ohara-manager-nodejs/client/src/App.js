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
import SchemaPage from './components/pages/SchemaPage';
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
        <Route path="/jobs" component={JobsPage} />
        <Route path="/account" component={AccountPage} />
        <Route path="/monitor" component={MonitorPage} />
        <Route path="/dashboard" component={DashboardPage} />
        <Route path="/topics" component={TopicsPage} />
        <Route path="/schema" component={SchemaPage} />
        <Route path="/login" component={LoginPage} />
        <Route path="/logout" component={LogoutPage} />
        <Route component={NotFoundPage} />
      </Switch>
    </Container>
  </BrowserRouter>
);

export default App;
