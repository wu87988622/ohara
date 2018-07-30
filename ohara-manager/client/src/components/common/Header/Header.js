import React from 'react';
import styled from 'styled-components';
import { NavLink } from 'react-router-dom';

import { primary } from '../../../theme/colors';
import { HOME } from '../../../constants/url';
import { isLoggedin, deleteUserKey } from '../../../utils/authHelpers';

const Nav = styled.header`
  background-color: ${primary};
`;

const Brand = styled(NavLink)`
  padding-top: 0.75rem;
  padding-bottom: 0.75rem;
  font-size: 1rem;
  box-shadow: inset -1px 0 0 rgba(0, 0, 0, 0.25);
`;

// TODO: Use React's new context API to share user login state
// Currently, this component is not rendering immediately after logging in or out,
// you need to refresh the page.

class Header extends React.Component {
  handleLogout = () => {
    deleteUserKey();
  };

  render() {
    return (
      <Nav className="navbar navbar-dark fixed-top flex-md-nowrap p-0 shadow">
        <Brand className="navbar-brand col-sm-3 col-md-2 mr-0" to={HOME}>
          Ohara
        </Brand>
        <ul className="navbar-nav px-3">
          <li className="nav-item">
            <NavLink
              className="nav-link"
              to={isLoggedin() ? `/logout` : '/login'}
            >
              {isLoggedin() ? '登出' : '登入'}
            </NavLink>
          </li>
        </ul>
      </Nav>
    );
  }
}

export default Header;
