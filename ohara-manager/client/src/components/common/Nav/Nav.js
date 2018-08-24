import React from 'react';
import styled from 'styled-components';
import { NavLink } from 'react-router-dom';

import * as URL from '../../../constants/urls';
import { blue, white, dimBlue, lighterGray } from '../../../theme/variables';

const NavWrapper = styled.div`
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  width: 199px;
  border-right: 1px solid ${lighterGray}
  background-color: ${white};
`;

const Brand = styled(NavLink)`
  font-family: Merriweather, sans-serif;
  color: ${blue};
  font-size: 24px;
  margin: 25px auto 0 50px;
  padding: 0;
  display: block;
`;

Brand.displayName = 'Brand';

const Ul = styled.nav`
  margin-top: 100px;
`;

const LinkWrapper = styled(NavLink)`
  color: ${dimBlue};
  font-size: 14px;
  display: block;
  padding: 15px 0 15px 50px;
  margin: 10px 0;
  position: relative;
  transition: 0.3s all;

  &:hover {
    color: ${blue};

    &:after {
      content: '';
      border-right: 2px solid ${blue};
      position: absolute;
      top: 0;
      left: 99%;
      height: 100%;
      transition: 0.3s all;
    }
  }

  &.active {
    color: ${blue};

    &:after {
      content: '';
      border-right: 2px solid ${blue};
      position: absolute;
      top: 0;
      left: 99%;
      height: 100%;
      transition: 0.3s all;
    }
  }
`;

LinkWrapper.displayName = 'LinkWrapper';

const IconWrapper = styled.i`
  margin-right: 8px;
`;

const Nav = () => {
  return (
    <NavWrapper>
      <Brand data-testid="brand" to={URL.HOME}>
        Ohara
      </Brand>

      <Ul>
        <LinkWrapper
          data-testid="nav-pipeline"
          exact
          activeClassName="active"
          to={URL.PIPELINE}
        >
          <IconWrapper className="fas fa-code-branch" />
          Pipeline
        </LinkWrapper>

        <LinkWrapper
          data-testid="nav-kafka"
          exact
          activeClassName="active"
          to={URL.KAFKA}
        >
          <IconWrapper className="fas fa-align-left" />
          Kafka
        </LinkWrapper>

        <LinkWrapper
          data-testid="nav-configuration"
          exact
          activeClassName="active"
          to={URL.CONFIGURATION}
        >
          <IconWrapper className="fas fa-cog" />
          Configuration
        </LinkWrapper>
      </Ul>
    </NavWrapper>
  );
};

export default Nav;
