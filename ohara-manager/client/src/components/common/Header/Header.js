import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { NavLink } from 'react-router-dom';

import * as URLS from 'constants/urls';
import * as MESSAGES from 'constants/messages';
import * as _ from 'utils/commonUtils';
import NAVS from 'constants/navs';
import Modal from 'pages/PipelinePage/Modal';
import toastr from 'toastr';
import { white, blue, dimBlue, lighterGray } from 'theme/variables';
import {
  validateHdfs,
  saveHdfs,
  fetchHdfs,
  deleteHdfs,
} from 'apis/configurationApis';

const Wrapper = styled.div`
  background-color: ${white};
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  height: 59px;
  border-bottom: 1px solid ${lighterGray};
  padding: 0 50px;
  z-index: 100;
`;

Wrapper.displayName = 'Wrapper';

const HeaderWrapper = styled.header`
  width: 100%;
  height: 100%;
  max-width: 1200px;
  display: flex;
  align-items: center;
  margin: auto;
`;

HeaderWrapper.displayName = 'Header';

const Brand = styled(NavLink)`
  font-family: Merriweather, sans-serif;
  color: ${blue};
  font-size: 24px;
  padding: 0;
  display: block;
`;

Brand.displayName = 'Brand';

const Nav = styled.nav`
  margin-left: 54px;
  background-color: ${white};
`;

Nav.displayName = 'Nav';

const Link = styled(NavLink)`
  color: ${dimBlue};
  font-size: 14px;
  padding: 15px 0;
  margin: 10px 20px;
  position: relative;
  transition: 0.3s all;

  &:hover,
  &.active {
    color: ${blue};
  }
`;

Link.displayName = 'Link';

const ConfigBtn = styled.button`
  border: none;
  color: ${dimBlue};
  font-size: 18px;
  margin-left: auto;

  &:hover,
  &.active {
    color: ${blue};
  }
`;

const Login = styled(Link)`
  margin: 0 10px;
`;

Login.displayName = 'Login';

const Icon = styled.i`
  margin-right: 8px;
`;

Icon.displayName = 'Icon';

class Header extends React.Component {
  state = {
    isFormDirty: false,
    isModalActive: false,
    partitions: '',
    replicationFactor: '',
    connections: [],
    connectionName: '',
    connectionUrl: '',
    connectionUuid: '',
    isBtnDisabled: true,
    isValidConnection: false,
  };
  static propTypes = {
    isLogin: PropTypes.bool.isRequired,
  };

  handleModalOpen = () => {
    this.setState({ isModalActive: true });
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false, isFormDirty: false });
    this.resetModal();
  };

  fetchHdfs = async () => {
    const res = await fetchHdfs();
    const result = _.get(res, 'data.result', []);

    if (!_.isEmptyArr(result)) {
      this.setState({ connections: result });
    }
  };

  handleAddConnection = e => {
    e.preventDefault();
    const { connections } = this.state;
    this.setState({
      connections: [
        ...connections,
        { name: 'Untitled Connection', uuid: '', uri: '' },
      ],
    });
  };

  handleDeleteConnection = async (e, uuid, name) => {
    e.preventDefault();
    const res = await deleteHdfs(uuid);
    const isSuccess = _.get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.CONFIG_DELETE_SUCCESS + name);
      this.fetchHdfs();
      this.resetModal();
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

  handleSave = async e => {
    e.preventDefault();
    const { connectionName: name, connectionUrl: uri } = this.state;
    const isValid = this.state.isValidConnection;
    if (isValid) {
      const res = await saveHdfs({ name, uri });
      const isSuccess = _.get(res, 'data.isSuccess', false);
      if (isSuccess) {
        toastr.success(MESSAGES.CONFIG_SAVE_SUCCESS);
        this.fetchHdfs();
      }
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
      this.setState({ isFormDirty: false, isValidConnection: true });
    }
  };

  handleApplyConnection = async e => {
    e.preventDefault();
    const { connectionName: name, connectionUrl: uri } = this.state;
    this.updateBtn(true);
    const res = await validateHdfs({ uri });
    this.updateBtn(false);

    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
      this.setState({ isFormDirty: false, isValidConnection: true });
      const { connections } = this.state;
      this.setState({
        connections: [...connections, { name: name, uuid: '', uri: uri }],
      });
    }
  };

  hasEmptyInput = inputs => {
    return inputs.some(input => _.isEmptyStr(input));
  };

  updateBtn = update => {
    this.setState({ isCreateHdfsWorking: update, isBtnDisabled: update });
  };

  resetModal = () => {
    this.setState({
      connectionName: '',
      connectionUrl: '',
      connectionUuid: '',
    });
  };

  setModalField = (e, uuid) => {
    const result = this.state.connections.filter(conn => {
      return conn.uuid === uuid;
    });

    this.setState({
      connectionName: result[0].name,
      connectionUrl: result[0].uri,
      connectionUuid: result[0].uuid,
    });
  };

  render() {
    const { isLogin } = this.props;
    const {
      isModalActive,
      connections,
      connectionName,
      connectionUrl,
      connectionUuid,
    } = this.state;

    return (
      <Wrapper>
        <Modal
          isActive={isModalActive}
          handleChange={this.handleChange}
          handleTestConnection={this.handleTestConnection}
          handleSave={this.handleSave}
          handleClose={this.handleModalClose}
          setModalField={this.setModalField}
          fetchHdfs={this.fetchHdfs}
          handleAddConnection={this.handleAddConnection}
          handleDeleteConnection={this.handleDeleteConnection}
          handleApplyConnection={this.handleApplyConnection}
          connections={connections}
          connectionName={connectionName}
          connectionUrl={connectionUrl}
          connectionUuid={connectionUuid}
          isFormDirty
          isCreateHdfsWorking
        />
        <HeaderWrapper>
          <Brand to={URLS.HOME}>Ohara</Brand>
          <Nav>
            {NAVS.map(({ testId, to, iconCls, text }) => {
              return (
                <Link
                  exact
                  activeClassName="active"
                  key={testId}
                  data-testid={testId}
                  to={to}
                >
                  <Icon className={`fas ${iconCls}`} />
                  <span>{text}</span>
                </Link>
              );
            })}
          </Nav>

          <ConfigBtn data-testid="config-btn" onClick={this.handleModalOpen}>
            <i className="fas fa-cog" />
          </ConfigBtn>

          <Login
            data-testid="login-state"
            to={isLogin ? URLS.LOGOUT : URLS.LOGIN}
          >
            {isLogin ? 'Log out' : 'Log in'}
          </Login>
        </HeaderWrapper>
      </Wrapper>
    );
  }
}

export default Header;
