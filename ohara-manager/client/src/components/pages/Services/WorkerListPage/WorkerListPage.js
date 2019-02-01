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

import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import { FormGroup } from 'common/Form';
import { primaryBtn } from 'theme/btnTheme';
import { TableLoader } from 'common/Loader';

import WorkerNewModal from '../WorkerNewModal';
import * as s from './Styles';

class WorkerListPage extends React.Component {
  static propTypes = {
    workers: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        nodeNames: PropTypes.arrayOf(PropTypes.string).isRequired,
        statusTopicName: PropTypes.string.isRequired,
        configTopicName: PropTypes.string.isRequired,
        offsetTopicName: PropTypes.string.isRequired,
      }),
    ).isRequired,
    newWorkerSuccess: PropTypes.func.isRequired,
    isLoading: PropTypes.bool,
  };

  headers = ['SERVICES', 'NODES', 'TOPICS'];

  state = {
    isModalOpen: false,
    isNewClusterBtnDisabled: false,
  };

  componentDidMount() {
    if (this.props.workers.length >= 1) {
      this.setState({ isNewClusterBtnDisabled: true });
    }
  }

  componentDidUpdate(prevProps) {
    const prevWorkerLen = prevProps.workers.length;
    const nextWorkerLen = this.props.workers.length;

    const isUpdate = prevWorkerLen !== nextWorkerLen;
    if (isUpdate && nextWorkerLen >= 1) {
      this.setState({
        isNewClusterBtnDisabled: true,
      });
    }
  }

  render() {
    const { workers, newWorkerSuccess, isLoading } = this.props;
    const { isModalOpen, isNewClusterBtnDisabled } = this.state;
    return (
      <React.Fragment>
        <Box>
          <FormGroup isInline>
            <H2>Services > Connect</H2>
            <s.TooltipWrapper
              data-tip={
                isNewClusterBtnDisabled
                  ? 'You cannot create more than one cluster'
                  : undefined
              }
            >
              <s.NewClusterBtn
                theme={primaryBtn}
                text="New cluster"
                disabled={isNewClusterBtnDisabled}
                handleClick={() => {
                  this.setState({ isModalOpen: true });
                }}
              />
            </s.TooltipWrapper>

            <ReactTooltip />
          </FormGroup>
          {isLoading ? (
            <TableLoader />
          ) : (
            <s.Table headers={this.headers}>
              {workers.map(worker => (
                <tr key={worker.name}>
                  <td>
                    <s.Link to={`/services/workers/${worker.name}`}>
                      {worker.name || ''}
                    </s.Link>
                  </td>
                  <td>{join(worker.nodeNames, ', ')}</td>
                  <td>
                    {worker.statusTopicName && (
                      <div>status-topic: {worker.statusTopicName}</div>
                    )}
                    {worker.configTopicName && (
                      <div>config-topic: {worker.configTopicName}</div>
                    )}
                    {worker.offsetTopicName && (
                      <div>offset-topic: {worker.offsetTopicName}</div>
                    )}
                  </td>
                </tr>
              ))}
            </s.Table>
          )}
        </Box>
        <WorkerNewModal
          isActive={isModalOpen}
          onClose={() => {
            this.setState({ isModalOpen: false });
          }}
          onConfirm={newWorkerSuccess}
        />
      </React.Fragment>
    );
  }
}

export default WorkerListPage;
