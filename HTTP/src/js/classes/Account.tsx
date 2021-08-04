export default class Account {
  private _account: SafeAccount = null;

  constructor(account: SafeAccount) {
    this._account = account;
  }

  get id(): number {
    return this._account.id;
  }

  get username(): string {
    return this._account.username;
  }

  get state(): AccountState {
    return {...this._account.state};
  }

  get roles(): AccountRoles {
    return {...this._account.roles};
  }

  get canUpload(): boolean {
    return !(this._account.state.BANNED || this._account.state.UPLOAD_RESTRICTED);
  }

  get canComment(): boolean {
    return !(this._account.state.BANNED || this._account.state.COMMENTS_RESTRICTED);
  }

  get hasModPerms(): boolean {
    return this._account.roles.MOD || this._account.roles.ADMIN;
  }

  get hasAdminPerms(): boolean {
    return this._account.roles.ADMIN;
  }
}
