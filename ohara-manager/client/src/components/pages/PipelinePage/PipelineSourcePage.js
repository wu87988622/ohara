import React from 'react';
import styled from 'styled-components';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';

import { Box } from '../../common/Layout';
import { Warning } from '../../common/Messages';
import { validateRdb } from '../../../apis/pipelinesApis';
import { H5 } from '../../common/Headings';
import { lightBlue, whiteSmoke } from '../../../theme/variables';
import { primaryBtn } from '../../../theme/btnTheme';
import { Input, Select, FormGroup, Label, Button } from '../../common/Form';
import { fetchTopics } from '../../../apis/topicApis';
import { queryRdb } from '../../../apis/pipelinesApis';
import * as URLS from '../../../constants/urls';
import * as _ from '../../../utils/helpers';
import * as MESSAGES from '../../../constants/messages';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
`;

const Form = styled.form`
  display: flex;
`;

const LeftCol = styled.div`
  width: 250px;
  padding-right: 45px;
  margin-right: 45px;
  border-right: 2px solid ${whiteSmoke};
  box-sizing: content-box;
`;

const Fieldset = styled.fieldset`
  border: none;
  position: relative;

  &:after {
    content: '';
    background-color: red;
    width: 100%;
    height: 100%;
    display: ${props => (props.disabled ? 'block' : 'none')};
    position: absolute;
    top: 0;
    left: 0;
    background: rgba(255, 255, 255, 0.5);
    cursor: not-allowed;
  }
`;

const RightCol = styled.div`
  width: 250px;
`;

const Actions = styled.div`
  display: flex;
  justify-content: flex-end;
`;

class PipelineSourcePage extends React.Component {
  state = {
    databases: [{ name: 'mysql', uuid: '1' }, { name: 'oracle', uuid: '2' }],
    currentDatabase: { name: 'oracle', uuid: '2' },
    tables: [],
    currentTable: {},
    writeTopics: [],
    currentWriteTopic: {},
    username: '',
    password: '',
    url: '',
    timestamp: '',
    isBtnWorking: false,
    isFormDisabled: true,
    isRedirect: false,
  };

  componentDidMount() {
    this.fetchTopic();
  }

  fetchTopic = async () => {
    const topicId = _.get(this.props.match, 'params.topicId', null);

    if (this.isValidTopicId(topicId)) {
      const res = await fetchTopics();
      const writeTopics = _.get(res, 'data.result', []);

      if (!_.isEmptyArr(writeTopics)) {
        const currentWriteTopic = this.getCurrentTopic(writeTopics, topicId);
        this.setState({ writeTopics, currentWriteTopic });
      } else {
        toastr.error(MESSAGES.INVALID_TOPIC_ID);
        this.setState({ isRedirect: true });
      }
    }
  };

  fetchRdbTables = async () => {
    const { url, user, password } = this.state;
    const res = await queryRdb({ url, user, password });
    const tables = _.get(res, 'data.result', null);

    if (!_.isNull(tables)) {
      this.setState({ tables });
    }

    console.log(res);
  };

  isValidTopicId = topicId => {
    return !_.isNull(topicId) && _.isUuid(topicId);
  };

  getCurrentTopic = (topics, targetTopic) => {
    return topics.find(t => t.uuid === targetTopic);
  };

  handleChangeInput = ({ target: { name, value } }) => {
    this.setState({ [name]: value });
  };

  handleChangeSelect = ({ target }) => {
    const { name, options, value } = target;
    const selectedIdx = options.selectedIndex;
    const { uuid } = options[selectedIdx].dataset;

    const upper = name.charAt(0).toUpperCase();
    const current = `current${upper}${name.slice(1, -1)}`;

    this.setState({
      [current]: {
        [name]: value,
        uuid,
      },
    });
  };

  handleTest = async e => {
    e.preventDefault();
    const { username: user, password, url: uri } = this.state;

    this.updateBtn(true);
    const res = await validateRdb({ user, password, uri });
    this.updateBtn(false);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
      this.setState({ isFormDisabled: false });
      this.fetchRdbTables();
    }
  };

  updateBtn = update => {
    this.setState({ isBtnWorking: update });
  };

  render() {
    const {
      url,
      username,
      password,
      databases,
      currentDatabase,
      isBtnWorking,
      tables,
      currentTable,
      timestamp,
      writeTopics,
      currentWriteTopic,
      isFormDisabled,
      isRedirect,
    } = this.state;

    if (isRedirect) {
      return <Redirect to={URLS.PIPELINE} />;
    }

    return (
      <Box>
        <H5Wrapper>JDBC connection</H5Wrapper>
        <Form>
          <LeftCol>
            <Fieldset disabled={isBtnWorking}>
              <FormGroup>
                <Label>Database</Label>
                <Select
                  name="databases"
                  list={databases}
                  selected={currentDatabase}
                  width="250px"
                  data-testid="dataset-select"
                  handleChange={this.handleChangeSelect}
                />
              </FormGroup>

              <FormGroup>
                <Label>URL</Label>
                <Input
                  name="url"
                  width="250px"
                  placeholder="jdbc:mysql://localhost:3030/my-db"
                  value={url}
                  data-testid="url-input"
                  handleChange={this.handleChangeInput}
                />
              </FormGroup>

              <FormGroup>
                <Label>User name</Label>
                <Input
                  name="username"
                  width="250px"
                  placeholder="John doe"
                  value={username}
                  data-testid="username-input"
                  handleChange={this.handleChangeInput}
                />
              </FormGroup>

              <FormGroup>
                <Label>Password</Label>
                <Input
                  type="password"
                  name="password"
                  width="250px"
                  placeholder="password"
                  value={password}
                  data-testid="password-input"
                  handleChange={this.handleChangeInput}
                />
              </FormGroup>
            </Fieldset>

            <Actions>
              <Button
                theme={primaryBtn}
                text="Test connection"
                isWorking={isBtnWorking}
                data-testid="test-connection-btn"
                handleClick={this.handleTest}
              />
            </Actions>
          </LeftCol>
          <RightCol>
            <Fieldset disabled={isFormDisabled}>
              <FormGroup>
                <Label>Tables</Label>
                <Select
                  name="tables"
                  list={tables}
                  selected={currentTable}
                  width="250px"
                  data-testid="table-select"
                  handleChange={this.handleChangeSelect}
                />
              </FormGroup>

              <FormGroup>
                <Label>Timestamp column</Label>
                <Input
                  name="timestamp"
                  width="250px"
                  placeholder="120"
                  value={timestamp}
                  data-testid="timestamp-input"
                  handleChange={this.handleChangeInput}
                />
              </FormGroup>

              <FormGroup>
                <Label>Write topic</Label>
                <Select
                  name="writeTopics"
                  list={writeTopics}
                  selected={currentWriteTopic}
                  width="250px"
                  data-testid="write-topic-select"
                  handleChange={this.handleChangeSelect}
                />
              </FormGroup>
            </Fieldset>

            <Warning text="You need to test JDBC connection before filling out the above form" />
          </RightCol>
        </Form>
      </Box>
    );
  }
}

export default PipelineSourcePage;
