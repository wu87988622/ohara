import styled from 'styled-components';
import { NavLink } from 'react-router-dom';
import * as CSS_VARS from 'theme/variables';

const Layout = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;

  & > div {
    display: flex;
    flex: 1;
    position: absolute;
    outline: none;
    overflow: hidden;
    flex-direction: row;
    max-width: 1200px;
    width: 100%;
    height: calc(100% - 100px);

    & > div:first-child {
      flex: 0 0 auto;
      position: relative;
      outline: none;
      overflow: auto;
      width: auto;
      margin: 1rem 0;
      padding: 0 1rem;
      border-right: 1px solid #aaa;
    }

    & > div:not(:first-child) {
      margin: 1rem 0;
      padding: 0 2rem;
      width: 100%;
    }
  }
`;

const Link = styled(NavLink)`
  color: ${CSS_VARS.dimBlue};
  font-size: 14px;
  padding: 0;
  margin: 0.5rem 1rem 0.5rem 0;
  position: relative;
  transition: 0.3s all;

  &:hover,
  &.active {
    color: ${CSS_VARS.blue};
  }

  display: block;
  font-weight: 700;
`;

const SubLink = styled(Link)`
  margin: 1rem 1rem 1rem 2rem;
  font-weight: 400;
`;

const Divider = styled.div`
  margin: 1.5rem 0;
`;

export { Layout, Link, SubLink, Divider };
