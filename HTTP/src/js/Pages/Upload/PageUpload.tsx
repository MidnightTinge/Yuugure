import * as React from 'react';
import {useMemo, useRef, useState} from 'react';
import {useHistory} from 'react-router-dom';
import KY from '../../classes/KY';
import RouterResponseConsumer from '../../classes/RouterResponseConsumer';
import Util from '../../classes/Util';
import Button from '../../Components/Button';

import CenteredBlockPage from '../../Components/CenteredBlockPage';
import FileInput from '../../Components/FileInput';
import Spinner from '../../Components/Spinner';
import RatingGroup from './RatingGroup';

export type PageUploadProps = {
  //
};

export default function PageUpload(props: PageUploadProps) {
  const history = useHistory();

  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string>(null);
  const [uploadResult, setUploadResult] = useState<UploadResult>(null);

  const [files, setFiles] = useState<FileList>(null);
  const [fileErrors, setFileErrors] = useState<string>(null);

  const cbPrivate = useRef<HTMLInputElement>(null);

  const [tags, setTags] = useState<string>('');
  const txtTagsId = useMemo(() => Util.mkid(), []);

  const [rating, setRating] = useState<string>('safe'); // TODO: Set based on account settings.

  const canUpload = useMemo(() => files != null && files.length > 0 && tags.trim().length > 0, [files, tags]);

  function handleClick() {
    setUploading(true);

    KY.post('/upload', {
      body: Util.formatFormData({
        private: String(cbPrivate.current.checked),
        file: files[0],
        tags,
        rating,
      }),
    }).json<RouterResponse>().then(data => {
        const consumed = RouterResponseConsumer<UploadResult>(data);
        if (consumed.success) {
          setError(null);
          setFileErrors(null);
          setUploadResult({...consumed.data[0]});
        } else {
          let [ur] = consumed.data;
          setError(consumed.message);

          if ('file' in ur.inputErrors) {
            setFileErrors(ur.inputErrors.file.join('\n'));
          }

          if (ur.errors.length > 0) {
            setError(ur.errors.join('\n'));
          }
        }
      })
      .catch(err => {
        console.error('Failed to upload:', err);
        setError(err.toString());
      })
      .then(() => {
        setUploading(false);
      });
  }

  function handleFiles(newFiles: FileList) {
    setFiles(newFiles);
  }

  function handleUploadNavigation(e: React.MouseEvent<HTMLAnchorElement, MouseEvent>) {
    e.preventDefault();
    history.push(`/view/${uploadResult.upload.upload.id}`);
  }

  function handleFormReset() {
    setUploading(false);
    setError(null);
    setUploadResult(null);
    setFileErrors(null);
    setFiles(null);
    setTags('');
    setRating('safe'); // TODO: Set based on account settings.
  }

  function handleTagsChange(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    setTags(e.currentTarget.value);
  }

  return (
    <CenteredBlockPage>
      <>
        {!uploadResult ? (
          <>
            <p className="text-lg text-center">Upload File</p>
            <form method="post" action="/upload" encType="multipart/form-data">
              <div className="mb-2">
                <FileInput label="Image/Video" onFiles={handleFiles} invalid={fileErrors && fileErrors.length > 0} errorText={fileErrors}/>
              </div>
              <div className="my-2">
                <label><input ref={cbPrivate} type="checkbox" name="private"/> Private</label>
              </div>
              <div className="my-2">
                <label htmlFor={txtTagsId} id={`lbl-${txtTagsId}`} className="mb-1 text-sm font-medium text-gray-800">Tags</label>
                <textarea id={txtTagsId} name="tags" onKeyUp={handleTagsChange} className="block w-full rounded-md border border-gray-300 hover:bg-gray-50 shadow focus:border-gray-500 focus:ring focus:ring-gray-400 focus:ring-opacity-50 disabled:cursor-not-allowed disabled:bg-gray-200" disabled={uploading} required/>
              </div>
              <div className="my-2">
                <RatingGroup rating={rating} onChange={setRating}/>
              </div>
              <div className="mt-2">
                <Button type="button" variant="green" className="block w-full" onClick={handleClick} disabled={!canUpload}>
                  {uploading ? (<><Spinner/> Uploading...</>) : `Upload`}
                </Button>
              </div>
            </form>
          </>
        ) : (
          <>
            <p className="text-lg text-center">File Uploaded</p>
            <p className="my-3">Your file has been uploaded successfully. Click <a href={`/view/${uploadResult.upload.upload.id}`} onClick={handleUploadNavigation} className="text-blue-500 hover:text-blue-600 focus:text-blue-500">here</a> to view it.</p>
            <Button type="button" variant="blue" className="block w-full" onClick={handleFormReset}>Upload Another</Button>
            {uploadResult.notices.length > 0 ? (
              <>
                <p className="mt-2 text-gray-700">Notice:</p>
                <ul className="ml-2 text-red-400 text-sm">
                  {uploadResult.notices.map((notice, idx) => (<li className="whitespace-pre-wrap" key={idx}>{notice}</li>))}
                </ul>
              </>
            ) : null}
          </>
        )}
        {error ? (
          <p className="text-red-500 whitespace-pre-wrap mt-2">{error}</p>
        ) : null}
      </>
    </CenteredBlockPage>
  );
}
