import React from 'react';
import styled from 'styled-components';
import toastr from 'toastr';

import AppWrapper from '../common/AppWrapper';
import { validate, save } from '../../apis/configurationApis';
import { Input, Button } from '../common/Form';
import { submitButton, cancelButton } from '../../theme/buttonTheme';
import { lightBlue, lighterGray } from '../../theme/variables';
import { get, isDefined } from '../../utils/helpers';

const FormInner = styled.div`
  padding: 45px 30px;
`;

const FormGroup = styled.div`
  display: flex;
  flex-direction: column;
`;

const Actions = styled.div`
  display: flex;
  padding: 20px;
  border-top: 1px solid ${lighterGray};
`;

const ActionGroup = styled.div`
  margin-left: auto;
`;

const Label = styled.label`
  color: ${lightBlue};
  margin-bottom: 20px;
`;

const CancelButton = styled(Button)`
  margin-right: 10px;
`;

class ConfigurationPage extends React.Component {
  state = {
    connectionUrl: '',
    target: 'hdfs',
  };

  handleChange = ({ target: { value } }) => {
    this.setState({ connectionUrl: value });
  };

  handleCancel = e => {
    e.preventDefault();
    this.props.history.goBack();
  };

  handleSave = async e => {
    e.preventDefault();
    const { connectionUrl: url, target } = this.state;
    const res = await save({ url, target });

    const _res = get(res, 'data.isSuccess', undefined);

    if (isDefined(_res)) {
      toastr.success('Configuration saved!');
    }
  };

  handleTest = async e => {
    e.preventDefault();
    const { connectionUrl: url, target } = this.state;
    const res = await validate({ url, target });

    const _res = get(res, 'data.isSuccess', undefined);

    if (isDefined(_res)) {
      toastr.success('Test passed!');
    }
  };

  render() {
    return (
      <AppWrapper title="Configuration">
        <form>
          <FormInner>
            <FormGroup>
              <Label>HDFS connection URL</Label>
              <Input
                type="text"
                width="250px"
                placeholder="http://localhost:5050"
                value={this.state.connectionUrl}
                handleChange={this.handleChange}
              />
            </FormGroup>
          </FormInner>

          <Actions>
            <Button text="Test connection" handleClick={this.handleTest} />
            <ActionGroup>
              <CancelButton
                text="cancel"
                theme={cancelButton}
                handleClick={this.handleCancel}
              />
              <Button
                text="Save"
                theme={submitButton}
                handleClick={this.handleSave}
              />
            </ActionGroup>
          </Actions>
        </form>
      </AppWrapper>
    );
  }
}

export default ConfigurationPage;
