import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';

import * as MESSAGES from 'constants/messages';
import * as _ from 'utils/commonUtils';
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { lightBlue } from 'theme/variables';
import { Input, Select, FormGroup, Label } from 'common/Form';
import { fetchTopics } from 'apis/topicApis';
import { fetchHdfs } from 'apis/configurationApis';
import { CONFIGURATION } from 'constants/urls';
import {
  createSink,
  updateSink,
  fetchSink,
  updatePipeline,
  fetchPipeline,
} from 'apis/pipelinesApis';

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

class PipelineSinkPage extends React.Component {
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
  };

  selectMaps = {
    topics: 'currTopic',
    hdfses: 'currHdfs',
  };

  state = {
    topics: [],
    currTopic: {},
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
    const { hasChanges, match } = this.props;

    const prevSinkId = _.get(prevProps.match, 'params.sinkId', null);
    const currSinkId = _.get(this.props.match, 'params.sinkId', null);
    const topicId = _.get(match, 'params.topicId');
    const hasTopicId = !_.isNull(topicId);
    const isUpdate = prevSinkId !== currSinkId;

    if (hasChanges) {
      this.save();
    }

    if (isUpdate && hasTopicId) {
      const { name, uuid, rules } = this.state.pipelines;

      const params = {
        name,
        rules: { ...rules, [topicId]: currSinkId },
      };

      this.updatePipeline(uuid, params);
    }
  }

  setDefaults = () => {
    this.setState(({ fileEncodings }) => ({
      currFileEncoding: fileEncodings[0],
    }));
  };

  fetchData = () => {
    const { match } = this.props;
    const topicId = _.get(match, 'params.topicId', null);
    const sinkId = _.get(match, 'params.sinkId', null);
    const pipelineId = _.get(match, 'params.pipelineId', null);

    this.setDefaults();

    if (sinkId) {
      const fetchTopicsPromise = this.fetchTopics(topicId);
      const fetchHdfsPromise = this.fetchHdfs(sinkId);
      const fetchPipelinePromise = this.fetchPipeline(pipelineId);

      Promise.all([
        fetchTopicsPromise,
        fetchHdfsPromise,
        fetchPipelinePromise,
      ]).then(() => {
        this.fetchSink(sinkId);
      });

      return;
    }

    this.fetchTopics(topicId);
    this.fetchPipeline(pipelineId);
    this.fetchHdfs();
  };

  fetchSink = async sinkId => {
    const res = await fetchSink(sinkId);
    const isSuccess = _.get(res, 'data.isSuccess', null);

    if (isSuccess) {
      const { currHdfs } = this.state;
      const {
        topic,
        hdfs,
        writePath,
        needHeader,
        'tmp.dir': tempDirectory,
        'flush.line.count': flushLineCount,
        'rotate.interval.ms': rotateInterval,
        'data.econde': currFileEncoding,
      } = res.data.result.configs;

      const currTopic = JSON.parse(topic);
      const prevHdfs = JSON.parse(hdfs);

      const isDiffHdfs = prevHdfs !== currHdfs ? true : false;
      const _currHdfs = isDiffHdfs ? currHdfs : prevHdfs;
      const _needHeader = needHeader === 'true' ? true : false;

      this.setState({
        currTopic,
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
      const mostRecent = hdfses.reduce(
        (prev, curr) => (prev.lastModified > curr.lastModified ? prev : curr),
      );

      const _currHdfs = _.isEmpty(currHdfs) ? mostRecent : currHdfs;

      this.setState({ hdfses: [_currHdfs], currHdfs: _currHdfs });
    } else {
      this.setState({ isRedirect: true });
      toastr.error(MESSAGES.NO_CONFIGURATION_FOUND_ERROR);
    }
  };

  fetchTopics = async topicId => {
    if (!_.isUuid(topicId)) return;

    const res = await fetchTopics();
    const topics = _.get(res, 'data.result', []);

    if (!_.isEmpty(topics)) {
      const currTopic = topics.find(topic => topic.uuid === topicId);
      this.setState({ topics, currTopic });
    } else {
      toastr.error(MESSAGES.INVALID_TOPIC_ID);
      this.setState({ isRedirect: true });
    }
  };

  fetchPipeline = async pipelineId => {
    if (!_.isUuid(pipelineId)) return;

    const res = await fetchPipeline(pipelineId);
    const pipelines = _.get(res, 'data.result', null);

    if (pipelines) {
      this.setState({ pipelines });

      const sinkId = _.get(this.props.match, 'params.sinkId', null);

      if (sinkId) {
        this.props.loadGraph(pipelines);
      }
    }
  };

  updatePipeline = async (uuid, params) => {
    const res = await updatePipeline({ uuid, params });
    const pipelines = _.get(res, 'data.result', []);

    if (!_.isEmpty(pipelines)) {
      this.setState({ pipelines });
      this.props.loadGraph(pipelines);
    }
  };

  handleChangeInput = ({ target: { name, value } }) => {
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
    const { uuid } = options[selectedIdx].dataset;

    const current = this.selectMaps[name];

    this.setState(
      () => {
        return {
          [current]: {
            name: value,
            uuid,
          },
        };
      },
      () => {
        this.props.updateHasChanges(true);
      },
    );
  };

  save = _.debounce(async () => {
    const { updateHasChanges, history, match } = this.props;
    const {
      currHdfs,
      currTopic,
      writePath,
      needHeader,
      tempDirectory,
      flushLineCount,
      rotateInterval,
      currFileEncoding,
    } = this.state;

    const sinkId = _.get(match, 'params.sinkId', null);
    const sourceId = _.get(match, 'params.sourceId', null);
    const isCreate = _.isNull(sinkId) ? true : false;
    const hasSourceId = _.isNull(sourceId) ? false : true;

    const params = {
      name: 'untitled sink',
      schema: [],
      className: 'hdfs',
      topics: [currTopic.uuid],
      numberOfTasks: 1,
      configs: {
        topic: JSON.stringify(currTopic),
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

    const res = isCreate
      ? await createSink(params)
      : await updateSink({ uuid: sinkId, params });

    const uuid = _.get(res, 'data.result.uuid');

    if (uuid) {
      updateHasChanges(false);
      if (isCreate && !hasSourceId) history.push(`${match.url}/__/${uuid}`);
      if (isCreate && hasSourceId) history.push(`${match.url}/${uuid}`);
    }
  }, 1000);

  render() {
    const {
      topics,
      currTopic,
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
          <FormGroup data-testid="read-from-topic">
            <Label>Read from topic</Label>
            <Select
              isObject
              name="topics"
              list={topics}
              selected={currTopic}
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
              handleChange={this.handleChangeInput}
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
              handleChange={this.handleChangeInput}
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
              handleChange={this.handleChangeInput}
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
              handleChange={this.handleChangeInput}
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

export default PipelineSinkPage;
