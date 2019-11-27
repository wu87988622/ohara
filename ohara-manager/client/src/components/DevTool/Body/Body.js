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
import styled from 'styled-components';

import DataTable from './DataTable';
import { tabName } from '../DevToolDialog';

const TabPanel = styled.div`
  display: ${props => (props.value !== props.index ? 'none' : 'block')};
  width: 100%;
  height: calc(100% - 48px);
  overflow: auto;
`;

const Body = props => {
  const { tabIndex, ...others } = props;
  const { data, cache } = others;

  return (
    <>
      <TabPanel value={tabIndex} index={tabName.topic}>
        <DataTable data={data} type={tabName.topic} />
      </TabPanel>
      <TabPanel value={tabIndex} index={tabName.log}>
        <DataTable data={data} type={tabName.log} cache={cache} />
      </TabPanel>
    </>
  );
};

Body.propTypes = {
  tabIndex: PropTypes.string.isRequired,
  others: PropTypes.node,
};

export default Body;
