type AuthResponse = {
  authed: boolean;
  inputErrors: Record<string, string[]>;
  errors: string[];
}

type AuthStateResponse = {
  authenticated: boolean;
  account_id: number;
}
