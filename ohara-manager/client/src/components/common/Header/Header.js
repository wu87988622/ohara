import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { NavLink } from 'react-router-dom';

import * as URLS from 'constants/urls';
import { white, lighterGray } from 'theme/variables';
import { deleteUserKey } from 'utils/authUtils';

const HeaderWrapper = styled.header`
  background-color: ${white};
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  height: 59px;
  border-bottom: 1px solid ${lighterGray};
  display: flex;
  align-items: center;
  padding: 0 30px;
  z-index: 100;
`;

const Ul = styled.ul`
  margin-left: auto;
`;

// TODO: Use React's new context API to share user login state
// Currently, this component is not rendering immediately after logging in or out,
// you need to refresh the page.

class Header extends React.Component {
  static propTypes = {
    isLogin: PropTypes.bool.isRequired,
  };

  handleLogout = () => {
    deleteUserKey();
  };

  render() {
    const { isLogin } = this.props;

    return (
      <HeaderWrapper>
        <Ul>
          <li>
            <NavLink
              data-testid="login-state"
              to={isLogin ? URLS.LOGOUT : URLS.LOGIN}
            >
              {isLogin ? 'Log out' : 'Log in'}
            </NavLink>
          </li>
        </Ul>
      </HeaderWrapper>
    );
  }
}

export default Header;
