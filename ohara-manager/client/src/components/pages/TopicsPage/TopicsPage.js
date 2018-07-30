import React from 'react';
import ReactModal from 'react-modal';
import styled from 'styled-components';
import toastr from 'toastr';

import AppWrapper from '../../common/AppWrapper';
import Loading from '../../common/Loading';
import { ListTable } from '../../common/Table';
import { fetchTopics, createTopics } from '../../../apis/topicsApis';
import { TOPICS } from '../../../constants/url';
import { isNumber } from '../../../utils/helpers';
import * as MESSAGE from '../../../constants/message';

const modalStyles = {
  content: {
    top: '15%',
    left: '50%',
    right: 'auto',
    bottom: 'auto',
    marginRight: '-50%',
    transform: 'translate(-50%, 0)',
    minWidth: '400px',
  },
};

const H4 = styled.h4`
  margin-bottom: 15px;
`;

const Icon = styled.i`
  position: absolute;
  right: 20px;
  top: 15px;
  cursor: pointer;
  padding: 8px;
`;

class TopicsPage extends React.Component {
  state = {
    headers: ['Topic Name', 'Details'],
    topics: [],
    isLoading: true,
    modalIsActive: false,
    topicName: '',
    partition: '',
    replication: '',
  };

  async componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const result = await fetchTopics();

    if (result.status) {
      const { uuids } = result;
      const topics = Object.keys(uuids).map(key => {
        return {
          [key]: uuids[key],
        };
      });

      this.setState({ topics, isLoading: false });
    }
  };

  handleChangeInput = ({ target: { name, value } }) => {
    this.setState({ [name]: value });
  };

  handleSave = async e => {
    e.preventDefault();
    const { topicName: name, partition, replication } = this.state;

    const numberOfPartitions = Number(partition);
    const numberOfReplications = Number(replication);

    if (!isNumber(numberOfPartitions) || !isNumber(numberOfReplications)) {
      toastr.error(MESSAGE.ONLY_NUMBER_ALLOW_ERROR);
      return;
    }

    const result = await createTopics({
      name,
      numberOfPartitions,
      numberOfReplications,
    });

    if (result.status) {
      toastr.success(MESSAGE.TOPIC_CREATION_SUCCESS);
      this.handleCloseModal();
      this.fetchData();
    }
  };

  handleOpenModal = () => {
    this.setState({ modalIsActive: true });
  };

  handleCloseModal = () => {
    this.setState({ modalIsActive: false });
    this.reset();
  };

  reset = () => {
    this.setState({ topicName: '', partition: '', replication: '' });
  };

  render() {
    const {
      isLoading,
      headers,
      topics,
      modalIsActive,
      topicName,
      partition,
      replication,
    } = this.state;

    if (isLoading) {
      return <Loading />;
    }
    return (
      <AppWrapper title="Topics">
        <button
          onClick={this.handleOpenModal}
          type="button"
          className="btn btn-outline-primary mb-3"
        >
          Create topics
        </button>
        <ReactModal
          isOpen={modalIsActive}
          contentLabel="Modal"
          ariaHideApp={false}
          style={modalStyles}
          onRequestClose={this.handleCloseModal}
        >
          <Icon
            className="fas fa-times text-muted"
            onClick={this.handleCloseModal}
          />
          <H4>Create a topic</H4>

          <form onSubmit={this.handleSave}>
            <div className="form-group">
              <label htmlFor="topic-name">Topic name</label>
              <input
                type="text"
                className="form-control"
                id="topic-name"
                name="topicName"
                placeholder="Enter a topic name"
                value={topicName}
                onChange={this.handleChangeInput}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="partition">Partition</label>
              <input
                type="text"
                className="form-control"
                id="partition"
                name="partition"
                placeholder="Numbers of partition"
                value={partition}
                onChange={this.handleChangeInput}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="replication">Replication</label>
              <input
                type="text"
                className="form-control"
                id="replication"
                name="replication"
                placeholder="Numbers of replication"
                value={replication}
                onChange={this.handleChangeInput}
                required
              />
            </div>
            <div className="float-right">
              <button type="submit" className="btn btn-primary">
                Save
              </button>
              <button
                type="button"
                className="btn btn-default"
                onClick={this.handleCloseModal}
              >
                Close
              </button>
            </div>
          </form>
        </ReactModal>
        <ListTable headers={headers} list={topics} urlDir={TOPICS} />
      </AppWrapper>
    );
  }
}

export default TopicsPage;
