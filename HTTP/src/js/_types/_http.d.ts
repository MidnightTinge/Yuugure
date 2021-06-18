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

type UploadState = {
  PRIVATE: boolean;
  DELETED: boolean;
  DMCA: boolean;
  LOCKED_TAGS: boolean;
  LOCKED_COMMENTS: boolean;
  MODERATION_QUEUED: boolean;
}

type RenderableUpload = {
  upload: DBUpload;
  media: DBMedia;
  media_meta: DBMediaMeta;
  owner: {
    id: number;
    username: string;
  };
  state: UploadState;
};

type ReportResponse = {
  report_id: number;
  reason: string;
}

type AccountState = {
  DEACTIVATED: boolean;
  DELETED: boolean;
  BANNED: boolean;
  UPLOAD_RESTRICTED: boolean;
  COMMENTS_RESTRICTED: boolean;
  TRUSTED_UPLOADS: boolean;
  PRIVATE: boolean;
}

type SafeAccount = {
  id: number;
  username: string;
  state: AccountState;
}

type ProfileResponse = {
  self: boolean;
  account: SafeAccount;
}
