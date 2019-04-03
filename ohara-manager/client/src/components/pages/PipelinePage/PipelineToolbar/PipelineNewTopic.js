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
import toastr from 'toastr';

import * as MESSAGES from 'constants/messages';
import { Box } from 'common/Layout';
import { Select } from 'common/Form';
import { createConnector } from '../pipelineUtils/pipelineToolbarUtils';
import { findByGraphId } from '../pipelineUtils/commonUtils';
import { graphPropType } from 'propTypes/pipeline';

class PipelineNewTopic extends React.Component {
  static propTypes = {
    graph: PropTypes.arrayOf(graphPropType).isRequired,
    updateGraph: PropTypes.func.isRequired,
    topics: PropTypes.array.isRequired,
    currentTopic: PropTypes.object.isRequired,
    updateTopic: PropTypes.func.isRequired,
    updateAddBtnStatus: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.props.updateAddBtnStatus(this.props.currentTopic);
  }

  handleSelectChange = ({ target }) => {
    const selectedIdx = target.options.selectedIndex;
    const { id } = target.options[selectedIdx].dataset;
    const currentTopic = { name: target.value, id };

    this.props.updateTopic(currentTopic);
  };

  update = () => {
    const { graph, updateGraph, currentTopic } = this.props;

    if (!currentTopic) {
      return toastr.error(MESSAGES.NO_TOPIC_IS_SUPPLIED);
    }

    // Don't add a topic if it's already existed in the pipeline graph
    const isTopicExist = findByGraphId(graph, currentTopic.id);

    if (!isTopicExist) {
      const connector = {
        ...currentTopic,
        className: 'topic',
        typeName: 'topic',
      };
      createConnector({ updateGraph, connector });
    }
  };

  render() {
    const { topics, currentTopic } = this.props;

    if (!topics) return null;

    return (
      <Box shadow={false}>
        <React.Fragment>
          <Select
            isObject
            list={topics}
            selected={currentTopic}
            handleChange={this.handleSelectChange}
          />
        </React.Fragment>
      </Box>
    );
  }
}

export default PipelineNewTopic;
