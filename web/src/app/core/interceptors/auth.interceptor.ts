import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Auth interceptor that enables credentials (cookies) for all HTTP requests
 * No need to manually add Authorization header - backend uses HTTP-only cookies
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Clone request to enable credentials (cookies)
  const authReq = req.clone({
    withCredentials: true
  });

  return next(authReq);
};
