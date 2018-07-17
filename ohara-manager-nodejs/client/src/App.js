import React from 'react';
import { BrowserRouter, Route } from 'react-router-dom';

import Header from './components/common/Header';
import Nav from './components/common/Nav';
import HomePage from './components/pages/HomePage';
import JobsPage from './components/pages/JobsPage';
import AccountPage from './components/pages/AccountPage';
import MonitorPage from './components/pages/MonitorPage';
import DashboardPage from './components/pages/DashboardPage';
import TopicPage from './components/pages/TopicPage';
import SchemaPage from './components/pages/SchemaPage';
import LoginPage from './components/pages/LoginPage';
import LogoutPage from './components/pages/LogoutPage';

const App = () => (
  <BrowserRouter>
    <div>
      <Header />
      <Nav />
      <Route exact path="/" component={HomePage} />
      <Route path="/jobs" component={JobsPage} />
      <Route path="/account" component={AccountPage} />
      <Route path="/monitor" component={MonitorPage} />
      <Route path="/dashboard" component={DashboardPage} />
      <Route path="/topic" component={TopicPage} />
      <Route path="/schema" component={SchemaPage} />
      <Route path="/login" component={LoginPage} />
      <Route path="/logout" component={LogoutPage} />
    </div>
  </BrowserRouter>
);

export default App;
