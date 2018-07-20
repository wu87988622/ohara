import React from 'react';

import AppWrapper from '../common/AppWrapper';
import Loading from '../common/Loading';
import { ListTable } from '../common/Table';
import { fetchTopics } from '../../apis/topicsApis';
import { TOPICS } from '../../constants/url';

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
        <ListTable headers={headers} list={topics} urlDir={TOPICS} />
      </AppWrapper>
    );
  }
}

export default TopicsPage;
