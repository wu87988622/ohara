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
import Tooltip from '@material-ui/core/Tooltip';

import { CONNECTOR_FILTERS } from 'constants/pipelines';
import OverviewTable from './OverviewTable';
import { TabHeading, StyledTableCell, StyledIcon, TooltipBody } from './styles';

const OverviewConnectors = props => {
  const { connectors } = props;
  const _connectors = connectors
    // filter out connectors that are not yet supported by UI
    .filter(({ className }) => !CONNECTOR_FILTERS.includes(className))
    .map(connector => {
      const { className, definitions: defs } = connector;
      const targetKeys = ['kind', 'version', 'author'];
      const displayInfo = defs
        .filter(def => targetKeys.includes(def.key))
        .reduce((acc, { key, defaultValue }) => {
          acc[key] = defaultValue;
          return acc;
        }, {});

      return {
        name: className.split('.').pop(),
        info: { ...displayInfo, className },
      };
    });
  return (
    <>
      <TabHeading>
        <StyledIcon className="fas fa-plug" />
        <span className="title">Connectors</span>
      </TabHeading>

      <OverviewTable headers={['Name', 'More info']} isLoading={false}>
        {_connectors.map(connector => {
          const { name, info } = connector;
          const { author, kind, version, className } = info;
          return (
            <TableRow key={name}>
              <StyledTableCell component="td" scope="row">
                {name}
              </StyledTableCell>
              <StyledTableCell component="td" scope="row" align="right">
                <Tooltip
                  interactive
                  placement="right"
                  data-testid={`${name}-tooltip`}
                  title={
                    <TooltipBody>
                      <li>
                        <b>Author:</b>
                        <span data-testid="author-value">{author}</span>
                      </li>
                      <li>
                        <b>Kind:</b>
                        <span data-testid="kind-value">{kind}</span>
                      </li>
                      <li>
                        <b>Version:</b>
                        <span data-testid="version-value">{version}</span>
                      </li>
                      <li>
                        <b>Class:</b>
                        <span data-testid="class-value">{className}</span>
                      </li>
                    </TooltipBody>
                  }
                >
                  <StyledIcon className="fas fa-info-circle" size="13" />
                </Tooltip>
              </StyledTableCell>
            </TableRow>
          );
        })}
      </OverviewTable>
    </>
  );
};

OverviewConnectors.propTypes = {
  connectors: PropTypes.arrayOf(
    PropTypes.shape({
      className: PropTypes.string.isRequired,
      definitions: PropTypes.arrayOf(
        PropTypes.shape({
          key: PropTypes.string.isRequired,
          defaultValue: PropTypes.string,
        }),
      ).isRequired,
    }),
  ).isRequired,
};

export default OverviewConnectors;
