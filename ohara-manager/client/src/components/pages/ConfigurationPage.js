import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import toastr from 'toastr';
import DocumentTitle from 'react-document-title';
import { Prompt } from 'react-router-dom';

import * as _ from 'utils/commonUtils';
import * as MESSAGES from 'constants/messages';
import { AppWrapper } from 'common/Layout';
import { Input, Button, FormGroup, Label } from 'common/Form';
import { primaryBtn, cancelBtn } from 'theme/btnTheme';
import { lighterGray } from 'theme/variables';
import { CONFIGURATION } from 'constants/documentTitles';
import { validateHdfs, saveHdfs, fetchHdfs } from 'apis/configurationApis';

const FormInner = styled.div`
  padding: 45px 30px;
`;

const Actions = styled.div`
  display: flex;
  padding: 20px;
  border-top: 1px solid ${lighterGray};
`;

const ActionGroup = styled.div`
  margin-left: auto;
`;

const CancelBtn = styled(Button)`
  margin-right: 10px;
`;

class ConfigurationPage extends React.Component {
  static propTypes = {
    history: PropTypes.object.isRequired,
  };

  state = {
    connectionName: '',
    connectionUrl: '',
    isFormDirty: false,
    isWorking: false,
    isBtnDisabled: true,
    isValidConnection: false,
  };

  componentDidMount() {
    this.fetchHdfs();
  }

  fetchHdfs = async () => {
    const res = await fetchHdfs();
    const result = _.get(res, 'data.result', []);

    if (!_.isEmptyArr(result)) {
      const mostRecent = _.reduceByProp(res.data.result, 'lastModified');
      const { name: connectionName, uri: connectionUrl } = mostRecent;
      const isBtnDisabled = this.hasEmptyInput([connectionName, connectionUrl]);

      this.setState({ connectionName, connectionUrl, isBtnDisabled });
    }
  };

  handleChange = ({ target: { value, name } }) => {
    this.setState(
      { [name]: value, isFormDirty: true, isBtnDisabled: true },
      () => {
        const { connectionName, connectionUrl } = this.state;
        if (!this.hasEmptyInput([connectionName, connectionUrl])) {
          this.setState({ isBtnDisabled: false });
        }
      },
    );
  };

  hasEmptyInput = inputs => {
    return inputs.some(input => _.isEmptyStr(input));
  };

  handleCancel = e => {
    e.preventDefault();
    this.props.history.goBack();
  };

  handleSave = async () => {
    const { connectionName: name, connectionUrl: uri } = this.state;
    const res = await saveHdfs({ name, uri });
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.CONFIG_SAVE_SUCCESS);
    }
  };

  handleTestConnection = async e => {
    e.preventDefault();
    const { connectionUrl: uri } = this.state;
    this.updateBtn(true);
    const res = await validateHdfs({ uri });
    this.updateBtn(false);

    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
      this.setState({ isFormDirty: false });
      this.handleSave();
    }
  };

  updateBtn = update => {
    this.setState({ isWorking: update, isBtnDisabled: update });
  };

  render() {
    const {
      isFormDirty,
      connectionName,
      connectionUrl,
      isWorking,
      isBtnDisabled,
    } = this.state;
    return (
      <DocumentTitle title={CONFIGURATION}>
        <AppWrapper title="Configuration">
          <Prompt
            when={isFormDirty || isWorking}
            message={MESSAGES.LEAVE_WITHOUT_SAVE}
          />
          <form>
            <FormInner>
              <FormGroup data-testid="connection-name">
                <Label htmlFor="connection-name">Name</Label>
                <Input
                  id="connection-name"
                  name="connectionName"
                  width="250px"
                  placeholder="Connection name"
                  value={connectionName}
                  data-testid="connection-name-input"
                  handleChange={this.handleChange}
                />
              </FormGroup>

              <FormGroup data-testid="connection-url">
                <Label htmlFor="connection-url">HDFS connection URL</Label>
                <Input
                  id="connection-url"
                  name="connectionUrl"
                  width="250px"
                  placeholder="http://localhost:5050"
                  value={connectionUrl}
                  data-testid="connection-url-input"
                  handleChange={this.handleChange}
                />
              </FormGroup>
            </FormInner>

            <Actions>
              <ActionGroup>
                <CancelBtn
                  text="Cancel"
                  theme={cancelBtn}
                  handleClick={this.handleCancel}
                  data-testid="cancel-btn"
                />
                <Button
                  theme={primaryBtn}
                  text="Test connection"
                  isWorking={isWorking}
                  data-testid="test-connection-btn"
                  handleClick={this.handleTestConnection}
                  disabled={isBtnDisabled}
                />
              </ActionGroup>
            </Actions>
          </form>
        </AppWrapper>
      </DocumentTitle>
    );
  }
}

export default ConfigurationPage;
