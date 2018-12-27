import { PIPELINE, SERVICES, NODES, MONITORING } from './urls';

const NAVS = [
  {
    testId: 'pipelines-link',
    to: PIPELINE,
    text: 'Pipelines',
    iconCls: 'fa-code-branch',
  },
  {
    testId: 'nodes-link',
    to: NODES,
    text: 'Nodes',
    iconCls: 'fa-sitemap',
  },
  {
    testId: 'services-link',
    to: SERVICES,
    text: 'Services',
    iconCls: 'fa-project-diagram',
  },
  {
    testId: 'monitoring-link',
    to: MONITORING,
    text: 'Monitoring',
    iconCls: 'fa-desktop',
  },
];

export default NAVS;
