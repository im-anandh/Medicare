import { useCallback, useEffect, useState } from 'react';

// Tiny router: the current page lives in the URL hash ("#/patients"), so the
// browser's back button works and a refresh keeps you on the same page - and
// the Spring Boot server never has to know about page URLs.
export function useHashRoute(fallback = 'dashboard') {
  const read = () => window.location.hash.replace(/^#\/?/, '') || fallback;
  const [route, setRoute] = useState(read);

  useEffect(() => {
    const onChange = () => setRoute(read());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);   // eslint-disable-line react-hooks/exhaustive-deps

  const navigate = useCallback((id) => { window.location.hash = `/${id}`; }, []);
  return [route, navigate];
}
