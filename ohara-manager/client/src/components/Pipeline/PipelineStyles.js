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

import styled, { css } from 'styled-components';
import SplitPane from 'react-split-pane';

export const PaperWrapper = styled.div`
  position: relative;

  /* Subtract the height of Toolbar  */
  height: calc(100vh - 72px);
`;

export const StyledSplitPane = styled(SplitPane)(
  () => css`
    /* Ensure the PropertyView can be seen since it's default top is: auto */
    top: 0;
  `,
);
