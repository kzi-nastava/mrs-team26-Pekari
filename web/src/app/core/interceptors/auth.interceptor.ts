import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = sessionStorage.getItem('auth_token') || localStorage.getItem('auth_token');

  let headers = req.headers;
  if (token) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }

  const authReq = req.clone({
    headers,
    withCredentials: true
  });
  return next(authReq);
};
