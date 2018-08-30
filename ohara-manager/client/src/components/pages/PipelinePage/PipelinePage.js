import React from 'react';
import DocumentTitle from 'react-document-title';
import styled from 'styled-components';
import toastr from 'toastr';

import { Modal } from '../../common/Modal';
import { Warning } from '../../common/Messages';
import { fetchTopics } from '../../../apis/topicApis';
import { savePipelines } from '../../../apis/pipelinesApis';
import { H2 } from '../../common/Headings';
import { Button, Select } from '../../common/Form';
import { primaryBtn } from '../../../theme/btnTheme';
import { PIPELINE } from '../../../constants/documentTitles';
import * as _ from '../../../utils/helpers';
import * as MESSAGES from '../../../constants/messages';

const Wrapper = styled.div`
  padding: 100px 30px 0 240px;
`;

const Inner = styled.div`
  padding: 30px 20px;
`;

class PipelinePage extends React.Component {
  state = {
    isModalActive: false,
    topics: [],
    currentTopic: {},
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const res = await fetchTopics();
    const result = _.get(res, 'data.result', null);

    if (result && result.length > 0) {
      this.setState({ topics: result });
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

    const params = { name: 'untitle pipeline', rules: { [topicUuid]: '?' } };
    const res = await savePipelines(params);

    const pipelineUuid = _.get(res, 'data.result.uuid', null);

    if (!_.isNull(pipelineUuid)) {
      this.handleModalClose();
      toastr.success('New pipeline has been created!');
      history.push(`${match.url}/new/topic/${pipelineUuid}/${topicUuid}`);
    }
  };

  save = () => {};

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
    const { isModalActive, topics, currentTopic } = this.state;

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
            <H2>Pipeline</H2>
            <Button
              theme={primaryBtn}
              text="New pipeline"
              data-testid="new-pipeline"
              handleClick={this.handleModalOpen}
            />
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default PipelinePage;
