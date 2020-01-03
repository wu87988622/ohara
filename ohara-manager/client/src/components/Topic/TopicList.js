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

import React, { useMemo, useState } from 'react';
import { get, filter, isEmpty, map } from 'lodash';

import Checkbox from '@material-ui/core/Checkbox';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Grid from '@material-ui/core/Grid';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

import { useViewTopicDialog } from 'context';
import { Table } from 'components/common/Table';
import { Button } from 'components/common/Form';
import { QuickSearch } from 'components/common/Search';
import ViewTopicDialog from './ViewTopicDialog';
import { useTopics } from './hooks';
import { Wrapper } from './TopicListStyles';
import TopicChip from './TopicChip';
import { someByKey } from 'utils/object';

const CHECKBOXES = {
  SHARED: 'shared',
};

function TopicList() {
  const { open: openViewTopicDialog } = useViewTopicDialog();
  const topics = useTopics();
  const [checked, setChecked] = useState({ [CHECKBOXES.SHARED]: false });
  const [searchResult, setSearchResult] = useState([]);

  const filteredTopics = useMemo(() => {
    if (isEmpty(topics)) return;

    const predicateBySearch = topic => someByKey(searchResult, topic);
    const predicateByCheckbox = topic =>
      !checked[CHECKBOXES.SHARED] || topic.isShared;

    return filter(
      topics,
      topic => predicateBySearch(topic) && predicateByCheckbox(topic),
    );
  }, [topics, checked, searchResult]);

  const handleSearchChange = result => {
    setSearchResult(result);
  };

  const handleCheckboxChange = name => event => {
    setChecked({ ...checked, [name]: event.target.checked });
  };

  const tableHeaders = [
    'Name',
    'Partitions',
    'Replications',
    // Completed in the next version, so hide it first
    // 'Used by pipelines',
    'Type',
    'State',
    'Actions',
  ];
  return (
    <Wrapper>
      <Grid container className="filters">
        <QuickSearch
          data={topics}
          keys={['name', 'tags.type', 'state']}
          setResults={handleSearchChange}
        />
        <FormGroup row className="checkboxes">
          <FormControlLabel
            control={
              <Checkbox
                checked={checked[CHECKBOXES.SHARED]}
                onChange={handleCheckboxChange(CHECKBOXES.SHARED)}
                value={CHECKBOXES.SHARED}
                color="primary"
              />
            }
            label="Shared only"
          />
        </FormGroup>
      </Grid>
      <Table headers={tableHeaders} title="All Topics">
        {map(filteredTopics, topic => {
          return (
            <TableRow key={get(topic, 'name')}>
              <TableCell>{get(topic, 'displayName')}</TableCell>
              <TableCell>{get(topic, 'numberOfPartitions', 0)}</TableCell>
              <TableCell>{get(topic, 'numberOfReplications', 0)}</TableCell>
              {/* Completed in the next version, so hide it first */}
              {/* <TableCell></TableCell> */}
              <TableCell>
                <TopicChip isShared={topic.isShared} />
              </TableCell>
              <TableCell>{get(topic, 'state', 'Unknown')}</TableCell>
              <TableCell align="right">
                <Button
                  variant="outlined"
                  color="primary"
                  onClick={() => {
                    openViewTopicDialog(topic);
                  }}
                >
                  View
                </Button>
              </TableCell>
            </TableRow>
          );
        })}
      </Table>
      <ViewTopicDialog />
    </Wrapper>
  );
}

export default TopicList;
