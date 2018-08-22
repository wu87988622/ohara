import React from 'react';
import styled from 'styled-components';
import toastr from 'toastr';
import DocumentTitle from 'react-document-title';
import { Prompt } from 'react-router-dom';

import { AppWrapper } from '../common/Layout';
import {
  validateHdfs,
  saveHdfs,
  fetchHdfs,
} from '../../apis/configurationApis';
import { Input, Button, FormGroup, Label } from '../common/Form';
import { submitButton, cancelButton } from '../../theme/buttonTheme';
import { lighterGray } from '../../theme/variables';
import { get, isDefined } from '../../utils/helpers';
import { LEAVE_WITHOUT_SAVE } from '../../constants/message';
import { CONFIGURATION } from '../../constants/documentTitles';

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

const CancelButton = styled(Button)`
  margin-right: 10px;
`;

class ConfigurationPage extends React.Component {
  state = {
    connectionName: '',
    connectionUrl: '',
    isFormDirty: false,
    isWorking: false,
    isValidConnection: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const res = await fetchHdfs();

    const _result = get(res, 'data.result');

    if (_result && _result.length > 0) {
      const target = res.data.result.reduce(
        (prev, curr) => (prev.lastModified > curr.lastModified ? prev : curr),
      );
      const { name: connectionName, uri: connectionUrl } = target;
      this.setState({ connectionName, connectionUrl });
    }
  };

  handleChange = ({ target: { value, id } }) => {
    this.setState({ [id]: value, isFormDirty: true });
  };

  handleCancel = e => {
    e.preventDefault();
    this.props.history.goBack();
  };

  handleSave = async () => {
    const { connectionName: name, connectionUrl: uri } = this.state;
    const res = await saveHdfs({ name, uri });

    const _res = get(res, 'data.isSuccess', false);

    if (isDefined(_res)) {
      toastr.success('Configuration saved!');
    }
  };

  handleTest = async e => {
    e.preventDefault();
    const { connectionUrl: uri } = this.state;
    this.updateIsWorking(true);
    const res = await validateHdfs({ uri });
    this.updateIsWorking(false);

    const _res = get(res, 'data.isSuccess', false);

    if (_res) {
      toastr.success('Test passed!');
      this.handleSave();
    }
  };

  updateIsWorking = update => {
    this.setState({ isWorking: update });
  };

  render() {
    const {
      isFormDirty,
      connectionName,
      connectionUrl,
      isWorking,
    } = this.state;
    return (
      <DocumentTitle title={CONFIGURATION}>
        <AppWrapper title="Configuration">
          <Prompt
            when={isFormDirty || isWorking}
            message={LEAVE_WITHOUT_SAVE}
          />
          <form>
            <FormInner>
              <FormGroup>
                <Label>Name</Label>
                <Input
                  id="connectionName"
                  width="250px"
                  placeholder="Connection name"
                  value={connectionName}
                  handleChange={this.handleChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>HDFS connection URL</Label>
                <Input
                  id="connectionUrl"
                  width="250px"
                  placeholder="http://localhost:5050"
                  value={connectionUrl}
                  handleChange={this.handleChange}
                />
              </FormGroup>
            </FormInner>

            <Actions>
              <ActionGroup>
                <CancelButton
                  text="Cancel"
                  theme={cancelButton}
                  handleClick={this.handleCancel}
                />
                <Button
                  theme={submitButton}
                  text="Test connection"
                  isWorking={isWorking}
                  handleClick={this.handleTest}
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
