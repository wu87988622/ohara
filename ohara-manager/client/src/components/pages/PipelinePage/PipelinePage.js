import React from 'react';
import { Link } from 'react-router-dom';
import DocumentTitle from 'react-document-title';
import styled from 'styled-components';
import toastr from 'toastr';

import * as _ from 'utils/helpers';
import * as MESSAGES from 'constants/messages';
import { Modal, ConfirmModal } from 'common/Modal';
import { DataTable } from 'common/Table';
import { Box } from 'common/Layout';
import { Warning } from 'common/Messages';
import { fetchTopics } from 'apis/topicApis';
import { H2 } from 'common/Headings';
import { Button, Select } from 'common/Form';
import { primaryBtn } from 'theme/btnTheme';
import { PIPELINE } from 'constants/documentTitles';
import { lightBlue, blue, red } from 'theme/variables';
import {
  createPipeline,
  fetchPipelines,
  deletePipeline,
} from 'apis/pipelinesApis';

const Wrapper = styled.div`
  padding: 100px 30px 0 240px;
`;

const Inner = styled.div`
  padding: 30px 20px;
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

const LinkIcon = styled(Link)`
  color: ${lightBlue};

  &:hover {
    color: ${blue};
  }
`;

const DeleteIcon = styled.a`
  color: ${lightBlue};
  cursor: pointer;

  &:hover {
    color: ${red};
  }
`;

DeleteIcon.displayName = 'DeleteIcon';

class PipelinePage extends React.Component {
  headers = ['#', 'name', 'status', 'start/stop', 'edit', 'delete'];
  state = {
    isSelectTopicModalActive: false,
    isDeletePipelineModalActive: false,
    deletePipelineUuid: '',
    pipelines: [],
    topics: [],
    currentTopic: {},
  };

  componentDidMount() {
    this.fetchTopics();
    this.fetchPipelines();
  }

  fetchPipelines = async () => {
    const res = await fetchPipelines();
    const pipelines = _.get(res, 'data.result', null);

    if (pipelines) {
      this.setState({ pipelines });
    }
  };

  fetchTopics = async () => {
    const res = await fetchTopics();
    const result = _.get(res, 'data.result', null);

    if (result) {
      this.setState({ topics: result });
      this.setCurrentTopic();
    }
  };

  handleSelectChange = ({ target }) => {
    const selectedIdx = target.options.selectedIndex;
    const { uuid } = target.options[selectedIdx].dataset;

    this.setState({
      currentTopic: {
        name: target.value,
        uuid,
      },
    });
  };

  handleSelectTopicModalConfirm = async () => {
    const { history, match } = this.props;
    const { uuid: topicUuid } = this.state.currentTopic;

    const params = { name: 'untitled pipeline', rules: { [topicUuid]: '?' } };
    const res = await createPipeline(params);

    const pipelineUuid = _.get(res, 'data.result.uuid', null);

    if (pipelineUuid) {
      this.handleSelectTopicModalClose();
      toastr.success(MESSAGES.PIPELINE_CREATION_SUCCESS);
      history.push(`${match.url}/new/topic/${pipelineUuid}/${topicUuid}`);
    }
  };

  handleSelectTopicModalOpen = e => {
    e.preventDefault();
    this.setState({ isSelectTopicModalActive: true });

    if (_.isEmpty(this.state.topics)) {
      toastr.error(MESSAGES.NO_TOPICS_FOUND_ERROR);
    }
  };

  handleSelectTopicModalClose = () => {
    this.setState({ isSelectTopicModalActive: false });
  };

  handleDeletePipelineModalOpen = uuid => {
    this.setState({
      isDeletePipelineModalActive: true,
      deletePipelineUuid: uuid,
    });
  };

  handleDeletePipelineModalClose = () => {
    this.setState({
      isDeletePipelineModalActive: false,
      deletePipelineUuid: '',
    });
  };

  handleDeletePipelineConfirm = async () => {
    const { deletePipelineUuid: uuid } = this.state;

    if (!_.isUuid(uuid)) return;

    const res = await deletePipeline(uuid);
    const deletedUuid = _.get(res, 'data.result.uuid', null);
    const deletedPipeline = _.get(res, 'data.result', null);
    if (deletedUuid) {
      this.setState(({ pipelines }) => {
        const _pipelines = pipelines.filter(p => p.uuid !== deletedUuid);
        return {
          pipelines: _pipelines,
          isDeletePipelineModalActive: false,
          deletePipelineUuid: '',
        };
      });
      toastr.success(
        `${MESSAGES.PIPELINE_DELETION_SUCCESS} ${deletedPipeline.name}`,
      );
    } else {
      toastr.error(
        `${MESSAGES.PIPELINE_DELETION_ERROR} ${deletedPipeline.name}`,
      );
    }
  };

  setCurrentTopic = (idx = 0) => {
    this.setState(({ topics }) => {
      return {
        currentTopic: topics[idx],
      };
    });
  };

  getEditUrl = pipeline => {
    const { match } = this.props;
    const { uuid: pipelineId, objects } = pipeline;

    const { topic: topicId, source: sourceId, sink: sinkId } = objects.reduce(
      (acc, { uuid, kind }) => {
        acc[kind] = uuid;
        return acc;
      },
      {},
    );

    const baseUrl = `${match.url}/edit/source/${pipelineId}/${topicId}`;
    let url = baseUrl;

    if (sinkId) {
      url = `${baseUrl}/${sourceId}/${sinkId}`;
    } else if (sourceId) {
      url = `${baseUrl}/${sourceId}/`;
    }

    return url;
  };

  reset = () => {
    this.setCurrentTopic();
  };

  render() {
    const {
      isSelectTopicModalActive,
      isDeletePipelineModalActive,
      topics,
      currentTopic,
      pipelines,
    } = this.state;

    return (
      <DocumentTitle title={PIPELINE}>
        <React.Fragment>
          <Modal
            isActive={isSelectTopicModalActive}
            title="Select topic"
            width="370px"
            confirmBtnText="Next"
            handleConfirm={this.handleSelectTopicModalConfirm}
            handleCancel={this.handleSelectTopicModalClose}
            isConfirmDisabled={_.isEmpty(topics) ? true : false}
          >
            <Inner>
              <Warning text="Please select a topic for the new pipeline" />
              <Select
                isObject
                list={topics}
                selected={currentTopic}
                handleChange={this.handleSelectChange}
              />
            </Inner>
          </Modal>

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
              <H2>Pipeline</H2>
              <NewPipelineBtn
                theme={primaryBtn}
                text="New pipeline"
                data-testid="new-pipeline"
                handleClick={this.handleSelectTopicModalOpen}
              />
            </TopWrapper>
            <Box>
              <DataTable headers={this.headers} align="center">
                {pipelines.map((pipeline, idx) => {
                  const { uuid, name, status } = pipeline;
                  const startStopCls =
                    status === 'running' ? 'fa-stop-circle' : 'fa-play-circle';

                  const editUrl = this.getEditUrl(pipeline);

                  // TODO: replace the Link URLs with the correct ones
                  return (
                    <tr key={uuid}>
                      <td>{idx}</td>
                      <td>{name}</td>
                      <td>{status}</td>
                      <td className="has-icon">
                        <LinkIcon to="/">
                          <i className={`far ${startStopCls}`} />
                        </LinkIcon>
                      </td>

                      <td className="has-icon">
                        <LinkIcon to={editUrl}>
                          <i className="far fa-edit" />
                        </LinkIcon>
                      </td>
                      <td className="has-icon">
                        <DeleteIcon
                          onClick={() =>
                            this.handleDeletePipelineModalOpen(uuid)
                          }
                        >
                          <i className="far fa-trash-alt" />
                        </DeleteIcon>
                      </td>
                    </tr>
                  );
                })}
              </DataTable>
            </Box>
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default PipelinePage;
