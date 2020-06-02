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

import { useCallback, useEffect } from 'react';
import { isEmpty } from 'lodash';
import { useDispatch, useSelector } from 'react-redux';

import * as hooks from 'hooks';
import * as actions from 'store/actions';

export const useIsAppReady = () =>
  useSelector(useCallback((state) => !!state.ui.app.lastUpdated, []));

export const useInitializeApp = (workspaceName, pipelineName) => {
  const isAppReady = useIsAppReady();
  const dispatch = useDispatch();
  useEffect(() => {
    if (isAppReady) return;
    dispatch(actions.initializeApp.trigger({ workspaceName, pipelineName }));
  }, [dispatch, isAppReady, pipelineName, workspaceName]);
};

export const useWelcome = () => {
  const isAppReady = useIsAppReady();
  const allWorkspaces = hooks.useAllWorkspaces();
  const wasIntroOpened = hooks.useWasIntroOpened();
  const openIntro = hooks.useOpenIntroAction();

  useEffect(() => {
    if (isAppReady && !wasIntroOpened && isEmpty(allWorkspaces)) openIntro();
  }, [isAppReady, allWorkspaces, wasIntroOpened, openIntro]);
};
