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
import { fetchTopic } from '../../../apis/topicApis';
import { H2 } from '../../common/Heading';
import { PIPELINE } from '../../../constants/url';
import { PIPELINE_NEW } from '../../../constants/documentTitles';
import * as _ from '../../../utils/helpers';

const Wrapper = styled.div`
  padding: 100px 30px 0 240px;
`;

class PipelineNewPage extends React.Component {
  state = {
    title: 'Untitle pipeline',
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
    const result = _.get(res, 'data.result', null);

    if (!_.isNull(result)) {
      this.setState({ topicName: result.name });
    }
  };

  checkTopicId = match => {
    const topicId = _.get(match, 'params.topicId', null);
    const isValid = !_.isNull(topicId) && _.isUuid(topicId);

    if (!isValid) {
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

  render() {
    const { title, graph, isRedirect, topicName } = this.state;

    if (isRedirect) {
      toastr.error(
        'You need to select a topic before creating a new pipeline!',
      );
      return <Redirect to={PIPELINE} />;
    }

    return (
      <DocumentTitle title={PIPELINE_NEW}>
        <Wrapper>
          <H2>
            <Editable
              text={title}
              inputWidth="250px"
              inputHeight="40px"
              inputMaxLength="50"
              labelFontWeight="bold"
              inputFontWeight="bold"
            />
          </H2>
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
            render={() => <PipelineTopicPage name={topicName} />}
          />
          <Route
            path="/pipeline/new/sink"
            render={() => <PipelineSinkPage />}
          />
        </Wrapper>
      </DocumentTitle>
    );
  }
}

export default PipelineNewPage;
