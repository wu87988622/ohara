import React from 'react';
import styled from 'styled-components';
import { NavLink } from 'react-router-dom';

import { primary } from '../../../theme/colors';
import * as URL from '../../../constants/url';

const NavWrapper = styled.div`
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  padding: 48px 0 0;
  box-shadow: inset -1px 0 0 rgba(0, 0, 0, 0.1);
`;

const NavInner = styled.nav`
  position: relative;
  top: 0;
  height: calc(100vh - 48px);
  padding-top: 0.5rem;
  overflow-x: hidden;
  overflow-y: auto;
`;

const NavLinkWrapper = styled(NavLink)`
  color: black;
  font-size: 14px;

  &.active {
    color: ${primary};
  }
`;

const IconWrapper = styled.i`
  margin-right: 8px;
`;

const Nav = () => {
  return (
    <NavWrapper className="col-md-2 d-none d-md-block bg-light sidebar">
      <NavInner className="sidebar-sticky">
        <ul className="nav flex-column">
          <li className="nav-item">
            <NavLinkWrapper
              className="nav-link"
              exact
              activeClassName="active"
              to={URL.JOBS}
            >
              <IconWrapper className="fas fa-align-left" />
              Jobs
            </NavLinkWrapper>
          </li>

          <li className="nav-item">
            <NavLinkWrapper
              className="nav-link"
              exact
              activeClassName="active"
              to={URL.ACCOUNT}
            >
              <IconWrapper className="fas fa-user" />
              Account
            </NavLinkWrapper>
          </li>

          <li className="nav-item">
            <NavLinkWrapper
              className="nav-link"
              exact
              activeClassName="active"
              to={URL.MONITOR}
            >
              <IconWrapper className="fas fa-desktop" />
              Monitor
            </NavLinkWrapper>
          </li>

          <li className="nav-item">
            <NavLinkWrapper
              className="nav-link"
              exact
              activeClassName="active"
              to={URL.DASHBOARD}
            >
              <IconWrapper className="fas fa-chart-bar" />
              DashBoard
            </NavLinkWrapper>
          </li>

          <li className="nav-item">
            <NavLinkWrapper
              className="nav-link"
              exact
              activeClassName="active"
              to={URL.TOPICS}
            >
              <IconWrapper className="fas fa-codepen" />
              Topic
            </NavLinkWrapper>
          </li>

          <li className="nav-item">
            <NavLinkWrapper
              className="nav-link"
              exact
              activeClassName="active"
              to={URL.SCHEMAS}
            >
              <IconWrapper className="fas fa-copy" />
              Schemas
            </NavLinkWrapper>
          </li>
        </ul>
      </NavInner>
    </NavWrapper>
  );
};

export default Nav;
