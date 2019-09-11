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

export const StyledHeader = styled.div`
  background-color: ${props => props.theme.palette.common.white};
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  height: 59px;
  border-bottom: 1px solid ${props => props.theme.palette.grey[200]};
  padding: 0 50px;
  z-index: 100;
`;

StyledHeader.displayName = 'StyledHeader';

export const HeaderWrapper = styled.header`
  width: 100%;
  height: 100%;
  max-width: 1200px;
  display: flex;
  align-items: center;
  margin: auto;
`;

HeaderWrapper.displayName = 'Header';

export const Brand = styled(NavLink)`
  font-family: Merriweather, sans-serif;
  color: ${props => props.theme.palette.primary.main};
  font-size: 24px;
  padding: 0;
  display: block;

  &:hover {
    color: ${props => props.theme.palette.primary.light};
  }
`;

Brand.displayName = 'Brand';

export const Nav = styled.nav`
  margin-left: 54px;
  background-color: ${props => props.theme.palette.common.white};
`;

Nav.displayName = 'Nav';

export const Link = styled(NavLink)`
  color: ${props => props.theme.palette.text.secondary};
  font-size: 14px;
  padding: 15px 0;
  margin: 10px 20px;
  position: relative;
  transition: 0.3s all;

  &:hover,
  &.active {
    color: ${props => props.theme.palette.primary.main};
  }
`;

Link.displayName = 'Link';

export const Btn = styled.button`
  border: none;
  color: ${props => props.theme.palette.grey[400]};
  font-size: 18px;
  background-color: transparent;

  &:hover,
  &.active {
    color: ${props => props.theme.palette.primary.main};
  }
`;

export const Icon = styled.i`
  margin-right: 8px;
`;

Icon.displayName = 'Icon';

export const RightCol = styled.div`
  margin-left: auto;
`;

export const Ul = styled.ul`
  padding: 22px 25px;

  li {
    margin-bottom: 15px;
    display: flex;
    align-items: center;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .item {
    margin-right: 10px;
    padding: 13px 15px;
    color: ${props => props.theme.palette.text.secondary};
    background-color: ${props => props.theme.palette.grey[100]};
  }

  .content {
    color: ${props => props.theme.palette.text.secondary};
  }

  .item,
  .content {
    font-size: 13px;
  }
`;

export const LoaderWrapper = styled.div`
  margin: 30px;
`;
