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
import DocumentTitle from 'react-document-title';
import { Redirect } from 'react-router-dom';
import { get } from 'lodash';

import * as URL from 'constants/urls';
import * as LOGIN_PAGE from 'constants/login';
import * as MESSAGES from 'constants/messages';
import { Input, Button } from 'common/Form';
import { setUserKey } from 'utils/authUtils';
import { login } from 'api/authApi';
import { LOGIN } from 'constants/documentTitles';
import { primaryBtn } from 'theme/btnTheme';

const FormContainer = styled.div`
  display: flex;
  height: calc(100vh - 80px);
`;

const Heading3 = styled.h3`
  font-size: 24px;
  color: ${props => props.theme.darkerBlue};
  text-transform: uppercase;
`;

Heading3.displayName = 'Heading3';

const Form = styled.form`
  max-width: 330px;
  padding: 25px 40px 70px 40px;
  margin: auto;
  text-align: center;
  background-color: ${props => props.theme.white};
  border-radius: ${props => props.theme.radiusNormal};
  border: 1px solid ${props => props.theme.lighterGray};
  box-shadow: ${props => props.theme.shadowNormal};
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
  static propTypes = {
    updateLoginState: PropTypes.func.isRequired,
  };

  state = {
    username: '',
    password: '',
    isRedirect: false,
  };

  handleChange = ({ target: { value, name } }) => {
    this.setState({ [name]: value });
  };

  handleSubmit = async e => {
    e.preventDefault();
    const { username, password } = this.state;

    const res = await login({
      username,
      password,
    });

    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      this.setState({ isRedirect: true });
      setUserKey(res.data.token);
      this.props.updateLoginState(true);
      toastr.success(MESSAGES.LOGIN_SUCCESS);
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
                name="username"
                type="text"
                placeholder={LOGIN_PAGE.USERNAME_PLACEHOLDER}
                value={username}
                width="250px"
                height="45px"
                handleChange={this.handleChange}
                data-testid="username"
              />

              <PasswordInput
                name="password"
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
                theme={primaryBtn}
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
