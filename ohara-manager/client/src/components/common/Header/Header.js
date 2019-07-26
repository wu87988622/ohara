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
import { get } from 'lodash';

import * as URLS from 'constants/urls';
import { ListLoader } from 'components/common/Loader';
import { fetchInfo } from 'api/infoApi';
import { InfoModal } from '../Modal';
import {
  StyledHeader,
  HeaderWrapper,
  Brand,
  Nav,
  Link,
  Btn,
  Icon,
  RightCol,
  Ul,
  LoaderWrapper,
} from './styles';

class Header extends React.Component {
  static propTypes = {
    versionInfo: PropTypes.object,
  };

  state = {
    isVersionModalActive: false,
    isLoading: true,
  };

  componentDidMount() {
    this.fetchInfo();
  }

  fetchInfo = async () => {
    const res = await fetchInfo();
    this.setState({ isLoading: false });
    const info = get(res, 'data.result', null);

    if (info) {
      this.setState({ info });
    }
  };

  handleVersionModalOpen = () => {
    this.setState({ isVersionModalActive: true });
  };

  handleVersionModalClose = () => {
    this.setState({ isVersionModalActive: false });
  };

  render() {
    const { isVersionModalActive, info = {}, isLoading } = this.state;

    const { versionInfo = {}, mode = '' } = info;
    const { version, revision, date } = versionInfo;

    return (
      <StyledHeader>
        <InfoModal
          title="Ohara version"
          isActive={isVersionModalActive}
          contentLabel="VersionModal"
          ariaHideApp={false}
          width="440px"
          onRequestClose={this.handleVersionModalClose}
          handleCancel={this.handleVersionModalClose}
          testId="info-modal"
        >
          {isLoading ? (
            <LoaderWrapper>
              <ListLoader />
            </LoaderWrapper>
          ) : (
            <Ul data-testid="info-list">
              <li>
                <span className="item">Mode:</span>
                <span className="content">{mode}</span>
              </li>
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
          )}
        </InfoModal>
        <HeaderWrapper>
          <Brand to={URLS.HOME}>Ohara Stream</Brand>
          <Nav>
            <Link
              activeClassName="active"
              data-testid="pipelines-link"
              to={URLS.PIPELINES}
            >
              <Icon
                className="fas fa-code-branch"
                data-testid="pipelines-icon"
              />
              <span>Pipelines</span>
            </Link>

            <Link
              activeClassName="active"
              data-testid="nodes-link"
              to={URLS.NODES}
            >
              <Icon className="fas fa-sitemap" data-testid="nodes-icon" />
              <span>Nodes</span>
            </Link>

            <Link
              activeClassName="active"
              data-testid="workspaces-link"
              to={URLS.WORKSPACES}
            >
              <Icon
                className="fas fa-project-diagram"
                data-testid="workspaces-icon"
              />
              <span>Workspaces</span>
            </Link>
          </Nav>

          <RightCol>
            <Btn
              data-tip="View ohara version"
              data-testid="version-btn"
              onClick={this.handleVersionModalOpen}
            >
              <i className="fas fa-info-circle" />
            </Btn>
          </RightCol>
        </HeaderWrapper>
      </StyledHeader>
    );
  }
}

export default Header;
