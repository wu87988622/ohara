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

import { useCallback, useMemo, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import * as hooks from 'hooks';
import { hashByGroupAndName } from 'utils/sha';

export const useCreateShabondiAction = () => {
  const dispatch = useDispatch();
  const group = useShabondiGroup();
  return (values, options) =>
    dispatch(
      actions.createShabondi.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useUpdateShabondiAction = () => {
  const dispatch = useDispatch();
  const group = useShabondiGroup();
  return (values, options) =>
    dispatch(
      actions.updateShabondi.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useUpdateShabondiLinkAction = () => {
  const dispatch = useDispatch();
  const group = useShabondiGroup();
  return (values, options) =>
    dispatch(
      actions.updateShabondiLink.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useRemoveSourceLinkAction = () => {
  const dispatch = useDispatch();
  const group = useShabondiGroup();
  return (values, options) =>
    dispatch(
      actions.removeShabondiSourceLink.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useRemoveSinkLinkAction = () => {
  const dispatch = useDispatch();
  const group = useShabondiGroup();
  return (values, options) =>
    dispatch(
      actions.removeShabondiSinkLink.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useStartShabondiAction = () => {
  const dispatch = useDispatch();
  const group = useShabondiGroup();
  return (values, options) =>
    dispatch(
      actions.startShabondi.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useStopShabondiAction = () => {
  const dispatch = useDispatch();
  const group = useShabondiGroup();
  return (values, options) =>
    dispatch(
      actions.stopShabondi.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useDeleteShabondiAction = () => {
  const dispatch = useDispatch();
  const group = useShabondiGroup();
  return (values, options) =>
    dispatch(
      actions.deleteShabondi.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useFetchShabondisAction = () => {
  const dispatch = useDispatch();
  const group = useShabondiGroup();
  return () =>
    dispatch(
      actions.fetchShabondis.trigger({
        group,
      }),
    );
};

export const useIsShabondiLoaded = () => {
  const mapState = useCallback(state => !!state.ui.shabondi?.lastUpdated, []);
  return useSelector(mapState);
};

export const useIsShabondiLoading = () => {
  const mapState = useCallback(state => !!state.ui.shabondi?.loading, []);
  return useSelector(mapState);
};

export const useShabondiGroup = () => {
  const pipelineGroup = hooks.usePipelineGroup();
  const pipelineName = hooks.usePipelineName();
  if (pipelineGroup && pipelineName)
    return hashByGroupAndName(pipelineGroup, pipelineName);
};

export const useShabondis = () => {
  const getShabondiByGroup = useMemo(selectors.makeGetAllShabondisByGroup, []);
  const group = useShabondiGroup();
  const fetchShabondis = useFetchShabondisAction();

  useEffect(() => {
    if (useIsShabondiLoaded || useIsShabondiLoading) return;

    fetchShabondis();
  }, [fetchShabondis]);

  return useSelector(
    useCallback(state => getShabondiByGroup(state, { group }), [
      getShabondiByGroup,
      group,
    ]),
  );
};
