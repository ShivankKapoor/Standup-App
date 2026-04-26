(function () {
    var toastEl = null;
    var toastTimer = null;

    var icons = {
        info: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
        error: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
        success: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>'
    };

    // Aliases
    icons.good = icons.success;
    icons.bad  = icons.error;

    function createToast() {
        var el = document.createElement('div');
        el.className = 'toast';
        el.setAttribute('role', 'alert');
        el.setAttribute('aria-live', 'polite');
        el.innerHTML =
            '<span class="toast-icon"></span>' +
            '<span class="toast-message"></span>' +
            '<button class="toast-close" aria-label="Dismiss">&times;</button>';
        el.querySelector('.toast-close').addEventListener('click', function () {
            clearTimeout(toastTimer);
            hide();
        });
        document.body.appendChild(el);
        return el;
    }

    function hide() {
        if (toastEl) toastEl.classList.remove('show');
    }

    window.showToast = function (message, opts) {
        opts = opts || {};
        var type = opts.type || 'info';
        var duration = opts.duration !== undefined ? opts.duration : 3000;

        if (!toastEl) toastEl = createToast();

        // Resolve aliases
        if (type === 'good') type = 'success';
        if (type === 'bad')  type = 'error';

        toastEl.className = 'toast toast--' + type;
        toastEl.querySelector('.toast-icon').innerHTML = icons[type] || icons.info;
        toastEl.querySelector('.toast-message').textContent = message;

        clearTimeout(toastTimer);
        void toastEl.offsetWidth; // force reflow so re-triggering re-animates
        toastEl.classList.add('show');

        if (duration > 0) {
            toastTimer = setTimeout(hide, duration);
        }
    };
})();
