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
import toastr from 'toastr';
import { Link } from 'react-router-dom';

import * as _ from 'utils/commonUtils';
import * as MESSAGES from 'constants/messages';
import { TableLoader } from 'common/Loader';
import { ConfirmModal } from 'common/Modal';
import { DataTable } from 'common/Table';
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import { Button } from 'common/Form';
import { primaryBtn } from 'theme/btnTheme';
import { PIPELINE } from 'constants/documentTitles';
import { lightBlue, blue, red, trBgColor } from 'theme/variables';
import { addPipelineStatus, getEditUrl } from 'utils/pipelineListPageUtils';
import {
  fetchPipelines,
  createPipeline,
  deletePipeline,
} from 'apis/pipelinesApis';

const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

const TopWrapper = styled.div`
  margin-bottom: 20px;
  display: flex;
  align-items: center;
`;

const NewPipelineBtn = styled(Button)`
  margin-left: auto;
`;

NewPipelineBtn.displayName = 'NewPipelineBtn';

const Table = styled(DataTable)`
  text-align: center;

  .is-running {
    background: ${trBgColor};
  }
`;

Table.displayName = 'Table';

const LinkIcon = styled(Link)`
  color: ${lightBlue};

  &:hover {
    color: ${blue};
  }
`;

LinkIcon.displayName = 'LinkIcon';

const DeleteIcon = styled.button`
  color: ${lightBlue};
  border: 0;
  font-size: 20px;
  cursor: pointer;
  background-color: transparent;

  &:hover {
    color: ${red};
  }
`;

DeleteIcon.displayName = 'DeleteIcon';

class PipelineListPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
  };

  headers = ['name', 'cluster', 'status', 'edit', 'delete'];
  state = {
    isSelectTopicModalActive: false,
    isDeletePipelineModalActive: false,
    isLoading: true,
    pipelines: [],
    currentTopic: {},
  };

  componentDidMount() {
    this.fetchPipelines();
  }

  fetchPipelines = async () => {
    const res = await fetchPipelines();
    const result = _.get(res, 'data.result', null);
    this.setState({ isLoading: false });

    if (result) {
      const pipelines = addPipelineStatus(result);
      this.setState({ pipelines });
    }
  };

  handleSelectChange = ({ target }) => {
    const selectedIdx = target.options.selectedIndex;
    const { id } = target.options[selectedIdx].dataset;

    this.setState({
      currentTopic: {
        name: target.value,
        id,
      },
    });
  };

  handleNewPipeline = async () => {
    const { history, match } = this.props;
    const params = { name: 'Untitled pipeline', rules: {} };
    const res = await createPipeline(params);

    const pipelineId = _.get(res, 'data.result.id', null);

    if (pipelineId) {
      history.push(`${match.url}/new/${pipelineId}`);
    }
  };

  handleDeletePipelineModalOpen = id => {
    this.setState({
      isDeletePipelineModalActive: true,
      deletePipelineId: id,
    });
  };

  handleDeletePipelineModalClose = () => {
    this.setState({
      isDeletePipelineModalActive: false,
      deletePipelineId: '',
    });
  };

  handleDeletePipelineConfirm = async () => {
    const { deletePipelineId: id } = this.state;
    const res = await deletePipeline(id);
    const deletedId = _.get(res, 'data.result.id', null);
    const deletedPipeline = _.get(res, 'data.result.name', null);

    if (deletedId) {
      this.setState(({ pipelines }) => {
        const _pipelines = pipelines.filter(p => p.id !== deletedId);
        return {
          pipelines: _pipelines,
          isDeletePipelineModalActive: false,
          deletePipelineId: '',
        };
      });
      toastr.success(
        `${MESSAGES.PIPELINE_DELETION_SUCCESS} ${deletedPipeline}`,
      );
    } else {
      toastr.error(`${MESSAGES.PIPELINE_DELETION_ERROR} ${deletedPipeline}`);
    }
  };

  render() {
    const { match } = this.props;
    const { isDeletePipelineModalActive, pipelines, isLoading } = this.state;

    return (
      <DocumentTitle title={PIPELINE}>
        <React.Fragment>
          <ConfirmModal
            isActive={isDeletePipelineModalActive}
            title="Delete pipeline?"
            confirmBtnText="Yes, Delete this pipeline"
            cancelBtnText="No, Keep it"
            handleCancel={this.handleDeletePipelineModalClose}
            handleConfirm={this.handleDeletePipelineConfirm}
            message="Are you sure you want to delete this pipeline? This action cannot be redo!"
            isDelete
          />

          <Wrapper>
            <TopWrapper>
              <H2>Pipelines</H2>
              <NewPipelineBtn
                theme={primaryBtn}
                text="New pipeline"
                data-testid="new-pipeline"
                handleClick={this.handleNewPipeline}
              />
            </TopWrapper>
            <Box>
              {isLoading ? (
                <TableLoader />
              ) : (
                <Table headers={this.headers}>
                  {pipelines.map(pipeline => {
                    const { id, name, status, workerClusterName } = pipeline;
                    const isRunning = status === 'Running' ? true : false;
                    const trCls = isRunning ? 'is-running' : '';
                    const editUrl = getEditUrl(pipeline, match);

                    return (
                      <tr key={id} className={trCls}>
                        <td>{name}</td>
                        <td>{workerClusterName}</td>
                        <td>{status}</td>
                        <td className="has-icon">
                          <LinkIcon to={editUrl}>
                            <i className="far fa-edit" />
                          </LinkIcon>
                        </td>
                        <td data-testid="delete-pipeline" className="has-icon">
                          <DeleteIcon
                            onClick={() =>
                              this.handleDeletePipelineModalOpen(id)
                            }
                          >
                            <i className="far fa-trash-alt" />
                          </DeleteIcon>
                        </td>
                      </tr>
                    );
                  })}
                </Table>
              )}
            </Box>
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default PipelineListPage;
