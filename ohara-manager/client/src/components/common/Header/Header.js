import React from 'react';
import styled from 'styled-components';
import { NavLink } from 'react-router-dom';

import { white, lightGray } from '../../../theme/colors';
import { isLoggedin, deleteUserKey } from '../../../utils/authHelpers';

const HeaderWrapper = styled.header`
  background-color: ${white};
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  height: 59px;
  border-bottom: 1px solid ${lightGray};
  display: flex;
  align-items: center;
  padding: 0 30px;
`;

const Ul = styled.ul`
  margin-left: auto;
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
      <HeaderWrapper>
        <Ul>
          <li>
            <NavLink to={isLoggedin() ? `/logout` : '/login'}>
              {isLoggedin() ? '登出' : '登入'}
            </NavLink>
          </li>
        </Ul>
      </HeaderWrapper>
    );
  }
}

export default Header;
