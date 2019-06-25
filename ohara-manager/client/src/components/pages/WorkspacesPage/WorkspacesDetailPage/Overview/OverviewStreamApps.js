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
import PropTypes from 'prop-types';
import TableRow from '@material-ui/core/TableRow';
import { get, isEmpty } from 'lodash';

import * as streamApi from 'api/streamApi';
import OverviewTable from './OverviewTable';
import {
  TabHeading,
  StyledTableCell,
  StyledIcon,
  StyledIconLink,
} from './styles';

const OverviewStreamApps = props => {
  const { handleRedirect, workerName } = props;

  const [jars, setJars] = useState([]);
  const [isFetchingJars, setIsFetchingJars] = useState(true);

  useEffect(() => {
    const fetchJars = async () => {
      const res = await streamApi.fetchJars(workerName);
      const jars = get(res, 'data.result', []);
      setIsFetchingJars(false);

      if (!isEmpty(jars)) {
        setJars(jars);
      }
    };

    fetchJars();
  }, [workerName]);

  return (
    <>
      <TabHeading>
        <StyledIcon className="fas fa-wind" />
        <span className="title">Stream Apps</span>
        <StyledIconLink onClick={() => handleRedirect('streamapps')}>
          <StyledIcon className="fas fa-external-link-square-alt" />
        </StyledIconLink>
      </TabHeading>

      <OverviewTable
        headers={['Jar name', 'File size(KB)']}
        isLoading={isFetchingJars}
      >
        {() => {
          return jars.map(jar => {
            return (
              <TableRow key={jar.name}>
                <StyledTableCell component="th" scope="row">
                  {jar.name}
                </StyledTableCell>
                <StyledTableCell align="left" />
              </TableRow>
            );
          });
        }}
      </OverviewTable>
    </>
  );
};

OverviewStreamApps.propTypes = {
  handleRedirect: PropTypes.func.isRequired,
  workerName: PropTypes.string.isRequired,
};

export default OverviewStreamApps;
