import React from 'react';
import toastr from 'toastr';
import { PageHeader, PageBody, StyledLabel } from './styles';
import { SortTable } from 'components/common/Mui/Table';
import { Button } from 'components/common/Mui/Form';

const StreamApp = () => {
  const { file, setFile } = React.useState(null);

  const handleFileSelect = e => {
    setFile({ file: e.target.files[0] }, () => {
      const { file } = this.state;
      if (file) {
        const filename = file.name;
        if (!this.validateJarExtension(filename)) {
          toastr.error(
            `This file type is not supported.\n Please select your '.jar' file.`,
          );
          return;
        }

        if (this.isDuplicateTitle(filename)) {
          toastr.error(`This file name is duplicate. '${filename}'`);
          return;
        }

        this.uploadJar(file);
      }
    });
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

  const rows = [
    createData('test', 'test', 'test', 'test'),
    createData('test2', 'test2', 'test2', 'test2'),
  ];

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
