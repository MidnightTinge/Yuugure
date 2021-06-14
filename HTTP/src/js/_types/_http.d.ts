type AuthResponse = {
  authed: boolean;
  inputErrors: Record<string, string[]>;
  errors: string[];
}

type AuthStateResponse = {
  authenticated: boolean;
  account_id: number;
}

type UploadResult = {
  success: boolean;
  inputErrors: Record<string, string[]>;
  errors: string[];
  media?: DBMedia;
  upload?: DBUpload;
}

type RenderableUpload = {
  upload: DBUpload;
  media: DBMedia;
  media_meta: DBMediaMeta;
  owner: {
    id: number;
    username: string;
  };
};
