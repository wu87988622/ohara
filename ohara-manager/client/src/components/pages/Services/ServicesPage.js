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
import { isEmpty, sortBy, get } from 'lodash';

import * as workerApi from 'api/workerApi';
import BrokerListPage from './BrokerListPage';
import ZookeeperListPage from './ZookeeperListPage';
import WorkerListPage from './WorkerListPage';
import WorkerDetailPage from './WorkerDetailPage';
import { SERVICES } from 'constants/documentTitles';
import { Layout, Link, SubLink } from './Styles';

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
    const res = await workerApi.fetchWorkers();
    const workers = get(res, 'data.result', []);
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
    const { workers } = this.state;
    const baseUrl = '/services';

    if (match.url === baseUrl) {
      return <Redirect to={`${baseUrl}/${WORKERS}`} />;
    }

    return (
      <DocumentTitle title={SERVICES}>
        <Layout>
          <div className="sidebar">
            <Link to={`${baseUrl}/${BROKERS}`}>Broker</Link>
            <Link to={`${baseUrl}/${ZOOKEEPERS}`}>Zookeeper</Link>
            <Link to={`${baseUrl}/${WORKERS}`}>Connect</Link>
            {workers &&
              workers.map(({ name }) => (
                <SubLink key={name} to={`${baseUrl}/${WORKERS}/${name}`}>
                  {name}
                </SubLink>
              ))}
          </div>
          <div className="main">{this.switchComponent()}</div>
        </Layout>
      </DocumentTitle>
    );
  }
}

export default ServicesPage;
