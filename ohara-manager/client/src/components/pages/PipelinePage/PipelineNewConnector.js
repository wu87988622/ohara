import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import * as PIPELINES from 'constants/pipelines';
import { DataTable } from 'common/Table';
import { lighterBlue, lightBlue, trBgColor } from 'theme/variables';
import { update } from 'utils/pipelineToolbarUtils';

const TableWrapper = styled.div`
  margin: 30px 30px 40px;
`;

const Table = styled(DataTable)`
  thead th {
    color: ${lightBlue};
    font-weight: normal;
  }

  td {
    color: ${lighterBlue};
  }

  tbody tr {
    cursor: pointer;
  }

  .is-active {
    background-color: ${trBgColor};
  }
`;

class PipelineNewConnector extends React.Component {
  static propTypes = {
    connectors: PropTypes.array.isRequired,
    activeConnector: PropTypes.object.isRequired,
    onSelect: PropTypes.func.isRequired,
    graph: PropTypes.arrayOf(
      PropTypes.shape({
        type: PropTypes.string,
        uuid: PropTypes.string,
        isActive: PropTypes.bool,
        isExact: PropTypes.bool,
        icon: PropTypes.string,
      }),
    ).isRequired,
    updateGraph: PropTypes.func.isRequired,
  };

  update = () => {
    const { updateGraph, graph, activeConnector: connector } = this.props;
    update({ graph, updateGraph, connector });
  };

  render() {
    const { connectors, activeConnector, onSelect } = this.props;

    return (
      <TableWrapper>
        <Table headers={PIPELINES.TABLE_HEADERS}>
          {connectors.map(({ className: name, version, revision }) => {
            const isActive =
              name === activeConnector.className ? 'is-active' : '';
            return (
              <tr
                className={isActive}
                key={name}
                onClick={() => onSelect(name)}
              >
                <td>{name}</td>
                <td>{version}</td>
                <td>{revision}</td>
              </tr>
            );
          })}
        </Table>
      </TableWrapper>
    );
  }
}

export default PipelineNewConnector;
