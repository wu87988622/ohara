import { of, throwError, iif, from } from 'rxjs';
import { delay, map, retryWhen, concatMap } from 'rxjs/operators';

import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';

const retry = params =>
  of(
    from(params).pipe(
      map(res => {
        if (!res.data?.state || res.data.state !== SERVICE_STATE.RUNNING)
          throw res;
        else return res.data;
      }),
      retryWhen(errors =>
        errors.pipe(
          concatMap((value, index) =>
            iif(
              () => index > 10,
              throwError({
                data: value?.data,
                meta: value?.meta,
                title:
                  `Try to start broker: "${params.name}" failed after retry ${index} times. ` +
                  `Expected state: ${SERVICE_STATE.RUNNING}, Actual state: ${value.data.state}`,
              }),
              of(value).pipe(delay(2000)),
            ),
          ),
        ),
      ),
    ),
  );

export default retry;
