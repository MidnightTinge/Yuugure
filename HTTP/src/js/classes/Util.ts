export default class Util {
  static mkid() {
    return new Array(4).fill(0).map(_ => ((Math.random() * 2e9) >> 0).toString(16).toUpperCase()).join('-');
  }

  static mapBulkUploads(bulk: BulkRenderableUpload): RenderableUpload[] {
    let ret: RenderableUpload[] = [];

    for (let i = 0; i < bulk.uploads.length; i++) {
      let upload = bulk.uploads[i].upload;

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
    let ret: RenderableComment[] = [];

    for (let i = 0; i < bulk.comments.length; i++) {
      let comment = bulk.comments[i];
      ret.push({
        ...comment,
        account: bulk.accounts[comment.account],
      });
    }

    return ret;
  }

  static formatUrlEncodedBody(body: Record<string, any>) {
    const params = new URLSearchParams();
    for (let x of Object.entries(body)) {
      params.set(x[0], x[1]);
    }

    return params;
  }

  static formatFormData(body: Record<string, any>) {
    const fd = new FormData();
    for (let x of Object.entries(body)) {
      fd.append(x[0], x[1]);
    }

    return fd;
  }

}
