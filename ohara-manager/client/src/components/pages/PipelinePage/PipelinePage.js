import React from 'react';
import DocumentTitle from 'react-document-title';
import styled from 'styled-components';
import toastr from 'toastr';

import Modal from '../../common/Modal';
import { fetchTopics } from '../../../apis/kafkaApis';
import { H2 } from '../../common/Heading';
import { Button, Select } from '../../common/Form';
import { submitButton } from '../../../theme/buttonTheme';
import { PIPELINE } from '../../../constants/documentTitles';
import { get, isEmptyArray } from '../../../utils/helpers';
import {
  lighterBlue,
  lightYellow,
  lightOrange,
  radiusCompact,
} from '../../../theme/variables';

const Wrapper = styled.div`
  padding: 100px 30px 0 240px;
`;

const Inner = styled.div`
  padding: 30px 20px;
`;

const Warning = styled.p`
  font-size: 13px;
  margin: 0 0 8px 0;
  color: ${lighterBlue};
`;

const IWrapper = styled.i`
  padding: 5px 10px;
  background-color: ${lightYellow};
  margin-right: 10px;
  display: inline-block;
  color: ${lightOrange};
  font-size: 12px;
  border-radius: ${radiusCompact};
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
    const result = get(res, 'data.result', null);

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

  handleModalConfirm = () => {
    console.log(this.state.currentTopic); // eslint-disable-line
    this.handleModalClose();
  };

  handleModalOpen = e => {
    e.preventDefault();
    this.setState({ isModalActive: true });

    if (isEmptyArray(this.state.topics)) {
      toastr.error(`You don't have any topics!`);
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
            confirmButtonText="Next"
            handleConfirm={this.handleModalConfirm}
            handleCancel={this.handleModalClose}
            isConfirmDisabled={isEmptyArray(topics) ? true : false}
          >
            <Inner>
              <Warning>
                <IWrapper className="fas fa-exclamation" />
                Please select a topic for the new pipeline
              </Warning>
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
              theme={submitButton}
              text="New pipeline"
              handleClick={this.handleModalOpen}
            />
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default PipelinePage;
