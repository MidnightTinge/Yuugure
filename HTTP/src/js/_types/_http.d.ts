type RouterResponse<A = any, B = any, C = any, D = any, E = any, F = any, G = any> = {
  status: string;
  code: number;
  messages: string[];
  data: Record<string, (A | B | C | D | E | F | G)[]>;
};

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
