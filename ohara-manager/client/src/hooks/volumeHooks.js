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

import { RESOURCE } from 'api/utils/apiUtils';

export const useVolumeGroup = () => {
  return RESOURCE.VOLUME;
};

export const useCreateVolumeAction = () => {
  const dispatch = useDispatch();
  const group = useVolumeGroup();
  return (values) =>
    dispatch(
      actions.createVolume.trigger({
        ...values,
        group,
      }),
    );
};

export const useUpdateVolumeAction = () => {
  const dispatch = useDispatch();
  const group = useVolumeGroup();
  return (values) =>
    dispatch(
      actions.updateVolume.trigger({
        ...values,
        group,
      }),
    );
};

export const useStartVolumeAction = () => {
  const dispatch = useDispatch();
  const group = useVolumeGroup();
  return (values) =>
    dispatch(
      actions.startVolume.trigger({
        ...values,
        group,
      }),
    );
};

export const useStopVolumeAction = () => {
  const dispatch = useDispatch();
  const group = useVolumeGroup();
  return (values) =>
    dispatch(
      actions.stopVolume.trigger({
        ...values,
        group,
      }),
    );
};

export const useDeleteVolumeAction = () => {
  const dispatch = useDispatch();
  const group = useVolumeGroup();
  return (values) =>
    dispatch(
      actions.deleteVolume.trigger({
        ...values,
        group,
      }),
    );
};

export const useFetchVolumesAction = () => {
  const dispatch = useDispatch();
  return () => dispatch(actions.fetchVolumes.trigger());
};

export const useIsVolumeLoaded = () => {
  const mapState = useCallback((state) => !!state.ui.volume?.lastUpdated, []);
  return useSelector(mapState);
};

export const useIsVolumeLoading = () => {
  const mapState = useCallback((state) => !!state.ui.volume?.loading, []);
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
