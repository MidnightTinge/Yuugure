type BinaryPacket = {
  type: number;
  header: number[];
  payload: number[];
}

type BookmarksUpdatedPacket = {
  change: 'remove' | 'add';
}

type VotesUpdatedPacket = {
  action: 'add' | 'remove' | 'swap';
  upvote: boolean;
}

type UploadStateUpdatePacket = {
  state: UploadState;
}
