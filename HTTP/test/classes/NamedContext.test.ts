import namedContext from '../../src/js/classes/NamedContext';

it('Creates a context with the specified displayName', () => {
  const context = namedContext('--test--');
  expect(context.displayName).toEqual('--test--');
});

it('Creates a context with the specified props', () => {
  const context = namedContext<{ value: string }>('--test--', {value: ' world'});
});
