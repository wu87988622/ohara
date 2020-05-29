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
import { useHistory } from 'react-router-dom';

import { Button } from 'components/common/Form';
import { ReactComponent as NotFoundSvg } from 'images/page-not-found.svg';
import { Wrapper } from './ErrorPageStyles';

const NotFoundPage = () => {
  const history = useHistory();

  return (
    <Wrapper>
      <Typography variant="h1">
        404: The page you are looking for isn't here
      </Typography>
      <Typography color="textSecondary" variant="body1">
        You either tried some shady route or you came here by mistake. Whichever
        it is, try using the navigation
      </Typography>

      <NotFoundSvg width="680" />
      <Button
        color="primary"
        onClick={() => history.push('/')}
        variant="outlined"
      >
        BACK TO HOME
      </Button>
    </Wrapper>
  );
};

export default NotFoundPage;
