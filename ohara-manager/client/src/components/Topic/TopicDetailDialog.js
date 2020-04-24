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
import styled, { css } from 'styled-components';
import { get, flatten, map } from 'lodash';

import Grid from '@material-ui/core/Grid';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardContent from '@material-ui/core/CardContent';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import Divider from '@material-ui/core/Divider';
import NumberFormat from 'react-number-format';

import { Dialog } from 'components/common/Dialog';
import TopicChip from './TopicChip';

const Wrapper = styled.div(
  ({ theme }) => css`
    .MuiCardContent-root {
      padding: 0;
    }

    .MuiTableRow-root:nth-child(2n) {
      background-color: ${theme.palette.grey[100]};
    }
  `,
);

const TopicDetailDialog = ({ isOpen, onClose, topic }) => {
  if (!topic) return null;

  const isShared = topic?.tags?.isShared;
  const displayName = isShared ? topic?.name : topic?.tags?.displayName;

  return (
    <Dialog
      open={isOpen}
      onClose={onClose}
      maxWidth="md"
      showActions={false}
      testId="view-topic-detail-dialog"
      title="View topic"
    >
      <Wrapper>
        <Grid container spacing={3}>
          <Grid item xs={4}>
            <Card>
              <CardHeader title="Information" />
              <Divider />
              <CardContent>
                <Table>
                  <TableBody>
                    <TableRow>
                      <TableCell>Name</TableCell>
                      <TableCell>{displayName}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Partitions</TableCell>
                      <TableCell>
                        {get(topic, 'numberOfPartitions', 0)}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Replications</TableCell>
                      <TableCell>
                        {get(topic, 'numberOfReplications', 0)}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Type</TableCell>
                      <TableCell>
                        <TopicChip isShared={isShared} />
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>State</TableCell>
                      <TableCell>{get(topic, 'state', 'Unknown')}</TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={8}>
            <Card>
              <CardHeader title="Metrics" />
              <Divider />
              <CardContent>
                <Table>
                  <TableBody>
                    {/* we need to display the metrics by each hostname
                        https://github.com/oharastream/ohara/issues/4495
                    */}
                    {flatten(
                      map(
                        get(topic, 'nodeMetrics', {}),
                        nodeMetric => nodeMetric.meters,
                      ),
                    ).map(metric => {
                      const document = get(metric, 'document');
                      const value = get(metric, 'value');
                      const unit = get(metric, 'unit');
                      return (
                        <TableRow>
                          <TableCell>{document}</TableCell>
                          <TableCell align="right">
                            <NumberFormat
                              value={value}
                              displayType="text"
                              thousandSeparator
                              renderText={value => (
                                <div>
                                  {value} {unit}
                                </div>
                              )}
                            />
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Wrapper>
    </Dialog>
  );
};

TopicDetailDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  topic: PropTypes.object,
};

TopicDetailDialog.defaultProps = {
  onClose: () => {},
};

export default TopicDetailDialog;
