/**
 * Axios HTTP Client Wrapper for Swiss Ledger System
 * Auto JWT injection, response unnesting, and 401 handling.
 * Attached to window.http
 */
(function(window) {
    'use strict';

    if (typeof axios === 'undefined') {
        console.error('Axios not found. Please ensure axios.min.js is loaded.');
        return;
    }

    const http = axios.create({
        baseURL: '/api/v1',
        timeout: 15000,
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    });

    // Request Interceptor: Inject JWT Token
    http.interceptors.request.use(
        (config) => {
            const token = localStorage.getItem('jwt_token');
            if (token) {
                config.headers['Authorization'] = `Bearer ${token}`;
            }
            return config;
        },
        (error) => Promise.reject(error)
    );

    // Response Interceptor: Unpack ApiResponse & Handle Errors
    http.interceptors.response.use(
        (response) => {
            const res = response.data;
            if (res && typeof res.success === 'boolean') {
                if (res.success) {
                    return res.data;
                } else {
                    const msg = res.message || '操作失敗';
                    if (window.SwissAlert) {
                        window.SwissAlert.error(msg);
                    }
                    return Promise.reject(new Error(msg));
                }
            }
            return res;
        },
        (error) => {
            let errorMsg = '連線異常，請稍後再試';

            if (error.response) {
                const status = error.response.status;
                const data = error.response.data;

                if (data && data.message) {
                    errorMsg = data.message;
                } else if (status === 401) {
                    errorMsg = '登入逾期或尚未登入，請重新登入 (401)';
                } else if (status === 403) {
                    errorMsg = '您無權限執行此操作 (403)';
                } else if (status === 404) {
                    errorMsg = '請求之資源不存在 (404)';
                } else if (status === 409) {
                    errorMsg = '資料衝突或已存在 (409)';
                } else if (status >= 500) {
                    errorMsg = '伺服器內部錯誤 (500)';
                }

                if (status === 401) {
                    localStorage.removeItem('jwt_token');
                    localStorage.removeItem('user_info');
                    if (!window.location.pathname.endsWith('/login')) {
                        if (window.SwissAlert) {
                            window.SwissAlert.error('登入已逾期，請重新登入', 'UNAUTHORIZED').then(() => {
                                window.location.href = '/login';
                            });
                        } else {
                            window.location.href = '/login';
                        }
                        return Promise.reject(error);
                    }
                }
            }

            if (window.SwissAlert) {
                window.SwissAlert.error(errorMsg);
            }
            return Promise.reject(error);
        }
    );

    window.http = http;
})(window);
