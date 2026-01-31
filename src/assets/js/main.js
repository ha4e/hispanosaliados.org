// HA4E Website JavaScript

(function() {
    'use strict';

    /**
     * Mobile navigation toggle
     */
    function initMobileNav() {
        const navToggle = document.querySelector('.nav-toggle');
        const navMenu = document.querySelector('.nav-menu');
        if (navToggle && navMenu) {
            navToggle.addEventListener('click', function() {
                navMenu.classList.toggle('active');
            });
        }
    }

    /**
     * Show success/error messages after HTMX form submit (Netlify Forms).
     * HTMX does the POST; we only update the message div since Netlify returns a full-page redirect.
     */
    function initFormMessages() {
        document.body.addEventListener('htmx:afterOnLoad', function(ev) {
            var target = ev.detail.target;
            var ok = ev.detail.successful;

            if (target.id === 'contact-form') {
                var msg = document.getElementById('contact-form-message');
                if (!msg) return;
                msg.style.display = 'block';
                msg.setAttribute('role', 'alert');
                if (ok) {
                    msg.className = 'form-message form-message-success';
                    msg.innerHTML = '<strong>Thank you!</strong> Your message has been sent successfully. We\'ll get back to you as soon as possible.';
                    target.reset();
                    msg.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                } else {
                    msg.className = 'form-message form-message-error';
                    msg.innerHTML = '<strong>Error:</strong> There was a problem sending your message. Please try again or contact us directly.';
                }
            } else if (target.id === 'newsletter-form') {
                var msg = document.getElementById('newsletter-message');
                if (!msg) return;
                msg.style.display = 'block';
                msg.setAttribute('role', 'alert');
                if (ok) {
                    msg.className = 'newsletter-message newsletter-message-success';
                    msg.textContent = 'Thank you! You\'ve been subscribed to our newsletter.';
                    var emailInput = document.getElementById('newsletter-email');
                    if (emailInput) emailInput.value = '';
                } else {
                    msg.className = 'newsletter-message newsletter-message-error';
                    msg.textContent = 'Something went wrong. Please try again or email us.';
                }
                setTimeout(function() { msg.style.display = 'none'; }, 5000);
            }
        });
    }

    function init() {
        initMobileNav();
        initFormMessages();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
