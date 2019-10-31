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

export const Header = styled.header`
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 70px;
  min-width: 70px;
  background: ${props => props.theme.palette.primary[900]};

  .brand {
    margin-top: ${props => props.theme.spacing(2)}px;
    margin-bottom: ${props => props.theme.spacing(3)}px;
  }
`;

export const WorkspaceList = styled.ul`
  display: flex;
  align-items: center;
  flex-direction: column;

  .current {
    .item {
      color: ${props => props.theme.palette.grey[400]};
      background-color: ${props => props.theme.palette.grey[300]};
    }
  }

  .item {
    margin-bottom: ${props => props.theme.spacing(2)}px;
    border-radius: ${props => props.theme.shape.borderRadius}px;
    background-color: ${props => props.theme.palette.grey[100]};
    color: ${props => props.theme.palette.grey[500]};
    font-size: 20px;
    width: 40px;
    height: 40px;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
  }

  .add-workspace {
    background-color: transparent;
    border: 1px solid ${props => props.theme.palette.common.white};
    color: ${props => props.theme.palette.common.white};
    display: flex;
    align-items: center;
    justify-content: center;
  }
`;

export const Tools = styled.ul`
  /* This moves tools to the bottom of the bar */
  margin-top: auto;
  display: flex;
  flex-direction: column;
  font-size: 20px;
  align-items: center;
  width: 100%;
  color: ${props => props.theme.palette.common.white};

  .item {
    margin-bottom: ${props => props.theme.spacing(2)}px;
    cursor: pointer;

    &:hover {
      opacity: 0.9;
    }
  }
`;

export const StyledNavLink = styled(NavLink)`
  text-decoration: none;
  color: ${props => props.theme.palette.grey[400]};

  &.active-link {
    border: 2px solid ${props => props.theme.palette.primary[400]};
  }
`;
