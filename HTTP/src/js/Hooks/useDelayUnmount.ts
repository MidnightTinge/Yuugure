import {useEffect, useState} from 'react';

/**
 * A function that aids in the delay of unmounting a component.
 *
 * @param isMounted
 * @param delayTime
 * @author deckele (https://stackoverflow.com/users/6088926/deckele)
 * @link https://stackoverflow.com/a/54114180
 */
export default function useDelayUnmount(isMounted: boolean, delayTime: number): boolean {
  const [shouldRender, setShouldRender] = useState(false);

  useEffect(() => {
    let timeoutId: number;
    if (isMounted && !shouldRender) {
      setShouldRender(true);
    } else if (!isMounted && shouldRender) {
      timeoutId = (setTimeout(() => setShouldRender(false), delayTime) as unknown) as number;
    }
    return () => clearTimeout(timeoutId);
  }, [isMounted, delayTime, shouldRender]);
  return shouldRender;
}
