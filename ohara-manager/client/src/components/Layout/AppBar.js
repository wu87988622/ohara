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
import Tooltip from '@material-ui/core/Tooltip';
import { Link } from 'react-router-dom';
import { get } from 'lodash';

import * as workerApi from 'api/workerApi';

// Import this logo as a React component
// https://create-react-app.dev/docs/adding-images-fonts-and-files/#adding-svgs
import { ReactComponent as Logo } from './logo.svg';
import { Header, Tools, WorkspaceList, StyledNavLink } from './Styles';

// Since Mui doesn't provide a vertical AppBar, we're creating our own
// therefore, this AppBar has nothing to do with Muis
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
      <Header>
        <div className="brand">
          <Link to="/">
            <Logo width="38" height="38" />
          </Link>
        </div>
        <WorkspaceList>
          {workers.map(worker => {
            const { name } = worker.settings;
            const displayName = name.substring(0, 2).toUpperCase();

            return (
              <Tooltip
                key={name}
                title={name}
                placement="right"
                enterDelay={1000}
              >
                <StyledNavLink
                  activeClassName="active-link"
                  className="workspace-name item"
                  to={`/${name}`}
                >
                  {displayName}
                </StyledNavLink>
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
      </Header>
    </>
  );
};

export default AppBar;
