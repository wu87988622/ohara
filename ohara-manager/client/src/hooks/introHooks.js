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

import { useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import * as actions from 'store/actions';

export const useIsIntroOpen = () => {
  const mapState = useCallback((state) => !!state.ui.intro.isOpen, []);
  return useSelector(mapState);
};

export const useWasIntroOpened = () => {
  const mapState = useCallback((state) => !!state.ui.intro.opened, []);
  return useSelector(mapState);
};

export const useOpenIntroAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.openIntro.trigger()), [dispatch]);
};

export const useCloseIntroAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.closeIntro.trigger()), [dispatch]);
};
