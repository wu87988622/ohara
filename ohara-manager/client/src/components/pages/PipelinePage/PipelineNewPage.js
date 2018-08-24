import React from 'react';
import styled from 'styled-components';
import DocumentTitle from 'react-document-title';
import toastr from 'toastr';
import { Route, Redirect } from 'react-router-dom';

import PipelineSourcePage from './PipelineSourcePage';
import PipelineTopicPage from './PipelineTopicPage';
import PipelineSinkPage from './PipelineSinkPage';
import Toolbar from './Toolbar';
import PipelineGraph from './PipelineGraph';
import Editable from './Editable';
import { ConfirmModal } from '../../common/Modal';
import { deleteBtn } from '../../../theme/btnTheme';
import { Button } from '../../common/Form';
import { fetchTopic } from '../../../apis/topicApis';
import { deletePipeline } from '../../../apis/pipelinesApis';
import { H2 } from '../../common/Headings';
import { PIPELINE } from '../../../constants/urls';
import { PIPELINE_NEW } from '../../../constants/documentTitles';
import * as _ from '../../../utils/helpers';
import * as MESSAGES from '../../../constants/messages';

const Wrapper = styled.div`
  padding: 100px 30px 0 240px;
`;

const Header = styled.div`
  display: flex;
  align-items: center;
`;

const Actions = styled.div`
  margin-left: auto;
`;

class PipelineNewPage extends React.Component {
  state = {
    title: 'Untitled pipeline',
    topicName: '',
    graph: [
      {
        type: 'source',
        isExist: false,
        isActive: false,
        uuid: null,
        icon: 'fa-database',
      },
      {
        type: 'topic',
        isExist: false,
        isActive: false,
        uuid: null,
        icon: 'fa-list-ul',
      },
      {
        type: 'sink',
        isExist: false,
        isActive: false,
        uuid: null,
        icon: 'hadoop',
      },
    ],
    isRedirect: false,
    isLoading: true,
    isModalActive: false,
  };

  componentDidMount() {
    const isValid = this.checkTopicId(this.props.match);

    if (isValid) {
      this.fetchData();
    }
  }

  componentDidUpdate({ match }) {
    const prevPage = _.get(match, 'params.page', null);
    const currPage = _.get(this.props.match, 'params.page', null);

    if (currPage !== prevPage) {
      const { graph } = this.state;
      const _page = graph.find(g => g.type === currPage);
      const update = { ..._page, isActive: true };
      this.updateGraph(graph, update, currPage);
    }
  }

  fetchData = async () => {
    const { topicId } = this.props.match.params;
    const res = await fetchTopic(topicId);
    this.setState(() => ({ isLoading: false }));

    const result = _.get(res, 'data.result', null);

    if (!_.isNull(result)) {
      this.setState({ topicName: result.name });
    }
  };

  checkTopicId = match => {
    const topicId = _.get(match, 'params.topicId', null);
    const isValid = !_.isNull(topicId) && _.isUuid(topicId);

    if (!isValid) {
      toastr.error(MESSAGES.TOPIC_ID_REQUIRED_ERROR);
      this.setState(() => ({ isRedirect: true }));
      return false;
    } else {
      const { graph } = this.state;
      const topic = graph.find(g => g.type === 'topic');
      const update = { ...topic, isActive: true, uuid: topicId, isExist: true };
      this.updateGraph(graph, update, 'topic');
      return true;
    }
  };

  updateGraph = (graph, update, type) => {
    const idx = graph.findIndex(g => g.type === type);
    const _graph = [...graph.slice(0, idx), update, ...graph.slice(idx + 1)];

    this.setState({ graph: _graph });
  };

  resetGraph = graph => {
    const update = graph.map(g => ({ ...g, isActive: false }));
    this.setState({ graph: update });
  };

  handleTitleChange = ({ target: { value: title } }) => {
    this.setState(() => ({ title }));
  };

  handleModalOpen = () => {
    this.setState({ isModalActive: true });
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false });
  };

  handlePipelineDelete = async () => {
    const pipelineId = _.get(this.props.match, 'params.pipelineId', null);
    const res = await deletePipeline(pipelineId);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.PIPELINE_DELETION_SUCCESS);
      this.setState(() => ({ isRedirect: true }));
    }
  };

  render() {
    const {
      title,
      isLoading,
      graph,
      isRedirect,
      topicName,
      isModalActive,
    } = this.state;

    if (isRedirect) {
      return <Redirect to={PIPELINE} />;
    }

    return (
      <DocumentTitle title={PIPELINE_NEW}>
        <React.Fragment>
          <ConfirmModal
            isActive={isModalActive}
            title="Delete pipeline"
            handleCancel={this.handleModalClose}
            handleConfirm={this.handlePipelineDelete}
            message="Are you sure you want to delete this pipeline? This action cannot be redo!"
          />

          <Wrapper>
            <Header>
              <H2>
                <Editable title={title} handleChange={this.handleTitleChange} />
              </H2>

              <Actions>
                <Button
                  theme={deleteBtn}
                  text="Delete pipeline"
                  data-testid="delete-pipeline-btn"
                  handleClick={this.handleModalOpen}
                />
              </Actions>
            </Header>
            <Toolbar
              updateGraph={this.updateGraph}
              graph={graph}
              {...this.props}
            />
            <PipelineGraph
              graph={graph}
              resetGraph={this.resetGraph}
              {...this.props}
            />

            <Route
              path="/pipeline/new/source"
              render={() => <PipelineSourcePage />}
            />
            <Route
              path="/pipeline/new/topic"
              render={() => (
                <PipelineTopicPage isLoading={isLoading} name={topicName} />
              )}
            />
            <Route
              path="/pipeline/new/sink"
              render={() => <PipelineSinkPage />}
            />
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default PipelineNewPage;
