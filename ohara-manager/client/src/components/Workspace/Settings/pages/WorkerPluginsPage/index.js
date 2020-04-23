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
import styled, { css } from 'styled-components';
import Box from '@material-ui/core/Box';

import PluginTable from './PluginTable';
import SharedJarTable from './SharedJarTable';

const Wrapper = styled.div(
  ({ theme }) => css`
    .shared-jars {
      margin-top: ${theme.spacing(6)}px;
    }
  `,
);

export default function() {
  return (
    <Wrapper>
      <Box className="plugins">
        <PluginTable />
      </Box>
      <Box className="shared-jars">
        <SharedJarTable />
      </Box>
    </Wrapper>
  );
}
