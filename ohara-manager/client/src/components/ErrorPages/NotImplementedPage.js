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
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';
import CircularProgress from '@material-ui/core/CircularProgress';

import { ReactComponent as NotImplemented } from 'images/not-implemented.svg';
import { Wrapper } from './ErrorPageStyles';
import * as inspectApi from 'api/inspectApi';

const NotFoundPage = () => {
  const [managerVersion, setManagerVersion] = React.useState('');
  const [configuratorVersion, setConfiguratorVersion] = React.useState('');

  React.useEffect(() => {
    const fetchConfiguratorInfo = async () => {
      const info = await inspectApi.getConfiguratorInfo();
      setConfiguratorVersion(info.data.versionInfo.version);
    };
    fetchConfiguratorInfo();
  }, []);

  React.useEffect(() => {
    const fetchMangerInfo = async () => {
      const info = await inspectApi.getManagerInfo();
      setManagerVersion(info.data.version);
    };

    fetchMangerInfo();
  }, []);

  return (
    <Wrapper>
      <Typography variant="h1">
        501: The version of the Ohara API is not compatible!
      </Typography>
      <Typography color="textSecondary" variant="body1">
        The version of the Ohara API is not compatible, please update as soon as
        possible.
      </Typography>

      <NotImplemented width="680" />

      {configuratorVersion &&
        (managerVersion ? (
          <>
            <div className="current-version-section">
              <Typography variant="h3">
                Current version from your system
              </Typography>
              <ul>
                <li>
                  <Typography
                    color="textSecondary"
                    variant="body1"
                  >{`oharastream-configurator-${managerVersion}`}</Typography>
                </li>
                <li>
                  <Typography
                    color="textSecondary"
                    variant="body1"
                  >{`oharastream-manager-${configuratorVersion}`}</Typography>
                </li>
              </ul>
            </div>

            <div className="suggestion-section">
              <Typography color="textSecondary" variant="h5">
                Looks like you're using different versions in your services.
                Please use the same version across all services.
              </Typography>
            </div>

            <Button
              color="primary"
              onClick={() =>
                window.open('https://github.com/oharastream/ohara/releases')
              }
              variant="outlined"
            >
              SEE AVAILABLE RELEASES
            </Button>
          </>
        ) : (
          <>
            <Typography color="textSecondary" variant="body1">
              Loading version information from your system...
            </Typography>
            <CircularProgress />
          </>
        ))}
    </Wrapper>
  );
};

export default NotFoundPage;
