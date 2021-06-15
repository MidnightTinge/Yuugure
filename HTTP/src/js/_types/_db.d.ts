type DBMedia = {
  id: number;
  sha256: string;
  md5: string;
  phash: string;
  mime: string;
}

type DBMediaMeta = {
  id: number;
  media: Extendable<DBMedia>;
  width: number;
  height: number;
  video: boolean;
  video_duration: number;
  has_audio: boolean;
  audio_duration: number;
}

type DBUpload = {
  id: number;
  media: Extendable<DBMedia>;
  parent: Extendable<DBUpload>;
  owner: number;
  upload_date: number;
  state: bigint;
}
