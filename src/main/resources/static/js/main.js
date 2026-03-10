/* =========================================
   Asian Door — Main JavaScript
   ========================================= */

document.addEventListener('DOMContentLoaded', function () {

    // Auto-dismiss flash alerts after 4 seconds
    document.querySelectorAll('.alert').forEach(function (alert) {
        setTimeout(function () {
            var bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            bsAlert.close();
        }, 4000);
    });

    // Navbar glass — darken on scroll
    var nav = document.getElementById('siteNav');
    if (nav) {
        var onScroll = function () {
            nav.classList.toggle('scrolled', window.scrollY > 20);
        };
        window.addEventListener('scroll', onScroll, { passive: true });
        onScroll();
    }

    // Active nav link — highlight based on current path
    var path = window.location.pathname;
    document.querySelectorAll('[data-nav-path]').forEach(function (link) {
        var navPath = link.getAttribute('data-nav-path');
        var isActive = navPath === '/'
            ? path === '/'
            : path.startsWith(navPath);
        if (isActive) { link.classList.add('active'); }
    });

});
