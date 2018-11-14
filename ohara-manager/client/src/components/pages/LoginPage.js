import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import toastr from 'toastr';
import DocumentTitle from 'react-document-title';
import { Redirect } from 'react-router-dom';

import * as URL from 'constants/urls';
import * as _ from 'utils/commonUtils';
import * as LOGIN_PAGE from 'constants/login';
import * as MESSAGES from 'constants/messages';
import { Input, Button } from 'common/Form';
import { setUserKey } from 'utils/authUtils';
import { login } from 'apis/authApis';
import { LOGIN } from 'constants/documentTitles';
import { primaryBtn } from 'theme/btnTheme';
import {
  white,
  radiusNormal,
  lighterGray,
  darkerBlue,
  shadowNormal,
} from 'theme/variables';

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

    const isSuccess = _.get(res, 'data.isSuccess', false);

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
