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
     * Handle contact form submission with Netlify Forms
     */
    function initContactForm() {
        const form = document.getElementById('contact-form');
        
        if (form) {
            form.addEventListener('submit', function(event) {
                // Let Netlify handle the form submission
                // Netlify will automatically redirect to the action URL if specified
                // or we can handle it manually
                const formData = new FormData(form);
                
                fetch('/', {
                    method: 'POST',
                    body: formData
                }).then(function(response) {
                    if (response.ok) {
                        // Redirect to success page
                        window.location.href = '/contact-success.html';
                    }
                }).catch(function(error) {
                    console.error('Form submission error:', error);
                    // Still redirect to success page (Netlify may have processed it)
                    window.location.href = '/contact-success.html';
                });
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
