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
import DocumentTitle from 'react-document-title';
import { get, isEmpty, split, map, includes } from 'lodash';
import styled from 'styled-components';

import * as logApi from 'api/logApi';
import { SERVICES } from 'constants/documentTitles';
import { TableLoader } from 'common/Loader';
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import NotFoundPage from 'components/pages/NotFoundPage';

const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

export const TopWrapper = styled.div`
  margin-bottom: 20px;
  display: flex;
  align-items: center;
`;

const ZOOKEEPERS = 'zookeepers';
const BROKERS = 'brokers';
const WORKERS = 'workers';

class LogsPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
    history: PropTypes.shape({
      push: PropTypes.func,
    }).isRequired,
  };

  state = {
    serviceName: null,
    clusterName: null,
    isLoading: true,
    logs: [],
  };

  componentDidMount() {
    const { serviceName, clusterName } = this.props.match.params;
    if (this.isValidService(serviceName)) {
      this.setState({ serviceName, clusterName }, () => {
        this.fetchData();
      });
    }
  }

  isValidService = serviceName =>
    includes([ZOOKEEPERS, BROKERS, WORKERS], serviceName);

  fetchData = async () => {
    const { serviceName, clusterName } = this.state;
    const res = await logApi.fetchLogs(serviceName, clusterName);
    const logs = get(res, 'data.result.logs', []);
    if (!isEmpty(logs)) {
      this.setState({ logs });
    }
    this.setState({ isLoading: false });
  };

  render() {
    const { serviceName, clusterName, isLoading, logs } = this.state;
    if (!this.isValidService(serviceName)) {
      return <NotFoundPage />;
    }

    const logContext = get(logs, '[0].value', '');
    const logLines = split(logContext, `\n`);
    return (
      <DocumentTitle title={SERVICES}>
        <React.Fragment>
          <Wrapper>
            <TopWrapper>
              <H2>Error log of cluster {clusterName}</H2>
            </TopWrapper>
            <Box>
              {isLoading ? (
                <TableLoader />
              ) : (
                <div>
                  {map(logLines, (logLine, i) => (
                    <span key={i}>
                      {logLine}
                      <br />
                    </span>
                  ))}
                </div>
              )}
            </Box>
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default LogsPage;
