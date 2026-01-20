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
        const messageDiv = document.getElementById('contact-form-message');
        
        if (form && messageDiv) {
            form.addEventListener('submit', function(event) {
                // Store a flag to show message after page reload (if Netlify redirects)
                sessionStorage.setItem('contactFormSubmitted', 'true');
                
                // Show success message immediately
                setTimeout(function() {
                    messageDiv.style.display = 'block';
                    messageDiv.className = 'form-message form-message-success';
                    messageDiv.innerHTML = '<strong>Thank you!</strong> Your message has been sent successfully. We\'ll get back to you as soon as possible.';
                    messageDiv.setAttribute('role', 'alert');
                    
                    // Reset the form
                    form.reset();
                    
                    // Scroll to message
                    messageDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                }, 500);
            });
            
            // Check if form was just submitted (after Netlify redirect)
            if (sessionStorage.getItem('contactFormSubmitted') === 'true') {
                sessionStorage.removeItem('contactFormSubmitted');
                messageDiv.style.display = 'block';
                messageDiv.className = 'form-message form-message-success';
                messageDiv.innerHTML = '<strong>Thank you!</strong> Your message has been sent successfully. We\'ll get back to you as soon as possible.';
                messageDiv.setAttribute('role', 'alert');
                messageDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
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
