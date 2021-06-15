import {useHistory} from 'react-router-dom';

type InteralNavigator = {
  navigate: (to: string) => void;
}

/**
 * Use the internal navigator hook.
 *
 * @param {boolean} push When `true`, we push a new state to history. When `false`, we replace the
 *                       current state. This determines how the browser history will react to the
 *                       back/forward button, if you want users to be able to use their back button
 *                       with internal navigation, ensure you're using push mode.
 */
export default function useInternalNavigator(push: boolean): InteralNavigator {
  const history = useHistory();

  function setQueryString(q: string) {
    if (typeof q !== 'string' || q.trim().length === 0) return;

    let qs = q.startsWith('?') ? q : '?' + q;
    if (push) {
      history.push(qs);
    } else {
      history.replace(qs);
    }
  }

  return {
    navigate: (to: string) => {
      setQueryString(to);
    },
  };
}
