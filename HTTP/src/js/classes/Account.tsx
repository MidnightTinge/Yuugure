export default class Account {
  private _account: SafeAccount = null;

  constructor(account: SafeAccount) {
    this._account = account;
  }

  get id() {
    return this._account.id;
  }

  get username() {
    return this._account.username;
  }

  get state() {
    return {...this._account.state};
  }

  get roles() {
    return {...this._account.roles};
  }

  get canUpload() {
    return !(this._account.state.BANNED || this._account.state.UPLOAD_RESTRICTED);
  }

  get canComment() {
    return !(this._account.state.BANNED || this._account.state.COMMENTS_RESTRICTED);
  }

  get hasModPerms() {
    return this._account.roles.MOD || this._account.roles.ADMIN;
  }

  get hasAdminPerms() {
    return this._account.roles.ADMIN;
  }
}
