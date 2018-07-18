import React from 'react';
import styled from 'styled-components';
import { Link } from 'react-router-dom';

import AppWrapper from '../common/AppWrapper';
import Loading from '../common/Loading';
import { fetchTopics } from '../../apis/topicApi';
import { TOPICS } from '../../constants/url';

const TableRow = styled.tr`
  background-color: #81bef7;
`;

class TopicsPage extends React.Component {
  state = {
    headers: ['Topic Name', 'Details'],
    topics: [],
    isLoading: true,
  };

  async componentDidMount() {
    const res = await fetchTopics();

    if (res) {
      const { uuids } = res.data;
      const topics = Object.keys(uuids).map(key => {
        return {
          [key]: uuids[key],
        };
      });

      this.setState({ topics, isLoading: false });
    }
  }
  render() {
    const { isLoading, headers, topics } = this.state;

    if (isLoading) {
      return <Loading />;
    }
    return (
      <AppWrapper title="Topics">
        <table className="table table-striped">
          <thead>
            <TableRow className="text-white">
              {headers.map(header => <th key={header}>{header}</th>)}
            </TableRow>
          </thead>
          <tbody>
            {topics.map(topic => {
              const uuid = Object.keys(topic)[0];
              const topicName = Object.values(topic)[0];

              return (
                <tr key={uuid}>
                  <td>{topicName}</td>
                  <td>
                    <Link to={`${TOPICS}/${topicName}`}>Details</Link>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </AppWrapper>
    );
  }
}

export default TopicsPage;
