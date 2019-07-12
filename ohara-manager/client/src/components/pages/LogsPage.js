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
import styled from 'styled-components';
import { get, isEmpty, split, map, includes } from 'lodash';

import * as logApi from 'api/logApi';
import NotFoundPage from 'components/pages/NotFoundPage';
import { TableLoader } from 'components/common/Loader';
import { Box } from 'components/common/Layout';
import { H2 } from 'components/common/Headings';
import { LOGS } from 'constants/documentTitles';

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

const Line = styled.div`
  color: ${props => props.theme.lightBlue};
  line-height: 1.6;
  font-size: 13px;
`;

class LogsPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      params: PropTypes.object.isRequired,
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
    includes(['zookeepers', 'brokers', 'workers'], serviceName);

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
      <DocumentTitle title={LOGS}>
        <>
          <Wrapper>
            <TopWrapper>
              <H2>Error log of cluster {clusterName}</H2>
            </TopWrapper>
            <Box>
              {isLoading ? (
                <TableLoader />
              ) : (
                <>
                  {map(logLines, (logLine, i) => (
                    <Line key={i}>{logLine}</Line>
                  ))}
                </>
              )}
            </Box>
          </Wrapper>
        </>
      </DocumentTitle>
    );
  }
}

export default LogsPage;
