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

export const useIsSettingsOpen = () => {
  const mapState = useCallback((state) => !!state.ui.settings.isOpen, []);
  return useSelector(mapState);
};

export const usePageNameInSettings = () => {
  const mapState = useCallback((state) => state.ui.settings.pageName, []);
  return useSelector(mapState);
};

export const useOpenSettingsAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    (values) => dispatch(actions.openSettings.trigger(values)),
    [dispatch],
  );
};

export const useCloseSettingsAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.closeSettings.trigger()), [
    dispatch,
  ]);
};
