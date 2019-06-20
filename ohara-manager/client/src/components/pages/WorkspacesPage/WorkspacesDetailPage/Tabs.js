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

import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import Paper from '@material-ui/core/Paper';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import { Route } from 'react-router-dom';

import Topics from './Topics';
import StreamApp from './StreamApp';

const StyledTabs = styled(Tabs)`
  border-bottom: 1px solid #eee;
`;

const MuiTabs = props => {
  const [value, setValue] = React.useState(0);
  const tabArray = ['overview', 'nodes', 'topics', 'streamapps'];
  const { workspaceName } = props.match.params;
  const baseUrl = `/workspaces/${workspaceName}`;

  const handleChange = (e, newValue) => {
    // map newValue to different path: e.g. 0 = overview, 1 = nodes
    const activeTab = tabArray[newValue];
    setValue(newValue);
    props.history.push(`${baseUrl}/${activeTab}`);
  };

  useEffect(() => {
    const { serviceName } = props.match.params;
    const activeTabIdx = tabArray.findIndex(t => t === serviceName);
    setValue(activeTabIdx);
  }, [props.match.params, tabArray]);

  return (
    <Paper square>
      <StyledTabs
        value={value}
        indicatorColor="primary"
        textColor="primary"
        onChange={handleChange}
      >
        <Tab label="Overview" />
        <Tab label="Nodes" />
        <Tab label="Topics" />
        <Tab label="Stream apps" />
      </StyledTabs>
      <Route path={`${baseUrl}/overview`} render={() => <h5>Overview</h5>} />
      <Route path={`${baseUrl}/nodes`} render={() => <h5>Nodes</h5>} />
      <Route path={`${baseUrl}/topics`} component={Topics} />
      <Route
        path={`${baseUrl}/streamapps`}
        render={() => <StreamApp workspaceName={workspaceName} />}
      />
    </Paper>
  );
};

MuiTabs.propTypes = {
  match: PropTypes.shape({
    url: PropTypes.string.isRequired,
    params: PropTypes.shape({
      workspaceName: PropTypes.string.isRequired,
    }),
  }).isRequired,
  history: PropTypes.shape({
    push: PropTypes.func.isRequired,
  }).isRequired,
};

export default MuiTabs;
