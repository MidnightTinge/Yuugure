export default class Util {
  static mkid() {
    return new Array(4).fill(0).map(_ => ((Math.random() * 2e9) >> 0).toString(16).toUpperCase()).join('-');
  }

  static joinedClassName(main: string, additive?: string) {
    return `${main}${additive ? ` ${additive}` : ''}`;
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
      });
    }

    return ret;
  }
}
