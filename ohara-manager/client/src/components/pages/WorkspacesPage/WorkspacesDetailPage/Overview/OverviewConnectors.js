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

import OverviewTable from './OverviewTable';
import { TabHeading, StyledTableCell, StyledIcon, TooltipBody } from './styles';

const OverviewConnectors = props => {
  const { connectors } = props;
  const _connectors = connectors.map(connector => {
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
        {() => {
          return _connectors.map(connector => {
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
                    title={
                      <TooltipBody>
                        <li>
                          <b>Author:</b> {author}
                        </li>
                        <li>
                          <b>Kind:</b> {kind}
                        </li>
                        <li>
                          <b>Version:</b> {version}
                        </li>
                        <li>
                          <b>Class:</b> {className}
                        </li>
                      </TooltipBody>
                    }
                  >
                    <StyledIcon className="fas fa-info-circle" size="13" />
                  </Tooltip>
                </StyledTableCell>
              </TableRow>
            );
          });
        }}
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
