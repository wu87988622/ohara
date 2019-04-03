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
import ReactTooltip from 'react-tooltip';
import { join } from 'lodash';

import WorkerNewModal from '../WorkerNewModal';
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import { FormGroup } from 'common/Form';
import { primaryBtn } from 'theme/btnTheme';
import { TableLoader } from 'common/Loader';
import { workersPropType } from 'propTypes/services';
import * as s from './styles';

class WorkerListPage extends React.Component {
  static propTypes = {
    workers: PropTypes.arrayOf(workersPropType).isRequired,
    newWorkerSuccess: PropTypes.func.isRequired,
    isLoading: PropTypes.bool,
  };

  headers = ['SERVICES', 'NODES', 'TOPICS'];

  state = {
    isModalOpen: false,
    isBtnDisabled: false,
  };

  componentDidMount() {
    if (this.props.workers.length >= 1) {
      this.setState({ isBtnDisabled: true });
    }
  }

  componentDidUpdate(prevProps) {
    const prevWorkerLen = prevProps.workers.length;
    const nextWorkerLen = this.props.workers.length;

    const isUpdate = prevWorkerLen !== nextWorkerLen;
    if (isUpdate && nextWorkerLen >= 1) {
      this.setState({ isBtnDisabled: true });
    }
  }

  handleModalOpen = () => {
    this.setState({ isModalOpen: true });
  };

  handleModalClose = () => {
    this.setState({ isModalOpen: false });
  };

  render() {
    const { workers, newWorkerSuccess, isLoading } = this.props;
    const { isModalOpen, isBtnDisabled } = this.state;
    return (
      <React.Fragment>
        <Box>
          <FormGroup isInline>
            <H2>Services > Connect</H2>
            <s.NewClusterBtn
              theme={primaryBtn}
              text="New cluster"
              disabled={isBtnDisabled || isLoading}
              handleClick={this.handleModalOpen}
            />

            <ReactTooltip />
          </FormGroup>
          {isLoading ? (
            <TableLoader />
          ) : (
            <s.Table headers={this.headers}>
              {workers.map(
                ({
                  name,
                  nodeNames,
                  statusTopicName,
                  configTopicName,
                  offsetTopicName,
                }) => (
                  <tr key={name}>
                    <td>
                      <s.Link to={`/services/workers/${name}`}>
                        {name || ''}
                      </s.Link>
                    </td>
                    <td>{join(nodeNames, ', ')}</td>
                    <td>
                      {statusTopicName && (
                        <div>status-topic: {statusTopicName}</div>
                      )}
                      {configTopicName && (
                        <div>config-topic: {configTopicName}</div>
                      )}
                      {offsetTopicName && (
                        <div>offset-topic: {offsetTopicName}</div>
                      )}
                    </td>
                  </tr>
                ),
              )}
            </s.Table>
          )}
        </Box>
        <WorkerNewModal
          isActive={isModalOpen}
          onConfirm={newWorkerSuccess}
          onClose={this.handleModalClose}
        />
      </React.Fragment>
    );
  }
}

export default WorkerListPage;
