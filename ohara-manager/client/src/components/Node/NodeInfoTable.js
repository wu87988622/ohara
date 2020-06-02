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
import { map, round } from 'lodash';

import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardContent from '@material-ui/core/CardContent';
import Divider from '@material-ui/core/Divider';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableRow from '@material-ui/core/TableRow';

import { MODE } from 'const';
import NodeStateChip from './NodeStateChip';

function NodeInfoTable({ mode, node }) {
  if (!node) return null;

  return (
    <Card>
      <CardHeader title="Info" />
      <Divider />
      <CardContent>
        <Table>
          <TableBody>
            <TableRow>
              <TableCell>Hostname</TableCell>
              <TableCell>{node?.hostname}</TableCell>
            </TableRow>
            {mode === MODE.DOCKER && (
              <>
                <TableRow>
                  <TableCell>Port</TableCell>
                  <TableCell>{node?.port || 'Unknown'}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>User</TableCell>
                  <TableCell>{node?.user || 'Unknown'}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Password</TableCell>
                  <TableCell>{node?.password || 'Unknown'}</TableCell>
                </TableRow>
              </>
            )}
            {map(node?.resources, (resource) => (
              <TableRow key={resource.name}>
                <TableCell>{resource.name}</TableCell>
                <TableCell>
                  {resource.used &&
                    `${round(resource.value * resource.used, 1)} ${
                      resource.unit
                    } / `}
                  {`${round(resource.value, 1)} ${resource.unit}`}
                </TableCell>
              </TableRow>
            ))}
            <TableRow>
              <TableCell>State</TableCell>
              <TableCell>
                <NodeStateChip node={node} />
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}

NodeInfoTable.propTypes = {
  mode: PropTypes.string,
  node: PropTypes.shape({
    hostname: PropTypes.string,
    port: PropTypes.number,
    username: PropTypes.string,
    password: PropTypes.string,
    resources: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string,
        unit: PropTypes.string,
        used: PropTypes.number,
        value: PropTypes.number,
      }),
    ),
    state: PropTypes.string,
  }).isRequired,
};

export default NodeInfoTable;
