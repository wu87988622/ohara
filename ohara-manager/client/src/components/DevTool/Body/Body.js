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

import { List } from 'react-virtualized/dist/commonjs/List';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';

import { TableLoader } from 'components/common/Loader';
import TopicDataTable from './TopicDataTable';
import { tabName } from '../DevToolDialog';

const TabPanel = props => {
  const { children, value, index } = props;
  return (
    <div id="tab-body" hidden={value !== index}>
      {children}
    </div>
  );
};
TabPanel.propTypes = {
  children: PropTypes.node.isRequired,
  index: PropTypes.string.isRequired,
  value: PropTypes.node.isRequired,
};

const Body = props => {
  const { tabIndex, ...others } = props;
  const { dialogEl, data } = others;

  const renderedRow = ({ index, key, style }) => {
    return (
      <div key={key} style={style}>
        {data.hostLog[index]}
      </div>
    );
  };
  renderedRow.propTypes = {
    index: PropTypes.number.isRequired,
    key: PropTypes.string.isRequired,
    style: PropTypes.object.isRequired,
  };

  const getRowHeight = ({ index }) => {
    const { width } = dialogEl.getBoundingClientRect();
    // calculate the row height by current row length and dialog width
    // ex:
    // data length = 100
    // dialog length = 500
    // the row height will be (100 / (500 / 10)) * 30 = 60
    // Note: the percentage of the "height" is controlled by the fraction number (6)
    return Math.ceil(data.hostLog[index].length / (width / 5)) * 40;
  };

  return (
    <>
      <TabPanel value={tabIndex} index={tabName.topic}>
        <TopicDataTable
          topicData={data.topicData}
          isLoadedTopic={data.isLoading}
        />
      </TabPanel>
      <TabPanel value={tabIndex} index={tabName.log}>
        <AutoSizer>
          {({ width, height }) =>
            data.isLoading ? (
              <TableLoader />
            ) : (
              <List
                height={height}
                overscanRowCount={10}
                rowCount={data.hostLog.length}
                rowHeight={getRowHeight}
                rowRenderer={renderedRow}
                width={width}
              />
            )
          }
        </AutoSizer>
      </TabPanel>
    </>
  );
};

Body.propTypes = {
  tabIndex: PropTypes.string.isRequired,
  others: PropTypes.node,
};

export default Body;
