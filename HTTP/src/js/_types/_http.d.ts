type RouterResponse<A = any, B = any, C = any, D = any, E = any, F = any, G = any> = {
  status: string;
  code: number;
  messages: string[];
  data: Record<string, (A | B | C | D | E | F | G)[]>;
};

type InputAwareResponse<T> = T & {
  inputErrors: Record<string, string[]>;
  errors: string[];
}

type AuthResponse = InputAwareResponse<{
  authed: boolean;
  authentication_token: string;
}>

type AuthStateResponse = {
  authenticated: boolean;
  account: SafeAccount;
}

type AuthConfirmResponse = InputAwareResponse<{
  authenticated: boolean;
  confirmation_token: string;
}>

type UploadResult = InputAwareResponse<{
  success: boolean;
  upload?: RenderableUpload;
}>

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

type SafeComment = {
  id: number;
  parent: number;
  account: number;
  timestamp: number;

  content_raw: string;
  content_rendered: string;
}

type RenderableComment = {
  id: number;
  timestamp: number;
  account: SafeAccount;
  content_raw: string;
  content_rendered: string;
}

type CommentResponse = InputAwareResponse<{
  comment: RenderableComment
}>
