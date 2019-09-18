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

import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { get } from 'lodash';

import * as URLS from 'constants/urls';
import { ListLoader } from 'components/common/Loader';
import * as infoApi from 'api/infoApi';
import { Dialog } from 'components/common/Mui/Dialog';
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

const Header = () => {
  const [isVersionModalActive, setIsVersionModalActive] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [info, setInfo] = useState({});

  useEffect(() => {
    const fetchInfo = async () => {
      const res = await infoApi.fetchInfo();
      setIsLoading(false);
      const info = get(res, 'data.result', null);

      if (info) setInfo(info);
    };

    fetchInfo();
  }, []);

  const { versionInfo = {}, mode = '' } = info;
  const { version, revision, date } = versionInfo;

  return (
    <StyledHeader>
      <Dialog
        title="Ohara version"
        open={isVersionModalActive}
        handleClose={() => setIsVersionModalActive(false)}
        showActions={false}
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
      </Dialog>
      <HeaderWrapper>
        <Brand to={URLS.HOME}>Ohara Stream</Brand>
        <Nav>
          <Link
            activeClassName="active"
            data-testid="pipelines-link"
            to={URLS.PIPELINES}
          >
            <Icon className="fas fa-code-branch" data-testid="pipelines-icon" />
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
            onClick={() => setIsVersionModalActive(true)}
          >
            <i className="fas fa-info-circle" />
          </Btn>
        </RightCol>
      </HeaderWrapper>
    </StyledHeader>
  );
};

Header.propTypes = {
  versionInfo: PropTypes.object,
};

export default Header;
