export default function ParseBinaryPacket(data: Int8Array): BinaryPacket {
  // [TYPE, ...HEADER, 00, ...PAYLOAD]
  const type: number = data[0];
  const header: number[] = [];
  let payload: number[] = [];

  let i = 1;
  for (; i < data.length; i++) {
    const byte = data[i];
    if (byte !== 0) {
      header.push(byte);
    } else {
      ++i;
      break;
    }
  }

  if (i < data.length) {
    payload = [...data.slice(i)];
  }

  return {
    type,
    header,
    payload,
  };
}
