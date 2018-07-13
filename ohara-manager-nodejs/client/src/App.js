import React from 'react';
import styled from 'styled-components';
import { BrowserRouter, Route, NavLink } from 'react-router-dom';

import Home from './components/Home';
import SchemaPage from './components/SchemaPage';
import TopicPage from './components/TopicPage';
import LoginPage from './components/pages/LoginPage';

const NavWrapper = styled.ul`
  margin: 20px;
`;

const App = () => (
  <BrowserRouter>
    <div>
      <NavWrapper className="nav nav-pills">
        <li className="nav-item">
          <NavLink
            data-test="nav-home"
            className="nav-link"
            exact
            activeClassName="active"
            to="/"
          >
            Home
          </NavLink>
        </li>
        <li className="nav-item">
          <NavLink
            data-test="nav-schema"
            className="nav-link"
            activeClassName="active"
            to="/schema"
          >
            Schema
          </NavLink>
        </li>

        <li className="nav-item">
          <NavLink
            data-test="nav-topics"
            className="nav-link"
            activeClassName="active"
            to="/topics"
          >
            Topics
          </NavLink>
        </li>
      </NavWrapper>
      <Route exact path="/" component={Home} />
      <Route path="/schema" component={SchemaPage} />
      <Route path="/topics" component={TopicPage} />
      <Route path="/login" component={LoginPage} />
    </div>
  </BrowserRouter>
);

export default App;
