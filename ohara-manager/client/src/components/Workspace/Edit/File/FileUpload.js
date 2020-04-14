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

import React, { useRef, cloneElement, useImperativeHandle } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import * as hooks from 'hooks';

const Wrapper = styled.div`
  .input {
    display: none;
  }
`;

const FileUpload = React.forwardRef(({ children, accept = '.jar' }, ref) => {
  const createFile = hooks.useCreateFileAction();
  const inputEl = useRef(null);

  const Trigger = () => {
    const handleClick = () => inputEl.current.click();
    if (children) {
      // Add event handler to React.DOM element dynamically
      return cloneElement(children, {
        onClick: handleClick,
      });
    }
    return null;
  };

  const handleFileSelect = event => {
    const file = event.target.files[0];

    // TODO: switch to the file tab if:
    // 1. file successfully uploaded
    // 2. the user is not at the file tab
    if (file) createFile(file);
  };

  useImperativeHandle(ref, () => ({
    click: () => inputEl.current.click(),
  }));

  return (
    <Wrapper>
      <input
        type="file"
        ref={inputEl}
        className="input"
        accept={accept}
        onChange={handleFileSelect}
        onClick={event => {
          /* Allow file to be added multiple times */
          event.target.value = null;
        }}
      />
      <Trigger />
    </Wrapper>
  );
});

FileUpload.propTypes = {
  children: PropTypes.node,
  accept: PropTypes.string,
};

export default FileUpload;
