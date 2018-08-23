import React from 'react';
import styled from 'styled-components';
import toastr from 'toastr';
import DocumentTitle from 'react-document-title';
import { Redirect } from 'react-router-dom';

import { Input, Button } from '../common/Form';
import { setUserKey } from '../../utils/authHelpers';
import { login } from '../../apis/authApis';
import { LOGIN } from '../../constants/documentTitles';
import * as URL from '../../constants/url';
import {
  white,
  radiusNormal,
  lighterGray,
  darkerBlue,
  shadowNormal,
} from '../../theme/variables';
import * as _ from '../../utils/helpers';

import { submitButton } from '../../theme/buttonTheme';

import * as LOGIN_PAGE from '../../constants/login';
import * as MESSAGE from '../../constants/message';

const FormContainer = styled.div`
  display: flex;
  height: calc(100vh - 80px);
`;

const Heading3 = styled.h3`
  font-size: 24px;
  color: ${darkerBlue};
  text-transform: uppercase;
`;

Heading3.displayName = 'Heading3';

const Form = styled.form`
  max-width: 330px;
  padding: 25px 40px 70px 40px;
  margin: auto;
  text-align: center;
  background-color: ${white};
  border-radius: ${radiusNormal};
  border: 1px solid ${lighterGray};
  box-shadow: ${shadowNormal};
`;

Form.displayName = 'Form';

const FormInner = styled.div`
  max-width: 250px;
`;

const UsernameInput = styled(Input)`
  margin-bottom: 10px;
`;

UsernameInput.displayName = 'UsernameInput';

const PasswordInput = styled(Input)`
  margin-bottom: 10px;
`;

PasswordInput.displayName = 'PasswordInput';

class LoginPage extends React.Component {
  state = {
    username: '',
    password: '',
    isRedirect: false,
  };

  handleChange = ({ target: { value, id: field } }) => {
    this.setState({ [field]: value });
  };

  handleSubmit = async e => {
    e.preventDefault();
    const { username, password } = this.state;

    const res = await login({
      username,
      password,
    });

    const isSuccess = _.get(res, 'data.isSuccess', undefined);

    if (_.isDefined(isSuccess) && isSuccess) {
      this.setState({ isRedirect: true });
      setUserKey(res.data.token);
      this.props.updateLoginState(true);
      toastr.success(MESSAGE.LOGIN_SUCCESS);
    }
  };

  render() {
    const { username, password, isRedirect } = this.state;

    if (isRedirect) {
      return <Redirect to={URL.HOME} />;
    }

    return (
      <DocumentTitle title={LOGIN}>
        <FormContainer>
          <Form data-testid="login-form" onSubmit={this.handleSubmit}>
            <FormInner>
              <Heading3>{LOGIN_PAGE.PAGE_HEADING}</Heading3>

              <UsernameInput
                id="username"
                type="text"
                placeholder={LOGIN_PAGE.USERNAME_PLACEHOLDER}
                value={username}
                width="250px"
                height="45px"
                handleChange={this.handleChange}
                data-testid="username"
              />

              <PasswordInput
                id="password"
                type="password"
                placeholder={LOGIN_PAGE.PASSWORD_PLACEHOLDER}
                value={password}
                width="250px"
                height="45px"
                handleChange={this.handleChange}
                data-testid="password"
              />

              <Button
                width="100%"
                theme={submitButton}
                text={LOGIN_PAGE.SUBMIT_BUTTON_TEXT}
              />
            </FormInner>
          </Form>
        </FormContainer>
      </DocumentTitle>
    );
  }
}

export default LoginPage;
