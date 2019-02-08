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
    min-height: calc(100% - 100px);

    & > div:not(:first-child) {
      margin: 1rem 0;
      padding: 0 2rem;
      width: 100%;
    }
  }
`;

const Link = styled(NavLink)`
  color: ${props => props.theme.dimBlue};
  font-size: 14px;
  padding: 0;
  margin: 0.5rem 1rem 0.5rem 0;
  position: relative;
  transition: 0.3s all;

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

const Divider = styled.div`
  margin: 1.5rem 0;
`;

export { Layout, Link, SubLink, Divider };
