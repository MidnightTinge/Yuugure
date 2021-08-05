import RelativeTime from '../../src/js/classes/RelativeTime';

const baseTime = new Date('2021-08-05 00:00:00');

const second = 1e3;
const minute = second * 60;
const hour = minute * 60;
const day = hour * 24;
const week = day * 7;

beforeAll(() => {
  Date.now = jest.fn().mockReturnValue(baseTime.getTime());
});

describe('Future', () => {
  it('Displays "just now" for short times', () => {
    const rendered = RelativeTime(Date.now() + second);
    expect(rendered).toEqual('just now');
  });

  it('Displays seconds', () => {
    const rendered = RelativeTime(Date.now() + (second * 15));
    expect(rendered).toEqual('in 15s');
  });

  it('Displays minutes', () => {
    expect(RelativeTime(Date.now() + minute)).toEqual('in 1m');
    expect(RelativeTime(Date.now() + (minute + (second * 5)))).toEqual('in 1m5s');
  });

  it('Displays hours', () => {
    expect(RelativeTime(Date.now() + hour)).toEqual('in 1h');
    expect(RelativeTime(Date.now() + (hour + (30 * minute)))).toEqual('in 1h30m');
  });

  it('Displays days', () => {
    expect(RelativeTime(Date.now() + day)).toEqual('in 1d');
    expect(RelativeTime(Date.now() + (day + (12 * hour)))).toEqual('in 1d12h');
  });

  it('Displays a hard date for anything over a week', () => {
    expect(RelativeTime(Date.now() + week)).toEqual('Aug 12 \'21 00:00');
    expect(RelativeTime(Date.now() + week + (2 * day) + (6 * hour) + (27 * minute))).toEqual('Aug 14 \'21 06:27');
  });
});

describe('Past', () => {
  it('Displays "just now" for short times', () => {
    const rendered = RelativeTime(Date.now() - second);
    expect(rendered).toEqual('just now');
  });

  it('Displays seconds', () => {
    const rendered = RelativeTime(Date.now() - (second * 15));
    expect(rendered).toEqual('15s ago');
  });

  it('Displays minutes', () => {
    expect(RelativeTime(Date.now() - minute)).toEqual('1m ago');
    expect(RelativeTime(Date.now() - (minute + (second * 5)))).toEqual('1m5s ago');
  });

  it('Displays hours', () => {
    expect(RelativeTime(Date.now() - hour)).toEqual('1h ago');
    expect(RelativeTime(Date.now() - (hour + (30 * minute)))).toEqual('1h30m ago');
  });

  it('Displays days', () => {
    expect(RelativeTime(Date.now() - day)).toEqual('1d ago');
    expect(RelativeTime(Date.now() - (day + (12 * hour)))).toEqual('1d12h ago');
  });

  it('Displays a hard date for anything over a week', () => {
    expect(RelativeTime(Date.now() - week)).toEqual('Jul 29 \'21 00:00');
    expect(RelativeTime(Date.now() - (week + (2 * day) + (6 * hour) + (27 * minute)))).toEqual('Jul 26 \'21 17:33');
  });
});
