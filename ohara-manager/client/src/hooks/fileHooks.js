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

import { useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { KIND } from 'const';
import { hashByGroupAndName } from 'utils/sha';

export const useIsFileLoaded = () => {
  const mapState = useCallback(state => !!state.ui.file?.lastUpdated, []);
  return useSelector(mapState);
};

export const useIsFileLoading = () => {
  const mapState = useCallback(state => !!state.ui.file?.loading, []);
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
  const group = hooks.useFileGroup();
  return useCallback(
    file => {
      const { name } = file;
      dispatch(actions.createFile.trigger({ name, group, file }));
    },
    [dispatch, group],
  );
};

export const useFetchFilesAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useFileGroup();
  return useCallback(
    workspaceName =>
      dispatch(actions.fetchFiles.trigger({ group, workspaceName })),
    [dispatch, group],
  );
};

export const useDeleteFileAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useFileGroup();
  return useCallback(
    name => dispatch(actions.deleteFile.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useFiles = () => {
  const makeFindFilesByGroup = useMemo(selectors.makeFindFilesByGroup, []);
  const group = hooks.useFileGroup();
  const findFilesByGroup = useCallback(
    state => makeFindFilesByGroup(state, { group }),
    [makeFindFilesByGroup, group],
  );
  return useSelector(findFilesByGroup);
};

export const useStreamFiles = () => {
  const files = useFiles();
  const streams = useMemo(
    () =>
      files.filter(file => {
        return file.classInfos.find(classInfo => {
          const { settingDefinitions: defs } = classInfo;
          const { defaultValue: kind } = defs.find(def => def.key === 'kind');
          return kind === KIND.stream;
        });
      }),
    [files],
  );

  return streams;
};
