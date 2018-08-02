import React from 'react';
import styled from 'styled-components';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';

import { Input, Button } from '../common/Form';
import { setUserKey } from '../../utils/authHelpers';
import { get, isDefined } from '../../utils/helpers';
import { login } from '../../apis/authApis';
import { HOME } from '../../constants/url';
import {
  white,
  radiusNormal,
  lighterGray,
  darkBlue,
} from '../../theme/variables';
import * as LOGIN_PAGE from '../../constants/login';
import * as MESSAGE from '../../constants/message';

const FormContainer = styled.div`
  display: flex;
  height: calc(100vh - 80px);
`;

const Heading3 = styled.h3`
  font-size: 24px;
  color: ${darkBlue};
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
  box-shadow: 0 1px 1px 0px rgba(0, 0, 0, 0.1);
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
    redirect: false,
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

    const isSuccess = get(res, 'data.isSuccess', undefined);

    if (isDefined(isSuccess) && isSuccess) {
      this.setState({ redirect: true });
      setUserKey(res.data.token);
      this.props.updateLoginState(true);
      toastr.success(MESSAGE.LOGIN_SUCCESS);
    }
  };

  render() {
    const { username, password, redirect } = this.state;

    if (redirect) {
      return <Redirect to={HOME} />;
    }

    return (
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
              type="submit"
              width="100%"
              text={LOGIN_PAGE.SUBMIT_BUTTON_TEXT}
            />
          </FormInner>
        </Form>
      </FormContainer>
    );
  }
}

export default LoginPage;
