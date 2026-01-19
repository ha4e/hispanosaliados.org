// Tests for main.js functionality

/**
 * Test: Mobile navigation toggle
 */
function testMobileNavToggle() {
    // Setup
    const navToggle = document.createElement('button');
    navToggle.className = 'nav-toggle';
    const navMenu = document.createElement('ul');
    navMenu.className = 'nav-menu';
    document.body.appendChild(navToggle);
    document.body.appendChild(navMenu);
    
    // Test
    navToggle.click();
    const hasActive = navMenu.classList.contains('active');
    
    // Cleanup
    document.body.removeChild(navToggle);
    document.body.removeChild(navMenu);
    
    return hasActive;
}

/**
 * Test: Contact form response handling
 */
function testContactFormResponse() {
    // Setup
    const form = document.createElement('form');
    form.id = 'contact-form';
    const responseDiv = document.createElement('div');
    responseDiv.id = 'form-response';
    document.body.appendChild(form);
    document.body.appendChild(responseDiv);
    
    // Simulate successful response
    const event = new CustomEvent('htmx:afterRequest', {
        detail: { xhr: { status: 200 } }
    });
    form.dispatchEvent(event);
    
    const hasSuccess = responseDiv.classList.contains('success');
    
    // Cleanup
    document.body.removeChild(form);
    document.body.removeChild(responseDiv);
    
    return hasSuccess;
}

// Export for testing framework
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        testMobileNavToggle,
        testContactFormResponse
    };
}
