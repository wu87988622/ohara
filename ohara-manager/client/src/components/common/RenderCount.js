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

/**
 * The purpose of this component is to help developers avoid to writing
 * redundant and invalid rendering. It is recommended to use it only in
 * the development.
 *
 * Example:
 *
 * <Dialog>
 *    <RenderCount />
 *    <Form>
 *      <Field>
 *        <RenderCount />
 *        <label>First Name</label>
 *        <input name="firstName" placeholder="First Name" />
 *      </Field>
 *    </Form>
 * </Dialog>
 */
export default function RenderCount() {
  const renders = React.useRef(0);

  return <Circle>{++renders.current}</Circle>;
}

const size = 30;
const Circle = styled.i`
  position: absolute;
  top: 0;
  right: 0;
  font-style: normal;
  text-align: center;
  height: ${size}px;
  width: ${size}px;
  line-height: ${size}px;
  border-radius: ${size / 2}px;
  border: 1px solid #ddd;
  background: #eee;
  z-index: 100000;
`;
