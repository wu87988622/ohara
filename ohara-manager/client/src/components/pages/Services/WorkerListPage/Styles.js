import styled from 'styled-components';
import { NavLink } from 'react-router-dom';

import { Button } from 'common/Form';
import { DataTable } from 'common/Table';
import * as CSS_VARS from 'theme/variables';

const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

const NewNodeBtn = styled(Button)`
  margin-left: auto;
`;

const Table = styled(DataTable)`
  text-align: left;

  .is-running {
    background: ${CSS_VARS.trBgColor};
  }
`;

const Link = styled(NavLink)`
  color: ${CSS_VARS.lightBlue};

  &:hover {
    color: ${CSS_VARS.blue};
  }
`;

const Icon = styled.i`
  color: ${CSS_VARS.lighterBlue};
  font-size: 20px;
  margin-right: 20px;
  transition: ${CSS_VARS.durationNormal} all;
  cursor: pointer;

  &:hover,
  &.is-active {
    transition: ${CSS_VARS.durationNormal} all;
    color: ${CSS_VARS.blue};
  }

  &:last-child {
    border-right: none;
    margin-right: 0;
  }
`;

export { Wrapper, NewNodeBtn, Table, Link, Icon };
