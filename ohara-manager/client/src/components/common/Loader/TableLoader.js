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
import styled from 'styled-components';
import ContentLoader from 'react-content-loader';
import Paper from '@material-ui/core/Paper';

const StyledPaper = styled(Paper)`
  padding: 20px;
`;

const Loader = (props) => {
  const random = Math.random() * (1 - 0.7) + 0.7;
  return (
    <ContentLoader
      backgroundColor="#d9d9d9"
      foregroundColor="#ecebeb"
      height={40}
      speed={2}
      width={1060}
      {...props}
    >
      <rect height="6.4" rx="4" ry="4" width="6" x="0" y="15" />
      <rect height="12" rx="6" ry="6" width={200 * random} x="34" y="13" />
      <rect height="12" rx="6" ry="6" width={23 * random} x="633" y="13" />
      <rect height="12" rx="6" ry="6" width={78 * random} x="653" y="13" />
      <rect height="12" rx="6" ry="6" width={117 * random} x="755" y="13" />
      <rect height="12" rx="6" ry="6" width={83 * random} x="938" y="13" />

      <rect height=".3" rx="6" ry="6" width="1060" x="0" y="39" />
    </ContentLoader>
  );
};

const TableLoader = () => (
  <StyledPaper>
    <div className="table-loader" data-testid="table-loader">
      {Array(10)
        .fill('')
        .map((e, i) => (
          <Loader key={i} style={{ opacity: Number(2 / i).toFixed(1) }} />
        ))}
    </div>
  </StyledPaper>
);

export default TableLoader;
