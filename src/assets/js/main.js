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
            // For Netlify Forms, we need to let it submit naturally
            // Add action attribute back for redirect
            form.setAttribute('action', '/contact-success.html');
            
            // Optional: Add a small delay before redirect to ensure Netlify processes it
            form.addEventListener('submit', function(event) {
                // Don't prevent default - let Netlify handle it
                // But add a fallback redirect in case Netlify's redirect doesn't work
                setTimeout(function() {
                    // Only redirect if we're still on the contact page
                    if (window.location.pathname === '/contact.html' || window.location.pathname === '/contact') {
                        window.location.href = '/contact-success.html';
                    }
                }, 2000);
            });
        }
    }
    
    /**
     * Handle newsletter form submission with Netlify Forms
     */
    function initNewsletterForm() {
        const form = document.getElementById('newsletter-form');
        const messageDiv = document.getElementById('newsletter-message');
        const emailInput = document.getElementById('newsletter-email');
        
        if (form && messageDiv) {
            form.addEventListener('submit', function(event) {
                // Let Netlify handle the form submission naturally
                // Show success message after a short delay
                setTimeout(function() {
                    messageDiv.style.display = 'block';
                    messageDiv.className = 'newsletter-message newsletter-message-success';
                    messageDiv.textContent = 'Thank you! You\'ve been subscribed to our newsletter.';
                    messageDiv.setAttribute('role', 'alert');
                    
                    // Clear the email input
                    if (emailInput) {
                        emailInput.value = '';
                    }
                    
                    // Hide message after 5 seconds
                    setTimeout(function() {
                        messageDiv.style.display = 'none';
                    }, 5000);
                }, 1000);
            });
        }
    }
    
    /**
     * Initialize all functionality
     */
    function init() {
        initMobileNav();
        initContactForm();
        initNewsletterForm();
    }
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
