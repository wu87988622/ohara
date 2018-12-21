import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { NavLink } from 'react-router-dom';

import NAVS from 'constants/navs';
import * as URLS from 'constants/urls';
import ConfigurationModal from 'pages/ConfigurationModal';
import { white, blue, dimBlue, lighterGray } from 'theme/variables';

const Wrapper = styled.div`
  background-color: ${white};
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  height: 59px;
  border-bottom: 1px solid ${lighterGray};
  padding: 0 50px;
  z-index: 100;
`;

Wrapper.displayName = 'Wrapper';

const HeaderWrapper = styled.header`
  width: 100%;
  height: 100%;
  max-width: 1200px;
  display: flex;
  align-items: center;
  margin: auto;
`;

HeaderWrapper.displayName = 'Header';

const Brand = styled(NavLink)`
  font-family: Merriweather, sans-serif;
  color: ${blue};
  font-size: 24px;
  padding: 0;
  display: block;
`;

Brand.displayName = 'Brand';

const Nav = styled.nav`
  margin-left: 54px;
  background-color: ${white};
`;

Nav.displayName = 'Nav';

const Link = styled(NavLink)`
  color: ${dimBlue};
  font-size: 14px;
  padding: 15px 0;
  margin: 10px 20px;
  position: relative;
  transition: 0.3s all;

  &:hover,
  &.active {
    color: ${blue};
  }
`;

Link.displayName = 'Link';

const ConfigBtn = styled.button`
  border: none;
  color: ${dimBlue};
  font-size: 18px;
  margin-left: auto;

  &:hover,
  &.active {
    color: ${blue};
  }
`;

const Login = styled(Link)`
  margin: 0 10px;
`;

Login.displayName = 'Login';

const Icon = styled.i`
  margin-right: 8px;
`;

Icon.displayName = 'Icon';

class Header extends React.Component {
  state = {
    isModalActive: false,
    partitions: '',
    replicationFactor: '',
  };
  static propTypes = {
    isLogin: PropTypes.bool.isRequired,
  };

  handleModalOpen = () => {
    this.setState({ isModalActive: true });
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false });
  };

  render() {
    const { isLogin } = this.props;
    const { isModalActive } = this.state;

    return (
      <Wrapper>
        <ConfigurationModal
          isActive={isModalActive}
          handleClose={this.handleModalClose}
        />
        <HeaderWrapper>
          <Brand to={URLS.HOME}>Ohara</Brand>
          <Nav>
            {NAVS.map(({ testId, to, iconCls, text }) => {
              return (
                <Link
                  exact
                  activeClassName="active"
                  key={testId}
                  data-testid={testId}
                  to={to}
                >
                  <Icon className={`fas ${iconCls}`} />
                  <span>{text}</span>
                </Link>
              );
            })}
          </Nav>

          <ConfigBtn data-testid="config-btn" onClick={this.handleModalOpen}>
            <i className="fas fa-cog" />
          </ConfigBtn>

          <Login
            data-testid="login-state"
            to={isLogin ? URLS.LOGOUT : URLS.LOGIN}
          >
            {isLogin ? 'Log out' : 'Log in'}
          </Login>
        </HeaderWrapper>
      </Wrapper>
    );
  }
}

export default Header;
