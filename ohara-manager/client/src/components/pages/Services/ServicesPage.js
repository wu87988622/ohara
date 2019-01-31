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
import { Redirect } from 'react-router-dom';
import { isEmpty, sortBy } from 'lodash';

import * as workerApis from 'apis/workerApis';
import * as _ from 'utils/commonUtils';
import { SERVICES } from 'constants/documentTitles';

import BrokerListPage from './BrokerListPage';
import ZookeeperListPage from './ZookeeperListPage';
import WorkerListPage from './WorkerListPage';
import WorkerDetailPage from './WorkerDetailPage';
import * as s from './Styles';

const BROKERS = 'brokers';
const ZOOKEEPERS = 'zookeepers';
const WORKERS = 'workers';

class ServicesPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
  };

  state = {
    isLoading: true,
    workers: [],
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const res = await workerApis.fetchWorkers();
    const workers = _.get(res, 'data.result', []);
    this.setState(() => ({ isLoading: false }));
    if (!isEmpty(workers)) {
      this.setState({ workers: sortBy(workers, 'name') });
    }
  };

  refreshData = () => {
    this.setState({ isLoading: true }, () => {
      this.fetchData();
    });
  };

  switchComponent = () => {
    const { match } = this.props;
    const { params } = match;
    const { serviceName, clusterName } = params;
    const { workers, isLoading } = this.state;

    switch (serviceName) {
      case BROKERS:
        return <BrokerListPage />;
      case ZOOKEEPERS:
        return <ZookeeperListPage />;
      case WORKERS:
        if (clusterName) {
          return <WorkerDetailPage workers={workers} name={clusterName} />;
        }
        return (
          <WorkerListPage
            workers={workers}
            isLoading={isLoading}
            newWorkerSuccess={this.refreshData}
          />
        );
      default:
        return null;
    }
  };

  render() {
    const { match } = this.props;
    if (match.url === '/services') {
      return <Redirect to={`/services/${WORKERS}`} />;
    }

    const { workers } = this.state;

    return (
      <DocumentTitle title={SERVICES}>
        <React.Fragment>
          <s.Layout>
            <div>
              <div>
                <s.Link to={`/services/${BROKERS}`}>Broker</s.Link>
                <s.Divider />
                <s.Link to={`/services/${ZOOKEEPERS}`}>Zookeeper</s.Link>
                <s.Divider />
                <s.Link to={`/services/${WORKERS}`}>Connect</s.Link>
                {workers &&
                  workers.map(worker => (
                    <s.SubLink
                      key={worker.name}
                      to={`/services/${WORKERS}/${worker.name}`}
                    >
                      {worker.name}
                    </s.SubLink>
                  ))}
              </div>
              <div>{this.switchComponent()}</div>
            </div>
          </s.Layout>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default ServicesPage;
