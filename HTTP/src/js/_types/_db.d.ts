type DBMedia = {
  id: number;
  sha256: string;
  md5: string;
  phash: string;
  mime: string;
}

type DBMediaMeta = {
  id: number;
  media: number;
  width: number;
  height: number;
  video: boolean;
  video_duration: number;
  has_audio: boolean;
  filesize: number;
}

type DBUpload = {
  id: number;
  media: number;
  parent: number;
  owner: number;
  upload_date: number;
  state: bigint;
}
