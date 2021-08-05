import ParseBinaryPacket from '../../src/js/classes/ParseBinaryPacket';

it('Parses a basic packet', () => {
  const res = ParseBinaryPacket(new Int8Array([1, 1, 0, 0x61, 0x62, 0x63]));

  expect(res.type).toEqual(1);
  expect(res.header).toEqual([1]);
  expect(res.payload).toEqual([0x61, 0x62, 0x63]);
});

it('Parses multi-byte headers', () => {
  const res = ParseBinaryPacket(new Int8Array([1, 3, 1, 4, 7, 8, 7, 1, 0, 0x61, 0x62, 0x63]));

  expect(res.type).toEqual(1);
  expect(res.header).toEqual([3, 1, 4, 7, 8, 7, 1]);
  expect(res.payload).toEqual([0x61, 0x62, 0x63]);
});

it('Handles empty payload without crashing', () => {
  const res = ParseBinaryPacket(new Int8Array([1, 3, 0]));

  expect(res.type).toEqual(1);
  expect(res.header).toEqual([3]);
  expect(res.payload).toEqual([]);
});
