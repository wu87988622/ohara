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

import React from 'react';
import PropTypes from 'prop-types';
import TableRow from '@material-ui/core/TableRow';
import { divide ,floor} from 'lodash';

import OverviewTable from './OverviewTable';
import { useFetchJars } from '../WorkspacesDetailPageUtils';
import {
  TabHeading,
  StyledTableCell,
  StyledIcon,
  StyledIconLink,
} from './styles';

const OverviewStreamApps = props => {
  const { handleRedirect, workerName } = props;
  const { jars, loading: fetchingJars } = useFetchJars(workerName);

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
        isLoading={fetchingJars}
      >
        {() => {
          return jars.map(jar => {
            return (
              <TableRow key={jar.name}>
                <StyledTableCell>{jar.name}</StyledTableCell>
                <StyledTableCell align="right" >
                {floor(divide(jar.size,1024),2)}
                </StyledTableCell>
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
