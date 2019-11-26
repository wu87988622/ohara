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
import styled from 'styled-components';

const Wrapper = styled.div`
  a {
    display: none;
  }
`;

const FileDownload = React.forwardRef(({ url, name }, ref) => (
  <Wrapper>
    <a ref={ref} href={url} download={name}>
      Click to download
    </a>
  </Wrapper>
));

FileDownload.propTypes = {
  url: PropTypes.string.isRequired,
  name: PropTypes.string,
};

export default FileDownload;
