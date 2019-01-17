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
import ContentLoader from 'react-content-loader';

const Loader = props => {
  const random = Math.random() * (1 - 0.7) + 0.7;
  return (
    <ContentLoader
      height={40}
      width={1060}
      speed={2}
      primaryColor="#d9d9d9"
      secondaryColor="#ecebeb"
      {...props}
    >
      <rect x="0" y="15" rx="4" ry="4" width="6" height="6.4" />
      <rect x="34" y="13" rx="6" ry="6" width={200 * random} height="12" />
      <rect x="633" y="13" rx="6" ry="6" width={23 * random} height="12" />
      <rect x="653" y="13" rx="6" ry="6" width={78 * random} height="12" />
      <rect x="755" y="13" rx="6" ry="6" width={117 * random} height="12" />
      <rect x="938" y="13" rx="6" ry="6" width={83 * random} height="12" />

      <rect x="0" y="39" rx="6" ry="6" width="1060" height=".3" />
    </ContentLoader>
  );
};

const TableLoader = () => (
  <React.Fragment>
    {Array(10)
      .fill('')
      .map((e, i) => (
        <Loader key={i} style={{ opacity: Number(2 / i).toFixed(1) }} />
      ))}
  </React.Fragment>
);

export default TableLoader;
