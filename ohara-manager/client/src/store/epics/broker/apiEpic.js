import * as brokerApi from 'api/brokerApi';
import * as inspectApi from 'api/inspectApi';
import { defer } from 'rxjs';

export const addNode$ = values => defer(() => brokerApi.remove(values));
export const create$ = values => defer(() => brokerApi.create(values));
export const forceStop$ = values => defer(() => brokerApi.remove(values));
export const get$ = values => defer(() => brokerApi.get(values));
export const getAll$ = () => defer(() => brokerApi.getAll());
export const remove$ = values => defer(() => brokerApi.remove(values));
export const removeNode$ = values => defer(() => brokerApi.removeNode(values));
export const start$ = values => defer(() => brokerApi.start(values));
export const stop$ = values => defer(() => brokerApi.stop(values));
export const update$ = values => defer(() => brokerApi.update(values));

export const getBrokerInfo$ = values =>
  defer(() => inspectApi.getBrokerInfo(values));
