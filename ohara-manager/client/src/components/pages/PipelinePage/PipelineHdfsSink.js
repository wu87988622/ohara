import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';

import * as MESSAGES from 'constants/messages';
import * as pipelinesApis from 'apis/pipelinesApis';
import * as _ from 'utils/commonUtils';
import { fetchSink } from 'utils/pipelineUtils';
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { lightBlue } from 'theme/variables';
import { Input, Select, FormGroup, Label } from 'common/Form';
import { fetchHdfs } from 'apis/configurationApis';
import { CONFIGURATION } from 'constants/urls';

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

class PipelineHdfsSink extends React.Component {
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
    name: '',
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
    const { hasChanges } = this.props;

    const prevSinkId = _.get(prevProps.match, 'params.connectorId', null);
    const currSinkId = _.get(this.props.match, 'params.connectorId', null);
    const isUpdate = prevSinkId !== currSinkId;

    if (hasChanges) {
      this.save();
    }

    if (isUpdate) {
      const { name, id, rules } = this.state.pipelines;

      const params = {
        name,
        rules: { ...rules, '?': currSinkId },
      };

      this.updatePipeline(id, params);
    }
  }

  setDefaults = () => {
    this.setState(({ fileEncodings }) => ({
      currFileEncoding: fileEncodings[0],
    }));
  };

  isConnectorExist = async id => {
    const res = await fetchSink(id);
    if (_.isNull(res)) return;

    return res.isSuccess;
  };

  fetchData = () => {
    const { match } = this.props;
    const sinkId = _.get(match, 'params.connectorId', null);
    const pipelineId = _.get(match, 'params.pipelineId', null);

    this.setDefaults();

    if (sinkId) {
      const fetchHdfsPromise = this.fetchHdfs(sinkId);

      const fetchPipelinePromise = this.fetchPipeline(pipelineId);

      Promise.all([fetchHdfsPromise, fetchPipelinePromise]).then(() => {
        this.isConnectorExist(sinkId) && this.fetchSink(sinkId);
      });

      return;
    }

    this.fetchPipeline(pipelineId);
    this.fetchHdfs();
  };

  fetchSink = async sinkId => {
    const sink = await fetchSink(sinkId);

    if (_.isNull(sink)) return;

    const { currHdfs } = this.state;
    const { name } = sink;
    const {
      topic,
      hdfs,
      writePath,
      needHeader,
      'tmp.dir': tempDirectory,
      'flush.line.count': flushLineCount,
      'rotate.interval.ms': rotateInterval,
      'data.econde': currFileEncoding,
    } = sink.configs;

    const currTopic = JSON.parse(topic);
    const prevHdfs = JSON.parse(hdfs);

    const isDiffHdfs = prevHdfs !== currHdfs ? true : false;
    const _currHdfs = isDiffHdfs ? currHdfs : prevHdfs;
    const _needHeader = needHeader === 'true' ? true : false;

    this.setState({
      name,
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

  fetchPipeline = async pipelineId => {
    if (!pipelineId) return;

    const res = await pipelinesApis.fetchPipeline(pipelineId);
    const pipelines = _.get(res, 'data.result', null);

    if (pipelines) {
      this.setState({ pipelines });

      const sinkId = _.get(this.props.match, 'params.sinkId', null);

      if (sinkId) {
        this.props.loadGraph(pipelines);
      }
    }
  };

  updatePipeline = async (id, params) => {
    const res = await pipelinesApis.updatePipeline({ id, params });
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
    const { updateHasChanges, history, match, isPipelineRunning } = this.props;
    const {
      name,
      currHdfs,
      currTopic,
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
    const topics = _.isEmpty(currTopic) ? [] : [currTopic.topicId];
    const isConnectorExist = !_.isNull(sinkId) && this.isConnectorExist(sinkId);

    const params = {
      name,
      schema: [],
      className: 'hdfs',
      topics,
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

    const res = isConnectorExist
      ? await pipelinesApis.updateSink({ id: sinkId, params })
      : await pipelinesApis.createSink(params);

    const updatedSinkId = _.get(res, 'data.result.id');
    updateHasChanges(false);

    if (updatedSinkId && !isConnectorExist) {
      history.push(`${match.url}/${updatedSinkId}`);
    }
  }, 1000);

  render() {
    const {
      name,
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
          <FormGroup data-testid="name">
            <Label>Name</Label>
            <Input
              name="name"
              width="100%"
              placeholder="HDFS sink name"
              value={name}
              data-testid="name-input"
              handleChange={this.handleChangeInput}
            />
          </FormGroup>
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

export default PipelineHdfsSink;
