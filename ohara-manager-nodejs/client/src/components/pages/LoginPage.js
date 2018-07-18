import React from 'react';
import styled from 'styled-components';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';

import { setUserKey } from '../../utils/authHelpers';
import { login } from '../../apis/authApis';
import { isDefined } from '../../utils/helpers';
import { HOME } from '../../constants/url';
import * as LOGIN_PAGE from '../../constants/login';
import * as MESSAGE from '../../constants/message';

const FormContainer = styled.div`
  display: flex;
  height: calc(100vh - 80px);
`;

const Form = styled.form`
  width: 100%;
  max-width: 330px;
  padding: 15px;
  margin: auto;
  text-align: center;
`;

const UserInput = styled.input`
  margin-bottom: -1px;
  border-bottom-right-radius: 0;
  border-bottom-left-radius: 0;
`;

const PasswordInput = styled.input`
  margin-bottom: 10px;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
`;

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

    if (isDefined(res)) {
      this.setState({ redirect: true });
      setUserKey(res.data.token);
      toastr.success(MESSAGE.LOGIN_SUCCESS);
    }
  };

  render() {
    const { username, password, redirect } = this.state;

    if (redirect) {
      return <Redirect to={HOME} />;
    }

    return (
      <FormContainer className="form-container">
        <Form className="form-login" onSubmit={this.handleSubmit}>
          <h3 className="h3">{LOGIN_PAGE.PAGE_HEADING}</h3>
          <UserInput
            id="username"
            className="form-control"
            type="text"
            placeholder={LOGIN_PAGE.USERNAME_PLACEHOLDER}
            value={username}
            onChange={this.handleChange}
          />
          <PasswordInput
            id="password"
            type="password"
            className="form-control"
            placeholder={LOGIN_PAGE.PASSWORD_PLACEHOLDER}
            value={password}
            onChange={this.handleChange}
          />
          <button type="submit" className="btn btn-lg btn-primary btn-block">
            {LOGIN_PAGE.SUBMIT_BUTTON_TEXT}
          </button>
        </Form>
      </FormContainer>
    );
  }
}

export default LoginPage;
