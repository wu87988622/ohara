import { useState, useEffect, useRef } from 'react';
import * as generate from 'utils/generate';

const WorkerVoBuilder = () => {
  const [name, setName] = useState();

  const [imageName, setImageName] = useState();

  const [brokerClusterName, setBrokerClusterName] = useState();

  const [clientPort, setClientPort] = useState();

  const [jmxPort, setJmxPort] = useState();

  const [groupId, setGroupId] = useState();

  const [configTopicName, setConfigTopicName] = useState();

  const [configTopicReplications, setConfigTopicReplications] = useState();

  const [offsetTopicName, setOffsetTopicName] = useState();

  const [offsetTopicPartitions, setOffsetTopicPartitions] = useState();

  const [offsetTopicReplications, setOffsetTopicReplications] = useState();

  const [statusTopicName, setStatusTopicName] = useState();

  const [statusTopicPartitions, setStatusTopicPartitions] = useState();

  const [statusTopicReplications, setStatusTopicReplications] = useState();

  const [jars, setJars] = useState([]);

  const [nodeNames, setNodeNames] = useState();

  const [deadNodes, setDeadNodes] = useState();

  const setData = useRef();

  const setDefault = () => {
    setClientPort(generate.port());
    setJmxPort(generate.port());
    setGroupId(generate.serviceName());
    setConfigTopicName(generate.serviceName());
    setOffsetTopicName(generate.serviceName());
    setStatusTopicName(generate.serviceName());
  };

  const getData = () => {
    setDefault();
    const data = setData.current;
    return data;
  };

  useEffect(() => {
    setData.current = {
      name,
      imageName,
      brokerClusterName,
      clientPort,
      jmxPort,
      groupId,
      configTopicName,
      configTopicReplications,
      offsetTopicName,
      offsetTopicPartitions,
      offsetTopicReplications,
      statusTopicName,
      statusTopicPartitions,
      statusTopicReplications,
      jars,
      nodeNames,
      deadNodes,
    };
  });

  return {
    setName,
    setImageName,
    setBrokerClusterName,
    setClientPort,
    setJmxPort,
    setGroupId,
    setConfigTopicName,
    setConfigTopicReplications,
    setOffsetTopicName,
    setOffsetTopicPartitions,
    setOffsetTopicReplications,
    setStatusTopicName,
    setStatusTopicPartitions,
    setStatusTopicReplications,
    setJars,
    setNodeNames,
    setDeadNodes,
    getData,
  };
};

const WorkerVo = res => {
  const name = res.name;

  const imageName = res.imageName;

  const brokerClusterName = res.brokerClusterName;

  const clientPort = res.clientPort;

  const jmxPort = res.jmxPort;

  const groupId = res.groupId;

  const configTopicName = res.configTopicName;

  const configTopicReplications = res.configTopicReplications;

  const offsetTopicName = res.offsetTopicName;

  const offsetTopicPartitions = res.offsetTopicPartitions;

  const offsetTopicReplications = res.offsetTopicReplications;

  const statusTopicName = res.statusTopicName;

  const statusTopicPartitions = res.statusTopicPartitions;

  const statusTopicReplications = res.statusTopicReplications;

  const jarInfos = res.jarInfos;

  const nodeNames = res.nodeNames;

  const deadNodes = res.deadNodes;

  return {
    name,
    imageName,
    brokerClusterName,
    clientPort,
    jmxPort,
    groupId,
    configTopicName,
    configTopicReplications,
    offsetTopicName,
    offsetTopicPartitions,
    offsetTopicReplications,
    statusTopicName,
    statusTopicPartitions,
    statusTopicReplications,
    jarInfos,
    nodeNames,
    deadNodes,
  };
};

export { WorkerVoBuilder, WorkerVo };
