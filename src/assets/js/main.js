// HA4E Website JavaScript

(function() {
    'use strict';
    
    /**
     * Initialize mobile navigation toggle
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
     * Handle contact form submission with HTMX
     */
    function initContactForm() {
        const form = document.getElementById('contact-form');
        const responseDiv = document.getElementById('form-response');
        
        if (form) {
            form.addEventListener('htmx:afterRequest', function(event) {
                if (event.detail.xhr.status === 200) {
                    responseDiv.className = 'success';
                    form.reset();
                } else {
                    responseDiv.className = 'error';
                }
            });
        }
    }
    
    /**
     * Initialize all functionality
     */
    function init() {
        initMobileNav();
        initContactForm();
    }
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
