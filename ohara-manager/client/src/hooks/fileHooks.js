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

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { hashByGroupAndName } from 'utils/sha';

export const useIsFileLoaded = () => {
  return useSelector(useCallback((state) => !!state.ui.file?.lastUpdated, []));
};

export const useIsFileLoading = () => {
  const mapState = useCallback((state) => !!state.ui.file?.loading, []);
  return useSelector(mapState);
};

export const useFileGroup = () => {
  const workspaceGroup = hooks.useWorkspaceGroup();
  const workspaceName = hooks.useWorkspaceName();
  if (workspaceGroup && workspaceName) {
    return hashByGroupAndName(workspaceGroup, workspaceName);
  }
};

export const useCreateFileAction = () => {
  const dispatch = useDispatch();
  const group = useFileGroup();
  return useCallback(
    (file) => {
      const { name } = file;
      dispatch(actions.createFile.trigger({ name, group, file }));
    },
    [dispatch, group],
  );
};

export const useFetchFilesAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.fetchFiles.trigger()), [dispatch]);
};

export const useDeleteFileAction = () => {
  const dispatch = useDispatch();
  const group = useFileGroup();
  return useCallback(
    (name) => dispatch(actions.deleteFile.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useFiles = () => {
  const group = useFileGroup();
  const isLoaded = useIsFileLoaded();
  const fetchFiles = useFetchFilesAction();

  useEffect(() => {
    if (!isLoaded) fetchFiles();
  }, [fetchFiles, isLoaded]);

  return useSelector((state) => selectors.getFilesByGroup(state, { group }));
};
