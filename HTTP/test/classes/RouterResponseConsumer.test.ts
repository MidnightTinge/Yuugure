import RouterResponseConsumer, {MESSAGES} from '../../src/js/classes/RouterResponseConsumer';

it('Joins messages into a single string', () => {
  expect(RouterResponseConsumer({
    messages: ['one', 'two', 'three'],
  } as any).message).toEqual(['one', 'two', 'three'].join('\n'));
});

it('Defaults messages to an internal map based on response code if no data present', () => {
  // for..in is used specifically to grab keys. this would usually be bad practice.
  for (let code in MESSAGES) {
    expect(RouterResponseConsumer({
      code,
    } as any).message).toEqual(MESSAGES[code]);
  }
});

it('Defaults message to a catch-all for unknown codes', () => {
  expect(RouterResponseConsumer({
    code: -1,
  } as any).message).toEqual(MESSAGES[0]);
});

it('Does not set message if data is present and message isn\'t set', () => {
  expect(RouterResponseConsumer({
    data: [{}],
  } as any).message).toEqual('');
});

it('Sets success based on response code', () => {
  expect(RouterResponseConsumer({
    code: 200,
  } as any).success).toStrictEqual(true);
  expect(RouterResponseConsumer({
    code: 300,
  } as any).success).toStrictEqual(false);
});

it('Passes code down to the response', () => {
  expect(RouterResponseConsumer({
    code: 317,
  } as any).code).toStrictEqual(317);
});

it('Defaults to a fail response when response is null', () => {
  expect(RouterResponseConsumer(null)).toEqual({
    success: false,
    message: MESSAGES[500],
    code: -1,
    data: [],
  });
});
