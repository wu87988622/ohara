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
import styled from 'styled-components';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';

import * as MESSAGES from 'constants/messages';
import * as pipelinesApis from 'apis/pipelinesApis';
import * as _ from 'utils/commonUtils';
import { CONNECTOR_TYPES } from 'constants/pipelines';
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { lightBlue } from 'theme/variables';
import { Input, Select, FormGroup, Label } from 'common/Form';
import { fetchHdfs } from 'apis/configurationApis';
import { CONFIGURATION } from 'constants/urls';
import { updateTopic, findByGraphId } from 'utils/pipelineUtils';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
`;

H5Wrapper.displayName = 'H5';

const FormGroupCheckbox = styled(FormGroup)`
  flex-direction: row;
  align-items: center;
  color: ${lightBlue};
`;

const Checkbox = styled(Input)`
  height: auto;
  width: auto;
  margin-right: 8px;
`;

Checkbox.displayName = 'Checkbox';

class HdfsSink extends React.Component {
  static propTypes = {
    hasChanges: PropTypes.bool.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    loadGraph: PropTypes.func.isRequired,
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
    topics: PropTypes.array.isRequired,
  };

  selectMaps = {
    topics: 'currReadTopic',
    hdfses: 'currHdfs',
  };

  state = {
    name: '',
    readTopics: [],
    currReadTopic: {},
    hdfses: [],
    currHdfs: {},
    writePath: '',
    pipelines: {},
    fileEncodings: ['UTF-8'],
    currFileEncoding: {},
    flushLineCount: '',
    rotateInterval: '',
    tempDirectory: '',
    needHeader: true,
    isRedirect: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  componentDidUpdate(prevProps) {
    const { topics: prevTopics } = prevProps;
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { hasChanges, topics: currTopics } = this.props;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevTopics !== currTopics) {
      this.setState({ writeTopics: currTopics });
    }

    if (prevConnectorId !== currConnectorId) {
      this.fetchData();
    }

    if (hasChanges) {
      this.save();
    }
  }

  setDefaults = () => {
    this.setState(({ fileEncodings }) => ({
      currFileEncoding: fileEncodings[0],
    }));
  };

  fetchData = () => {
    const sinkId = _.get(this.props.match, 'params.connectorId', null);
    this.setDefaults();
    this.fetchHdfs(sinkId);
    this.fetchSink(sinkId);
  };

  fetchSink = async sinkId => {
    const res = await pipelinesApis.fetchSink(sinkId);
    const result = _.get(res, 'data.result', null);

    if (result) {
      const { currHdfs } = this.state;
      const { name, configs, topics: prevTopics } = result;
      const {
        hdfs = '{}',
        writePath = '',
        needHeader = false,
        'tmp.dir': tempDirectory = '',
        'flush.line.count': flushLineCount = '',
        'rotate.interval.ms': rotateInterval = '',
        'data.econde': currFileEncoding = '',
      } = configs;

      if (_.isEmpty(prevTopics)) {
        this.setTopic();
      } else {
        const { topics } = this.props;
        const currReadTopic = topics.find(topic => topic.id === prevTopics[0]);

        updateTopic(this.props, currReadTopic, 'sink');
        this.setState({ readTopics: topics, currReadTopic });
      }

      const prevHdfs = JSON.parse(hdfs);
      const isDiffHdfs = prevHdfs !== currHdfs ? true : false;
      const _currHdfs = isDiffHdfs ? currHdfs : prevHdfs;
      const _needHeader = needHeader === 'true' ? true : false;

      this.setState({
        name,
        writePath,
        currHdfs: _currHdfs,
        needHeader: _needHeader,
        tempDirectory,
        flushLineCount,
        rotateInterval,
        currFileEncoding,
      });

      if (isDiffHdfs) {
        this.save();
      }
    }
  };

  fetchHdfs = async () => {
    const { currHdfs } = this.state;
    const res = await fetchHdfs();
    const hdfses = await _.get(res, 'data.result', []);

    if (!_.isEmpty(hdfses)) {
      const mostRecent = hdfses.reduce((prev, curr) =>
        prev.lastModified > curr.lastModified ? prev : curr,
      );

      const _currHdfs = _.isEmpty(currHdfs) ? mostRecent : currHdfs;

      this.setState({ hdfses: [_currHdfs], currHdfs: _currHdfs });
    } else {
      this.setState({ isRedirect: true });
      toastr.error(MESSAGES.NO_CONFIGURATION_FOUND_ERROR);
    }
  };

  setTopic = () => {
    const { topics } = this.props;

    this.setState(
      {
        readTopics: topics,
        currReadTopic: topics[0],
      },
      () => {
        const { currReadTopic } = this.state;
        updateTopic(this.props, currReadTopic, 'sink');
      },
    );
  };

  handleInputChange = ({ target: { name, value } }) => {
    this.setState({ [name]: value }, () => {
      this.props.updateHasChanges(true);
    });
  };

  handleCheckboxChange = ({ target }) => {
    const { name, checked } = target;
    this.setState({ [name]: checked }, () => {
      this.props.updateHasChanges(true);
    });
  };

  handleSelectChange = ({ target }) => {
    const { name, options, value } = target;
    const selectedIdx = options.selectedIndex;
    const { id } = options[selectedIdx].dataset;

    const current = this.selectMaps[name];

    this.setState(
      () => {
        return {
          [current]: {
            name: value,
            id,
          },
        };
      },
      () => {
        this.props.updateHasChanges(true);
      },
    );
  };

  save = _.debounce(async () => {
    const {
      updateHasChanges,
      match,
      graph,
      updateGraph,
      isPipelineRunning,
    } = this.props;
    const {
      name,
      currHdfs,
      currReadTopic,
      writePath,
      needHeader,
      tempDirectory,
      flushLineCount,
      rotateInterval,
      currFileEncoding,
    } = this.state;

    if (isPipelineRunning) {
      toastr.error(MESSAGES.CANNOT_UPDATE_WHILE_RUNNING_ERROR);
      updateHasChanges(false);
      return;
    }

    const sinkId = _.get(match, 'params.connectorId', null);
    const topics = _.isEmpty(currReadTopic) ? [] : [currReadTopic.id];

    const params = {
      name,
      schema: [],
      className: CONNECTOR_TYPES.hdfsSink,
      topics,
      numberOfTasks: 1,
      configs: {
        topic: JSON.stringify(currReadTopic),
        hdfs: JSON.stringify(currHdfs),
        needHeader: String(needHeader),
        writePath,
        'datafile.needheader': String(needHeader),
        'datafile.prefix.name': 'part',
        'data.dir': writePath,
        'hdfs.url': currHdfs.uri,
        'tmp.dir': tempDirectory,
        'flush.line.count': flushLineCount,
        'rotate.interval.ms': rotateInterval,
        'data.econde': currFileEncoding,
      },
    };

    await pipelinesApis.updateSink({ id: sinkId, params });
    updateHasChanges(false);

    const currTopicId = _.isEmpty(currReadTopic) ? '?' : currReadTopic.id;
    const topic = findByGraphId(graph, currTopicId);

    if (topic) {
      const update = { ...topic, to: sinkId };
      updateGraph(update, currTopicId);
    }
  }, 1000);

  render() {
    const {
      name,
      readTopics,
      currReadTopic,
      hdfses,
      currHdfs,
      writePath,
      needHeader,
      isRedirect,
      fileEncodings,
      currFileEncoding,
      tempDirectory,
      rotateInterval,
      flushLineCount,
    } = this.state;

    if (isRedirect) {
      return <Redirect to={CONFIGURATION} />;
    }

    return (
      <Box>
        <H5Wrapper>HDFS</H5Wrapper>
        <form>
          <FormGroup data-testid="name">
            <Label>Name</Label>
            <Input
              name="name"
              width="100%"
              placeholder="HDFS sink name"
              value={name}
              data-testid="name-input"
              handleChange={this.handleInputChange}
            />
          </FormGroup>
          <FormGroup data-testid="read-from-topic">
            <Label>Read from topic</Label>
            <Select
              isObject
              name="topics"
              list={readTopics}
              selected={currReadTopic}
              width="100%"
              data-testid="topic-select"
              handleChange={this.handleSelectChange}
            />
          </FormGroup>

          <FormGroup data-testid="hdfses">
            <Label>HDFS</Label>
            <Select
              isObject
              name="hdfses"
              list={hdfses}
              selected={currHdfs}
              width="100%"
              data-testid="hdfses-select"
              handleChange={this.handleSelectChange}
            />
          </FormGroup>

          <FormGroup data-testid="write-path">
            <Label>Write path</Label>
            <Input
              name="writePath"
              width="100%"
              placeholder="file://"
              value={writePath}
              data-testid="write-path-input"
              handleChange={this.handleInputChange}
            />
          </FormGroup>

          <FormGroup data-testid="temp-directory">
            <Label>Temp directory</Label>
            <Input
              name="tempDirectory"
              width="100%"
              placeholder="/tmp"
              value={tempDirectory}
              data-testid="temp-directory"
              handleChange={this.handleInputChange}
            />
          </FormGroup>

          <FormGroup data-testid="file-encoding">
            <Label>File encoding</Label>
            <Select
              name="fileEncoding"
              width="100%"
              data-testid="file-enconding-select"
              selected={currFileEncoding}
              list={fileEncodings}
              handleChange={this.handleSelectChange}
            />
          </FormGroup>

          <FormGroup data-testid="rotate-interval">
            <Label>Rotate interval (ms)</Label>
            <Input
              name="rotateInterval"
              width="100%"
              placeholder="60000"
              value={rotateInterval}
              data-testid="rotate-interval"
              handleChange={this.handleInputChange}
            />
          </FormGroup>

          <FormGroup data-testid="flush-line-count">
            <Label>Flush line count</Label>
            <Input
              name="flushLineCount"
              width="100%"
              placeholder="10"
              value={flushLineCount}
              data-testid="flush-line-count"
              handleChange={this.handleInputChange}
            />
          </FormGroup>

          <FormGroupCheckbox data-testid="need-header">
            <Checkbox
              type="checkbox"
              name="needHeader"
              width="25px"
              value=""
              checked={needHeader}
              data-testid="needheader-input"
              handleChange={this.handleCheckboxChange}
            />
            Include header
          </FormGroupCheckbox>
        </form>
      </Box>
    );
  }
}

export default HdfsSink;
