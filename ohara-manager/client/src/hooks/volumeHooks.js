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
import { useDispatch, useSelector } from 'react-redux';

import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import * as hooks from 'hooks';

import { GROUP } from 'const';

export const useVolumeGroup = () => {
  return GROUP.VOLUME;
};

export const useCreateVolumeAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useVolumeGroup();
  return useCallback(
    (values) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.createVolume.trigger({
            values: { ...values, group },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useUpdateVolumeAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useVolumeGroup();
  return (values) =>
    new Promise((resolve, reject) =>
      dispatch(
        actions.updateVolume.trigger({
          values: { ...values, group },
          resolve,
          reject,
        }),
      ),
    );
};

export const useStartVolumeAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useVolumeGroup();
  return useCallback(
    (values) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.startVolume.trigger({
            values: { ...values, group },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useStopVolumeAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useVolumeGroup();
  return useCallback(
    (values) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.stopVolume.trigger({
            values: { ...values, group },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useDeleteVolumeAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useVolumeGroup();
  return useCallback(
    (values) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.deleteVolume.trigger({
            values: { ...values, group },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useFetchVolumesAction = () => {
  const dispatch = useDispatch();
  return () => dispatch(actions.fetchVolumes.trigger());
};

export const useClearVolumesAction = () => {
  const dispatch = useDispatch();
  return () => dispatch(actions.clearValidate.request());
};

export const useValidateVolumePathAction = () => {
  const dispatch = useDispatch();
  return (values) =>
    new Promise((resolve, reject) =>
      dispatch(
        actions.validateVolumePath.trigger({
          values: { ...values },
          resolve,
          reject,
        }),
      ),
    );
};

export const useIsVolumeLoaded = () => {
  const mapState = useCallback((state) => !!state.ui.volume?.lastUpdated, []);
  return useSelector(mapState);
};

export const useIsVolumeLoading = () => {
  const mapState = useCallback((state) => !!state.ui.volume?.loading, []);
  return useSelector(mapState);
};

export const useValidateVolumePath = () => {
  const mapState = useCallback((state) => state.ui.volume?.validate, []);
  return useSelector(mapState);
};

export const useVolumes = () => {
  const isAppReady = hooks.useIsAppReady();
  const workspaceName = hooks.useWorkspaceName();
  const group = useVolumeGroup();
  const fetchVolumes = useFetchVolumesAction();
  const isVolumeLoaded = useIsVolumeLoaded();
  const isVolumeLoading = useIsVolumeLoading();

  useEffect(() => {
    if (isVolumeLoaded || isVolumeLoading || !isAppReady) return;
    fetchVolumes();
  }, [fetchVolumes, isAppReady, isVolumeLoaded, isVolumeLoading]);

  return useSelector((state) => {
    return selectors.getVolumeByWorkspaceName(state, { group, workspaceName });
  });
};

export const useVolumesByUsedZookeeper = () => {
  const isAppReady = hooks.useIsAppReady();
  const group = useVolumeGroup();
  const fetchVolumes = useFetchVolumesAction();
  const isVolumeLoaded = useIsVolumeLoaded();
  const isVolumeLoading = useIsVolumeLoading();

  useEffect(() => {
    if (isVolumeLoaded || isVolumeLoading || !isAppReady) return;
    fetchVolumes();
  }, [fetchVolumes, isAppReady, isVolumeLoaded, isVolumeLoading]);

  return useSelector((state) => {
    return selectors.getVolumesByUsedZookeeper(state, { group });
  });
};

export const useVolumesByUsedBroker = () => {
  const isAppReady = hooks.useIsAppReady();
  const group = useVolumeGroup();
  const fetchVolumes = useFetchVolumesAction();
  const isVolumeLoaded = useIsVolumeLoaded();
  const isVolumeLoading = useIsVolumeLoading();

  useEffect(() => {
    if (isVolumeLoaded || isVolumeLoading || !isAppReady) return;
    fetchVolumes();
  }, [fetchVolumes, isAppReady, isVolumeLoaded, isVolumeLoading]);

  return useSelector((state) => {
    return selectors.getVolumesByUsedBroker(state, { group });
  });
};

export const useAllVolumes = () => {
  const isAppReady = hooks.useIsAppReady();
  const fetchVolumes = useFetchVolumesAction();
  const isVolumeLoaded = useIsVolumeLoaded();
  const isVolumeLoading = useIsVolumeLoading();

  useEffect(() => {
    if (isVolumeLoaded || isVolumeLoading || !isAppReady) return;
    fetchVolumes();
  }, [fetchVolumes, isAppReady, isVolumeLoaded, isVolumeLoading]);

  return useSelector((state) => {
    return selectors.getAllVolume(state);
  });
};
