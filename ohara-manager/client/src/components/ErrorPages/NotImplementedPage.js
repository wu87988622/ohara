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
import { useHistory } from 'react-router-dom';

import { ReactComponent as NotImplemented } from 'images/not-implemented.svg';
import { Wrapper } from './ErrorPageStyles';

const NotFoundPage = () => {
  const history = useHistory();

  return (
    <Wrapper>
      <Typography variant="h1">
        501: The version of the Ohara API is not compatible!
      </Typography>
      <Typography variant="body1">
        The version of the Ohara API is not compatible, please update as soon as
        possible.
      </Typography>

      <NotImplemented width="680" />

      <Typography variant="h3">Current version of your system</Typography>

      <Typography variant="h3">Recommended version</Typography>

      <Button
        variant="outlined"
        color="primary"
        onClick={() => history.push('/')}
      >
        DOWNLOAD
      </Button>
    </Wrapper>
  );
};

export default NotFoundPage;
