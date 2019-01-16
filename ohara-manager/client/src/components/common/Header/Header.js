import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { NavLink } from 'react-router-dom';

import * as URLS from 'constants/urls';
import * as _ from 'utils/commonUtils';
import NAVS from 'constants/navs';
import ConfigurationModal from 'pages/ConfigurationModal';
import { fetchCluster } from 'apis/clusterApis';
import { InfoModal } from '../Modal';
import {
  white,
  whiteSmoke,
  blue,
  dimBlue,
  darkerBlue,
  lightBlue,
  lighterGray,
} from 'theme/variables';

const StyledHeader = styled.div`
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

StyledHeader.displayName = 'StyledHeader';

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

const Btn = styled.button`
  border: none;
  color: ${dimBlue};
  font-size: 18px;
  background-color: transparent;

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

const RightCol = styled.div`
  margin-left: auto;
`;

const Ul = styled.ul`
  padding: 22px 25px;

  li {
    margin-bottom: 15px;
    display: flex;
    align-items: center;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .item {
    margin-right: 10px;
    padding: 13px 15px;
    color: ${darkerBlue};
    background-color: ${whiteSmoke};
  }

  .content {
    color: ${lightBlue};
  }

  .item,
  .content {
    font-size: 13px;
  }
`;

class Header extends React.Component {
  state = {
    isConfigModalActive: false,
    isVersionModalActive: false,
    partitions: '',
    replicationFactor: '',
  };

  static propTypes = {
    isLogin: PropTypes.bool.isRequired,
    versionInfo: PropTypes.object,
  };

  componentDidMount() {
    this.fetchCluster();
  }

  fetchCluster = async () => {
    const res = await fetchCluster();
    const versionInfo = _.get(res, 'data.result.versionInfo', null);
    if (versionInfo) {
      this.setState({ versionInfo });
    }
  };

  handleConfigModalOpen = () => {
    this.setState({ isConfigModalActive: true });
  };

  handleConfigModalClose = () => {
    this.setState({ isConfigModalActive: false });
  };

  handleVersionModalOpen = () => {
    this.setState({ isVersionModalActive: true });
  };

  handleVersionModalClose = () => {
    this.setState({ isVersionModalActive: false });
  };

  render() {
    const { isLogin } = this.props;
    const {
      isConfigModalActive,
      isVersionModalActive,
      versionInfo,
    } = this.state;

    if (!versionInfo) return null;

    const { version, revision, date } = versionInfo;

    return (
      <StyledHeader>
        <ConfigurationModal
          isActive={isConfigModalActive}
          handleClose={this.handleConfigModalClose}
        />

        <InfoModal
          title="Ohara version"
          isActive={isVersionModalActive}
          contentLabel="VersionModal"
          ariaHideApp={false}
          width="440px"
          onRequestClose={this.handleVersionModalClose}
          handleCancel={this.handleVersionModalClose}
        >
          <Ul>
            <li>
              <span className="item">Version:</span>
              <span className="content">{version}</span>
            </li>
            <li>
              <span className="item">Revision:</span>
              <span className="content">{revision}</span>
            </li>
            <li>
              <span className="item">Build date:</span>
              <span className="content">{date}</span>
            </li>
          </Ul>
        </InfoModal>
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

          <RightCol>
            <Btn
              data-testid="version-btn"
              onClick={this.handleVersionModalOpen}
            >
              <i className="fas fa-info-circle" />
            </Btn>

            <Btn data-testid="config-btn" onClick={this.handleConfigModalOpen}>
              <i className="fas fa-cog" />
            </Btn>

            {/* v0.2 先不要顯示 Login */}
            {false && (
              <Login
                data-testid="login-state"
                to={isLogin ? URLS.LOGOUT : URLS.LOGIN}
              >
                {isLogin ? 'Log out' : 'Log in'}
              </Login>
            )}
          </RightCol>
        </HeaderWrapper>
      </StyledHeader>
    );
  }
}

export default Header;
