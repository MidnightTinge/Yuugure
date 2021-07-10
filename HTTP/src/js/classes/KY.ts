import ky from 'ky';

const KY = ky.create({
  // our API returns contextual 4xx responses with additional details in the status body. we do not
  // want those thrown as generic errors.
  throwHttpErrors: false,
});
export default KY;
