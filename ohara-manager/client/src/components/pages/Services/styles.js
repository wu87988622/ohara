/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import styled from 'styled-components';
import { NavLink } from 'react-router-dom';
import { DataTable } from 'common/Table';

const Layout = styled.div`
  display: flex;
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;

  .main {
    margin: 20px 0;
    padding-left: 30px;
    width: 100%;
  }
`;

const Link = styled(NavLink)`
  color: ${props => props.theme.dimBlue};
  font-size: 14px;
  padding: 0;
  margin: 0.5rem 1rem 0.5rem 0;
  position: relative;
  transition: 0.3s all;
  margin: 1.5rem 0;

  &:hover,
  &.active {
    color: ${props => props.theme.blue};
  }

  display: block;
  font-weight: 700;
`;

const SubLink = styled(Link)`
  margin: 1rem 1rem 1rem 2rem;
  font-weight: 400;
`;

const Table = styled(DataTable)`
  text-align: left;

  .is-running {
    background: ${props => props.theme.trBgColor};
  }
`;

export { Layout, Link, SubLink, Table };
