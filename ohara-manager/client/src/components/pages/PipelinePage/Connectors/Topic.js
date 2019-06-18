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
import { get, isEmpty, some, includes, omit } from 'lodash';
import toastr from 'toastr';

import * as pipelineApi from 'api/pipelineApi';
import * as MESSAGES from 'constants/messages';
import * as s from './styles';
import Controller from './Controller';
import { ListLoader } from 'components/common/Loader';
import { Box } from 'components/common/Layout';
import { FormGroup, Label, Input } from 'components/common/Form';
import { fetchTopic } from 'api/topicApi';
import { graph as graphPropType } from 'propTypes/pipeline';

class Topic extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      params: PropTypes.object,
    }).isRequired,
    history: PropTypes.object,
    pipeline: PropTypes.shape({
      id: PropTypes.string,
      name: PropTypes.string,
      rules: PropTypes.object,
    }).isRequired,
    graph: PropTypes.arrayOf(graphPropType).isRequired,
    refreshGraph: PropTypes.func.isRequired,
  };

  state = {
    topic: null,
    isLoading: true,
  };

  componentDidMount() {
    this.fetchTopic();
  }

  componentDidUpdate(prevProps) {
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevConnectorId !== currConnectorId) {
      this.fetchTopic();
    }
  }

  fetchTopic = async () => {
    const { connectorId } = this.props.match.params;
    const res = await fetchTopic(connectorId);
    const topic = get(res, 'data.result', null);
    if (topic) {
      this.setState({ topic });
    }
    this.setState({ isLoading: false });
  };

  deleteTopic = async () => {
    const { match, history, pipeline, graph, refreshGraph } = this.props;
    const connectorId = get(match, 'params.connectorId', null);

    if (this.hasAnyConnection(graph, connectorId)) {
      toastr.error(MESSAGES.CANNOT_DELETE_TOPIC_ERROR);
      return;
    }

    const {
      id: pipelineId,
      name: pipelineName,
      rules: pipelineRules,
    } = pipeline;

    const params = {
      name: pipelineName,
      rules: omit(pipelineRules, connectorId),
    };

    const res = await pipelineApi.updatePipeline({ id: pipelineId, params });
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      const {
        topic: { name: topicName },
      } = this.state;
      toastr.success(`${MESSAGES.TOPIC_DELETION_SUCCESS} ${topicName}`);
      refreshGraph();
      history.push(`/pipelines/edit/${pipelineId}`);
    }
  };

  hasAnyConnection = (graph, connectorId) =>
    some(graph, ({ id, to }) => {
      if (id === connectorId && !isEmpty(to)) return true;
      if (includes(to, connectorId)) return true;
    });

  render() {
    const { topic, isLoading } = this.state;
    return (
      <React.Fragment>
        {isLoading ? (
          <Box>
            <ListLoader />
          </Box>
        ) : (
          <Box>
            <s.TitleWrapper>
              <s.H5Wrapper>Topic</s.H5Wrapper>
              <Controller
                kind="topic"
                onDelete={this.deleteTopic}
                show={['delete']}
              />
            </s.TitleWrapper>
            <FormGroup data-testid="name">
              <Label>Name</Label>
              <Input name="name" width="100%" value={topic.name} disabled />
            </FormGroup>
          </Box>
        )}
      </React.Fragment>
    );
  }
}

export default Topic;
