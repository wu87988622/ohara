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
import { isFunction } from 'lodash';

const Wrapper = styled.div`
  .input {
    display: none;
  }
`;

const FileUpload = React.forwardRef(({ accept, children, onUpload }, ref) => {
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

  const handleFileSelect = (event) => {
    const file = event.target.files?.[0];
    if (isFunction(onUpload)) {
      onUpload(event, file);
    }
  };

  useImperativeHandle(ref, () => ({
    click: () => inputEl.current.click(),
  }));

  return (
    <Wrapper>
      <input
        accept={accept}
        className="input"
        onChange={handleFileSelect}
        onClick={(event) => {
          /* Allow file to be added multiple times */
          event.target.value = null;
        }}
        ref={inputEl}
        type="file"
      />
      <Trigger />
    </Wrapper>
  );
});

FileUpload.propTypes = {
  children: PropTypes.node,
  accept: PropTypes.string,
  onUpload: PropTypes.func,
};

FileUpload.defaultProps = {
  children: null,
  accept: '.jar',
  onUpload: () => {},
};

export default FileUpload;
