export type ConsumedRouterResponse<T> = {
  success: boolean;
  message: string;
  code: number;
  data: T[];
}

export const MESSAGES: Readonly<Record<number, string>> = Object.freeze({
  // OK
  200: '',
  // Bad Request
  400: 'The request was malformed. Please reload and try again later.',
  // Unauthorized
  401: 'You must log in to perform this action.',
  // Forbidden
  403: 'You do not have permission to perform this action.',
  // Not Found
  404: 'The resource you requested could not be found.',
  // Method Not Allowed
  405: 'The server returned MethodNotAllowed. Please reload and try again later.',
  // Too Many Requests
  429: 'You are doing that too often. Please try again later.',
  // Internal Server Error
  500: 'The server returned InternalServerError. Please try again later.',
  // Not Implemented
  501: 'The server returned NotYetImplemented. If you believe this is an error, please contact the webmaster.',

  // Catch-all default
  0: 'The server returned an invalid response. Please try again later. If the problem persists, please contact the webmaster.',
});

export default function RouterResponseConsumer<T = any>(response: RouterResponse<T>): ConsumedRouterResponse<T> {
  if (response) {
    const data: T[] = (Array.isArray(response.data)) ? response.data : [];

    let message;
    if (Array.isArray(response.messages) && response.messages.length > 0) {
      message = response.messages.join('\n');
    } else if (!data.length) {
      // only set a default message if we don't have any returned data
      message = MESSAGES[response.code] ?? MESSAGES[0];
    } else {
      // it is assumed the RouterResponse being consumed will have messages/errors on its datum.
      message = '';
    }

    return {
      success: response.code === 200,
      message: message,
      code: response.code,
      data: data,
    };
  } else {
    return {
      success: false,
      message: MESSAGES[500],
      code: -1,
      data: [],
    };
  }
}
