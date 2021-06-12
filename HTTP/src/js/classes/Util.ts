export default class Util {
  static mkid() {
    return new Array(4).fill(0).map(_ => ((Math.random() * 2e9) >> 0).toString(16).toUpperCase()).join('-');
  }
}
