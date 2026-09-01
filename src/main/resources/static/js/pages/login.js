/**
 * Login & Register Page Vue 3 Application
 */
(function() {
    'use strict';

    const { createApp, ref, reactive, onMounted } = Vue;

    createApp({
        setup() {
            const mode = ref('login'); // 'login' | 'register'
            const loading = ref(false);

            const loginForm = reactive({
                username: '',
                password: ''
            });

            const registerForm = reactive({
                username: '',
                password: '',
                email: '',
                displayName: ''
            });

            // Check if already authenticated
            onMounted(async () => {
                const token = localStorage.getItem('jwt_token');
                if (token) {
                    try {
                        await window.http.get('/auth/me');
                        window.location.href = '/ledger';
                    } catch (e) {
                        localStorage.removeItem('jwt_token');
                        localStorage.removeItem('user_info');
                    }
                }
            });

            const handleLogin = async () => {
                if (!loginForm.username || !loginForm.password) {
                    window.SwissAlert.error('請輸入帳號與密碼');
                    return;
                }

                loading.value = true;
                try {
                    const data = await window.http.post('/auth/login', {
                        username: loginForm.username,
                        password: loginForm.password
                    });

                    localStorage.setItem('jwt_token', data.token);
                    localStorage.setItem('user_info', JSON.stringify(data.user));

                    window.SwissAlert.toast(`歡迎回來，${data.user.displayName || data.user.username}`);
                    setTimeout(() => {
                        window.location.href = '/ledger';
                    }, 500);
                } catch (err) {
                    console.error('Login error:', err);
                } finally {
                    loading.value = false;
                }
            };

            const handleRegister = async () => {
                if (!registerForm.username || !registerForm.password || !registerForm.email) {
                    window.SwissAlert.error('請填寫所有必填欄位');
                    return;
                }

                loading.value = true;
                try {
                    await window.http.post('/auth/register', {
                        username: registerForm.username,
                        password: registerForm.password,
                        email: registerForm.email,
                        displayName: registerForm.displayName
                    });

                    window.SwissAlert.toast('帳號建立成功，自動為您登入...');
                    
                    // Auto login
                    const loginData = await window.http.post('/auth/login', {
                        username: registerForm.username,
                        password: registerForm.password
                    });

                    localStorage.setItem('jwt_token', loginData.token);
                    localStorage.setItem('user_info', JSON.stringify(loginData.user));

                    setTimeout(() => {
                        window.location.href = '/ledger';
                    }, 600);
                } catch (err) {
                    console.error('Register error:', err);
                } finally {
                    loading.value = false;
                }
            };

            return {
                mode,
                loading,
                loginForm,
                registerForm,
                handleLogin,
                handleRegister
            };
        }
    }).mount('#app');
})();
