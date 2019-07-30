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
import Tooltip from '@material-ui/core/Tooltip';
import TableRow from '@material-ui/core/TableRow';
import { divide, floor, get } from 'lodash';

import OverviewTable from './OverviewTable';
import {
  TabHeading,
  StyledTableCell,
  StyledIcon,
  StyledIconLink,
} from './styles';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';

const OverviewStreamApps = props => {
  const { handleRedirect, workerName } = props;
  // const { jars, loading: fetchingJars } = useFetchJars(workerName);
  const { data: jars, loading: fetchingJars } = useApi.useFetchApi(
    `${URL.FILE_URL}?group=${workerName}`,
  );

  return (
    <>
      <TabHeading>
        <StyledIcon className="fas fa-wind" />
        <span className="title">Stream Apps</span>
        <StyledIconLink onClick={() => handleRedirect('streamapps')}>
          <Tooltip title="Link to Stream Apps page" enterDelay={1000}>
            <StyledIcon
              className="fas fa-external-link-square-alt"
              data-testid="overview-streamapps-link"
            />
          </Tooltip>
        </StyledIconLink>
      </TabHeading>

      <OverviewTable
        headers={['Jar name', 'File size(KB)']}
        isLoading={fetchingJars}
      >
        {get(jars, 'data.result', []).map(jar => {
          return (
            <TableRow key={jar.name}>
              <StyledTableCell>{jar.name}</StyledTableCell>
              <StyledTableCell align="right">
                {floor(divide(jar.size, 1024), 1)}
              </StyledTableCell>
            </TableRow>
          );
        })}
      </OverviewTable>
    </>
  );
};

OverviewStreamApps.propTypes = {
  handleRedirect: PropTypes.func.isRequired,
  workerName: PropTypes.string.isRequired,
};

export default OverviewStreamApps;
