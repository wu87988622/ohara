import React from 'react';
import toastr from 'toastr';
import * as streamApi from 'api/streamApi';
import { get, endsWith } from 'lodash';
import { PageHeader, PageBody, StyledLabel } from './styles';
import { SortTable } from 'components/common/Mui/Table';
import { Button } from 'components/common/Mui/Form';
import * as MESSAGES from 'constants/messages';

const StreamApp = props => {
  const { workspaceName } = props;

  const fetchJars = async () => {
    const res = await streamApi.fetchJars(workspaceName);
    return get(res, 'data.result', null);
  };
  const uploadJar = async file => {
    const res = await streamApi.uploadJar({
      workerClusterName: workspaceName,
      file,
    });
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_UPLOAD_SUCCESS);
    }
  };

  const validateJarExtension = jarName => endsWith(jarName, '.jar');

  const handleFileSelect = e => {
    const file = e.target.files[0];
    if (e.target.files[0]) {
      const filename = file.name;
      if (!validateJarExtension(filename)) {
        toastr.error(
          `This file type is not supported.\n Please select your '.jar' file.`,
        );
        return;
      }

      // if (this.isDuplicateTitle(filename)) {
      //   toastr.error(`This file name is duplicate. '${filename}'`);
      //   return;
      // }

      uploadJar(file);
    }
  };

  const headRows = [
    { id: 'name', label: 'Jar name' },
    { id: 'fileSize', label: 'File size (KB)' },
    { id: 'lastModified', label: 'Last modified' },
    { id: 'action', label: 'Action', sortable: false },
  ];

  const [order, setOrder] = React.useState('asc');
  const [orderBy, setOrderBy] = React.useState('name');

  const createData = (name, fileSize, lastModified, action) => {
    return { name, fileSize, lastModified, action };
  };

  const rows = fetchJars.map(jar => {
    return createData(jar.name);
  });
  const handleRequestSort = (event, property) => {
    const isDesc = orderBy === property && order === 'desc';
    setOrder(isDesc ? 'asc' : 'desc');
    setOrderBy(property);
  };

  return (
    <>
      <PageHeader>
        <input
          accept=".jar"
          style={{ display: 'none' }}
          id="contained-button-file"
          multiple
          type="file"
          onChange={handleFileSelect}
        />
        <StyledLabel htmlFor="contained-button-file">
          <Button component="span" text="new jar" />
        </StyledLabel>
      </PageHeader>
      <PageBody>
        <SortTable
          isLoading={false}
          headRows={headRows}
          rows={rows}
          onRequestSort={handleRequestSort}
          order={order}
          orderBy={orderBy}
        />
      </PageBody>
    </>
  );
};

export default StreamApp;
