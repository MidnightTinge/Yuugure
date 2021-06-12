import {faCheckCircle} from '@fortawesome/pro-duotone-svg-icons';
import {faSpinner} from '@fortawesome/pro-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import * as React from 'react';
import {useRef, useState} from 'react';
import {useHistory} from 'react-router-dom';
import {XHR} from '../../classes/XHR';
import CenteredBlockPage from '../../Components/CenteredBlockPage';
import FileInput from '../../Components/FileInput';

export type PageUploadProps = {
  //
};

export default function PageUpload(props: PageUploadProps) {
  const history = useHistory();

  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string>(null);
  const [uploadResult, setUploadResult] = useState<UploadResult>(null);

  const [fileErrors, setFileErrors] = useState<string>(null);

  const [canUpload, setCanUpload] = useState(false);
  const [files, setFiles] = useState<FileList>(null);

  const cbPrivate = useRef<HTMLInputElement>(null);

  function handleClick() {
    const fd = new FormData();
    fd.append('private', String(cbPrivate.current.checked));
    fd.append('file', files[0]);
    XHR.for('/upload').post(XHR.BODY_TYPE.FORM_DATA, fd).getJson<RouterResponse<UploadResult>>()
      .then(res => {
        if (res && res.data) {
          if (res.data.UploadResult && res.data.UploadResult[0]) {
            let ur = res.data.UploadResult[0];
            if (res.code === 200 && ur.success === true) {
              setError(null);
              setFileErrors(null);
              setUploadResult({...ur});
            } else {
              if (ur.inputErrors.file && ur.inputErrors.file.length > 0) {
                setFileErrors(ur.inputErrors.file.join('\n'));
              }

              if (ur.errors.length > 0) {
                setError(ur.errors.join('\n'));
              }
            }
          }
        } else {
          console.error('Invalid upload response:', res);
          setError('An internal server error occurred. Please try again later.');
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
    setCanUpload(newFiles.length > 0 && newFiles[0] != null);
  }

  function handleUploadNavigation(e: React.MouseEvent<HTMLAnchorElement, MouseEvent>) {
    e.preventDefault();
    history.push(`/view/${uploadResult.upload.id}`);
  }

  function handleFormReset() {
    setUploading(false);
    setError(null);
    setUploadResult(null);
    setFileErrors(null);
    setCanUpload(false);
    setFiles(null);
  }

  return (
    <CenteredBlockPage>
      <>
        {!uploadResult ? (
          <>
            <p className="text-lg text-center">Upload File</p>
            <form method="post" action="/upload" encType="multipart/form-data">
              <FileInput label="Image/Video" onFiles={handleFiles} invalid={fileErrors && fileErrors.length > 0} errorText={fileErrors}/>
              <label><input ref={cbPrivate} type="checkbox" name="private"/> Private</label>
              <button type="button" className="block w-full bg-green-500 py-1 mt-1 shadow-sm rounded text-white hover:bg-green-600 focus:bg-green-600 disabled:cursor-not-allowed disabled:bg-green-700 disabled:text-gray-400" onClick={handleClick} disabled={!canUpload}>Upload</button>
            </form>
          </>
        ) : (
          <>
            <p className="text-lg text-center">File Uploaded</p>
            <p className="my-3">Your file has been uploaded successfully. Click <a href={`/view/${uploadResult.upload.id}`} onClick={handleUploadNavigation} className="text-blue-500 hover:text-blue-600 focus:text-blue-500">here</a> to view it.</p>
            <button type="button" className="block w-full py-1 border bg-blue-400 border-blue-500 text-white rounded-md cursor-pointer shadow-sm hover:bg-blue-500 hover:border-blue-600 hover:text-gray-100" onClick={handleFormReset}>
              {uploading ? (
                <>
                  <FontAwesomeIcon icon={faSpinner} spin/> Uploading...
                </>
              ) : `Upload Another`}
            </button>
          </>
        )}
        {error ? (
          <p className="text-red-500 whitespace-pre-wrap">{error}</p>
        ) : null}
      </>
    </CenteredBlockPage>
  );
}
