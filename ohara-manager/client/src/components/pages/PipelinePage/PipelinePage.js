import React from 'react';
import { Link } from 'react-router-dom';
import DocumentTitle from 'react-document-title';
import styled from 'styled-components';
import toastr from 'toastr';

import { Modal } from '../../common/Modal';
import { DataTable } from '../../common/Table';
import { Box } from '../../common/Layout';
import { Warning } from '../../common/Messages';
import { fetchTopics } from '../../../apis/topicApis';
import { createPipeline, fetchPipelines } from '../../../apis/pipelinesApis';
import { H2 } from '../../common/Headings';
import { Button, Select } from '../../common/Form';
import { primaryBtn } from '../../../theme/btnTheme';
import { PIPELINE } from '../../../constants/documentTitles';
import * as _ from '../../../utils/helpers';
import * as MESSAGES from '../../../constants/messages';
import { lightBlue, blue, red } from '../../../theme/variables';

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

const DeleteIcon = styled(Link)`
  color: ${lightBlue};

  &:hover {
    color: ${red};
  }
`;

class PipelinePage extends React.Component {
  headers = ['#', 'name', 'status', 'start/stop', 'edit', 'delete'];
  state = {
    isModalActive: false,
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
      this.setState(() => {
        return { topics: result };
      });
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

  handleModalConfirm = async () => {
    const { history, match } = this.props;
    const { uuid: topicUuid } = this.state.currentTopic;

    const params = { name: 'untitled pipeline', rules: { [topicUuid]: '?' } };
    const res = await createPipeline(params);

    const pipelineUuid = _.get(res, 'data.result.uuid', null);

    if (!_.isNull(pipelineUuid)) {
      this.handleModalClose();
      toastr.success('New pipeline has been created!');
      history.push(`${match.url}/new/topic/${pipelineUuid}/${topicUuid}`);
    }
  };

  handleModalOpen = e => {
    e.preventDefault();
    this.setState({ isModalActive: true });

    if (_.isEmptyArr(this.state.topics)) {
      toastr.error(MESSAGES.NO_TOPICS_FOUND_ERROR);
    }
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false });
  };

  setCurrentTopic = (idx = 0) => {
    this.setState(({ topics }) => {
      return {
        currentTopic: topics[idx],
      };
    });
  };

  reset = () => {
    this.setCurrentTopic();
  };

  render() {
    const { isModalActive, topics, currentTopic, pipelines } = this.state;

    return (
      <DocumentTitle title={PIPELINE}>
        <React.Fragment>
          <Modal
            isActive={isModalActive}
            title="Select topic"
            width="370px"
            confirmBtnText="Next"
            handleConfirm={this.handleModalConfirm}
            handleCancel={this.handleModalClose}
            isConfirmDisabled={_.isEmptyArr(topics) ? true : false}
          >
            <Inner>
              <Warning text="Please select a topic for the new pipeline" />
              <Select
                list={topics}
                selected={currentTopic}
                handleChange={this.handleSelectChange}
              />
            </Inner>
          </Modal>
          <Wrapper>
            <TopWrapper>
              <H2>Pipeline</H2>
              <NewPipelineBtn
                theme={primaryBtn}
                text="New pipeline"
                data-testid="new-pipeline"
                handleClick={this.handleModalOpen}
              />
            </TopWrapper>
            <Box>
              <DataTable headers={this.headers} align="center">
                {pipelines.map(({ uuid, name, status }, idx) => {
                  const startStopCls =
                    status === 'running' ? 'fa-stop-circle' : 'fa-play-circle';

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
                        <LinkIcon to="/">
                          <i className="far fa-edit" />
                        </LinkIcon>
                      </td>
                      <td className="has-icon">
                        <DeleteIcon to="/">
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
