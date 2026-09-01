/**
 * Swiss Style Alert & Toast System (SweetAlert2 wrapper)
 * Attached to window.SwissAlert
 */
(function(window) {
    'use strict';

    if (typeof Swal === 'undefined') {
        console.warn('SweetAlert2 not loaded; falling back to window.alert');
        window.SwissAlert = {
            toast: (msg) => console.log('TOAST:', msg),
            error: (msg, title) => alert((title ? title + ': ' : '') + msg),
            confirm: (msg, title) => Promise.resolve(window.confirm((title ? title + '\n' : '') + msg))
        };
        return;
    }

    const SwissAlert = {
        toast(message) {
            return Swal.fire({
                text: message,
                icon: 'success',
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                timerProgressBar: true,
                customClass: {
                    popup: 'swiss-swal-toast'
                }
            });
        },

        error(message, title = 'ERROR') {
            return Swal.fire({
                title: `<span class="swiss-swal-title">${title}</span>`,
                text: message,
                icon: 'error',
                confirmButtonText: '確定',
                customClass: {
                    popup: 'swiss-swal-popup',
                    confirmButton: 'btn btn-swiss-primary px-4'
                },
                buttonsStyling: false
            });
        },

        confirm(message, title = 'CONFIRMATION') {
            return Swal.fire({
                title: `<span class="swiss-swal-title">${title}</span>`,
                text: message,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: '確認執行',
                cancelButtonText: '取消',
                customClass: {
                    popup: 'swiss-swal-popup',
                    confirmButton: 'btn btn-swiss-danger px-4 me-2',
                    cancelButton: 'btn btn-swiss-outline px-4'
                },
                buttonsStyling: false
            }).then(result => result.isConfirmed);
        }
    };

    window.SwissAlert = SwissAlert;
})(window);
