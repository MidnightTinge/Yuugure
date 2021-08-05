import Util from '../../src/js/classes/Util';

describe('mkid', () => {
  it('Returns a string of 4 segments', () => {
    const id = Util.mkid();
    expect(id).not.toBeNull();
    expect(typeof id).toStrictEqual('string');
    expect(id.split('-')).toHaveLength(4);
    expect(id).toMatch(/^([a-f0-9]+-){3}[a-f0-9]+$/i); // 4 hex parts separated by a dash.
  });
});

describe('mapBulkUploads', () => {
  const UPLOAD: BulkRenderableUpload = Object.freeze({
    accounts: {
      1: {
        id: 1,
        state: {
          BANNED: false,
          COMMENTS_RESTRICTED: false,
          DEACTIVATED: false,
          DELETED: false,
          PRIVATE: false,
          TRUSTED_UPLOADS: false,
          UPLOAD_RESTRICTED: false,
        },
        roles: {
          ADMIN: false,
          MOD: false,
        },
        username: 'MidnightTinge',
      },
    },
    tags: {
      1: {
        id: 1,
        category: 'userland',
        name: 'glasses',
        parent: null,
      },
    },
    medias: {
      27: {
        id: 27,
        md5: 'md5',
        mime: 'mime',
        phash: 'phash',
        sha256: 'sha256',
      },
    },
    metas: {
      27: {
        id: 27,
        media: 27,
        filesize: 1193498,
        has_audio: false,
        height: 1920,
        width: 1080,
        video: false,
        video_duration: 0,
      },
    },
    uploads: [
      {
        upload: {
          id: 27,
          media: 27,
          state: 0n,
          owner: 1,
          upload_date: new Date('2021-08-05 14:03:27').getUTCDate(),
          parent: null,
        },
        state: {
          DELETED: false,
          DMCA: false,
          LOCKED_COMMENTS: false,
          LOCKED_TAGS: false,
          PRIVATE: false,
          MODERATION_QUEUED: false,
        },
        tags: [
          1,
        ],
        votes: {
          is_upvote: false,
          voted: false,
          total_downvotes: 0,
          total_upvotes: 1,
        },
        bookmarks: {
          bookmarked: false,
          bookmarked_publicly: false,
          total_public: 1,
        },
      },
    ],
  });

  it('maps a basic bulk response', () => {
    expect(Util.mapBulkUploads(UPLOAD)).toEqual([
      {
        upload: {...UPLOAD.uploads[0].upload},
        media: {...UPLOAD.medias[27]},
        media_meta: {...UPLOAD.metas[27]},
        owner: {...UPLOAD.accounts[1]},
        state: {...UPLOAD.uploads[0].state},
        tags: [...[{...UPLOAD.tags[1]}]],
        votes: {...UPLOAD.uploads[0].votes},
        bookmarks: {...UPLOAD.uploads[0].bookmarks},
      } as RenderableUpload,
    ]);
  });

});

describe('mapBulkComments', () => {
  const COMMENTS: BulkRenderableComment = Object.freeze({
    accounts: {
      1: {
        id: 1,
        state: {
          BANNED: false,
          COMMENTS_RESTRICTED: false,
          DEACTIVATED: false,
          DELETED: false,
          PRIVATE: false,
          TRUSTED_UPLOADS: false,
          UPLOAD_RESTRICTED: false,
        },
        roles: {
          ADMIN: false,
          MOD: false,
        },
        username: 'MidnightTinge',
      },
    },
    comments: [
      {
        id: 1,
        parent: null,
        account: 1,
        timestamp: new Date('2021-08-05 14:11:28').getTime(),
        content_raw: 'content',
        content_rendered: '<p>content</p>',
      },
    ],
  });

  it('Maps a basic renderable comment', () => {
    expect(Util.mapBulkComments(COMMENTS)).toEqual([
      {
        id: 1,
        parent: null,
        account: {...COMMENTS.accounts[1]},
        content_raw: 'content',
        content_rendered: '<p>content</p>',
        timestamp: new Date('2021-08-05 14:11:28').getTime(),
      } as RenderableComment,
    ]);
  });
});

describe('formatFormData', () => {
  it('formats', () => {
    const input = {
      one: 1,
      two: 2,
      three: 3,
    };
    const formatted = Util.formatFormData(input);

    expect(formatted.get('one')).toEqual(String(input.one));
    expect(formatted.get('two')).toEqual(String(input.two));
    expect(formatted.get('three')).toEqual(String(input.three));
  });
});

describe('formatBytes', () => {
  const kb = 1024;
  const mb = kb * kb;
  const gb = kb * kb * kb;

  it('formats', () => {
    expect(Util.formatBytes(112)).toEqual('112 Bytes');
    expect(Util.formatBytes(kb)).toEqual('1 KB');
    expect(Util.formatBytes(mb)).toEqual('1 MB');
    expect(Util.formatBytes(gb)).toEqual('1 GB');
    expect(Util.formatBytes(3 * gb)).toEqual('3 GB');
    expect(Util.formatBytes((3 * gb) + (300 * mb))).toEqual('3.29 GB'); // rounding+log issue. not worth to fix. assert the state exists regardless.
    expect(Util.formatBytes((3 * gb) + (40 * mb))).toEqual('3.04 GB');
  });

  it('returns correctly for 0 bytes', () => {
    expect(Util.formatBytes(0)).toEqual('0 Bytes');
  });

  it('respects decimals param', () => {
    expect(Util.formatBytes((3 * gb) + (310 * mb), 1)).toEqual('3.3 GB');
  })

  it('resets decimals to 0 if underflow', () => {
    expect(Util.formatBytes((3 * gb) + (40 * mb), -3)).toEqual('3 GB');
  })
});
