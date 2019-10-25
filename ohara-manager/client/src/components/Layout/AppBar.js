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

import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import Tooltip from '@material-ui/core/Tooltip';
import { Link } from 'react-router-dom';
import { get } from 'lodash';

// Import this logo as a React component
// https://create-react-app.dev/docs/adding-images-fonts-and-files/#adding-svgs
import { ReactComponent as Logo } from './logo.svg';
import * as workerApi from 'api/workerApi';

const StyledHeader = styled.header`
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 70px;
  background: ${props => props.theme.palette.primary[900]};

  .brand {
    margin-top: ${props => props.theme.spacing(2)}px;
    margin-bottom: ${props => props.theme.spacing(3)}px;
  }
`;

const WorkspaceList = styled.header`
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
    border: 1px solid ${props => props.theme.palette.common.white};
    background-color: transparent;
    color: ${props => props.theme.palette.common.white};
  }
`;

const Tools = styled.ul`
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

  .workspace {
    width: 100%;
    text-align: center;
    /* We don't have this color #1b4778 in our theme */
    border-bottom: 1px solid #1b4778;
    padding-bottom: ${props => props.theme.spacing(2)}px;
  }
`;

// Since Mui doesn't provide a vertical AppBar, we're creating our own
// therefore, this AppBar has nothing to do with Mui's
const AppBar = () => {
  const [workers, setWorkers] = useState([]);

  useEffect(() => {
    const fetchWorkers = async () => {
      const response = await workerApi.fetchWorkers();
      const workers = get(response, 'data.result', []);
      // Sort by alphabetical order of the worker name
      const sortedWorkers = workers.sort((a, b) =>
        a.settings.name.localeCompare(b.settings.name),
      );

      setWorkers(sortedWorkers);
    };

    fetchWorkers();
  }, []);

  return (
    <>
      <StyledHeader>
        <div className="brand">
          <Link to="/">
            <Logo width="38" height="38" />
          </Link>
        </div>
        <WorkspaceList>
          {workers.map(worker => {
            const { name } = worker.settings;

            // TODO: replace this with real data
            const current = name === 'abc' ? 'current' : '';
            const displayName = name.substring(0, 2).toUpperCase();

            return (
              <Tooltip
                key={name}
                title={name}
                placement="right"
                enterDelay={1000}
              >
                <li className={`${current} item`}>
                  <div className="workspace-name">{displayName}</div>
                </li>
              </Tooltip>
            );
          })}

          <Tooltip
            title="Create a new workspace"
            placement="right"
            enterDelay={1000}
          >
            <i className="add-workspace item fas fa-plus"></i>
          </Tooltip>
        </WorkspaceList>

        <Tools>
          <i className="workspace item fas fa-th"></i>
          <i className="fas item fa-envelope-open-text"></i>
          <i className="fas item fa-code"></i>
          <i className="fas item fa-server"></i>
        </Tools>
      </StyledHeader>
    </>
  );
};

export default AppBar;
