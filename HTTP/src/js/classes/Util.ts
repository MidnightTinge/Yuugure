export default class Util {
  static mkid(): string {
    return new Array(4).fill(0).map(() => ((Math.random() * 2e9) >> 0).toString(16).toUpperCase()).join('-');
  }

  static mapBulkUploads(bulk: BulkRenderableUpload): RenderableUpload[] {
    const ret: RenderableUpload[] = [];

    for (let i = 0; i < bulk.uploads.length; i++) {
      const upload = bulk.uploads[i].upload;

      ret.push({
        upload,
        media: bulk.medias[upload.media],
        media_meta: bulk.metas[upload.media],
        owner: bulk.accounts[upload.owner],
        state: bulk.uploads[i].state,
        tags: bulk.uploads[i].tags.map(tid => bulk.tags[tid]),
        votes: bulk.uploads[i].votes,
        bookmarks: bulk.uploads[i].bookmarks,
      });
    }

    return ret;
  }

  static mapBulkComments(bulk: BulkRenderableComment): RenderableComment[] {
    const ret: RenderableComment[] = [];

    for (let i = 0; i < bulk.comments.length; i++) {
      const comment = bulk.comments[i];
      ret.push({
        ...comment,
        account: bulk.accounts[comment.account],
      });
    }

    return ret;
  }

  static formatUrlEncodedBody(body: Record<string, any>): URLSearchParams {
    const params = new URLSearchParams();
    for (const x of Object.entries(body)) {
      params.set(x[0], x[1]);
    }

    return params;
  }

  static formatFormData(body: Record<string, any>): FormData {
    const fd = new FormData();
    for (const x of Object.entries(body)) {
      fd.append(x[0], x[1]);
    }

    return fd;
  }

  static formatBytes(bytes: number, decimals = 2): string {
    // https://stackoverflow.com/a/18650828
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  }
}
