import styled from 'styled-components';
import { Link } from 'react-router-dom';

import { Button } from 'common/Form';
import { DataTable } from 'common/Table';
import * as CSS_VARS from 'theme/variables';

export const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

export const TopWrapper = styled.div`
  margin-bottom: 20px;
  display: flex;
  align-items: center;
`;

export const NewNodeBtn = styled(Button)`
  margin-left: auto;
`;

NewNodeBtn.displayName = 'NewNodeBtn';

export const TestConnectionBtn = styled(Button)`
  margin-right: auto;
`;

TestConnectionBtn.displayName = 'TestConnectionBtn';

export const Table = styled(DataTable)`
  text-align: left;

  .is-running {
    background: ${CSS_VARS.trBgColor};
  }
`;

Table.displayName = 'Table';

export const LinkIcon = styled(Link)`
  color: ${CSS_VARS.lightBlue};

  &:hover {
    color: ${CSS_VARS.blue};
  }
`;

LinkIcon.displayName = 'LinkIcon';

export const Icon = styled.i`
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

Icon.displayName = 'Icon';
