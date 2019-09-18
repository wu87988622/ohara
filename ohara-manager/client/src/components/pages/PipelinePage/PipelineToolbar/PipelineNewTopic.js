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

import React, {
  useState,
  useEffect,
  useImperativeHandle,
  forwardRef,
} from 'react';
import PropTypes from 'prop-types';
import toastr from 'toastr';
import { isEmpty, get, isNull } from 'lodash';
import { Link } from 'react-router-dom';

import * as MESSAGES from 'constants/messages';
import * as URLS from 'constants/urls';
import * as pipelineApi from 'api/pipelineApi';
import { Warning } from 'components/common/Messages';
import { Select } from 'components/common/Form';
import { createConnector } from './pipelineToolbarUtils';
import { findByGraphName } from '../pipelineUtils';
import { graph as graphPropType } from 'propTypes/pipeline';
import { Wrapper } from './styles';

const PipelineNewTopic = forwardRef((props, ref) => {
  const [updatedTopics, setUpdateTopics] = useState([]);

  const { enableAddButton, currentTopic, topics, workerClusterName } = props;

  useEffect(() => {
    enableAddButton(isNull(currentTopic));

    const fetchPipelines = async () => {
      const response = await pipelineApi.fetchPipelines();
      const pipelines = get(response, 'data.result', []);

      const updatedTopics = topics.map(topic => {
        const isDisabled = pipelines.some(pipeline => {
          return pipeline.objects.some(object => object.name === topic.name);
        });

        return {
          ...topic,
          disabled: isDisabled,
        };
      });

      setUpdateTopics(updatedTopics);
    };

    fetchPipelines();
  }, [currentTopic, topics, enableAddButton]);

  const handleSelectChange = ({ target }) => {
    const currentTopic = { name: target.value };
    props.updateTopic(currentTopic);
  };

  useImperativeHandle(ref, () => ({
    update() {
      const { graph, updateGraph, currentTopic, pipelineName } = props;

      if (!currentTopic) {
        return toastr.error(MESSAGES.NO_TOPIC_IS_SUPPLIED);
      }

      // Don't add a topic if it's already existed in the pipeline graph
      const isTopicExist = findByGraphName(graph, currentTopic.name);

      if (!isTopicExist) {
        const connector = {
          ...currentTopic,
          className: 'topic',
          typeName: 'topic',
        };
        createConnector({ updateGraph, connector, pipelineName });
      }
    },
  }));

  return (
    <Wrapper>
      {isEmpty(topics) ? (
        <Warning
          text={
            <>
              {`You don't have any topics available in this workspace yet. But you can create one in `}
              <Link to={`${URLS.WORKSPACES}/${workerClusterName}/topics`}>
                here
              </Link>
            </>
          }
        />
      ) : (
        <Select
          isObject
          list={updatedTopics}
          selected={currentTopic}
          handleChange={handleSelectChange}
          data-testid="topic-select"
        />
      )}
    </Wrapper>
  );
});

PipelineNewTopic.propTypes = {
  graph: PropTypes.arrayOf(graphPropType).isRequired,
  updateGraph: PropTypes.func.isRequired,
  topics: PropTypes.array.isRequired,
  updateTopic: PropTypes.func.isRequired,
  enableAddButton: PropTypes.func.isRequired,
  workerClusterName: PropTypes.string.isRequired,
  currentTopic: PropTypes.object,
  pipelineName: PropTypes.string.isRequired,
};

export default PipelineNewTopic;
