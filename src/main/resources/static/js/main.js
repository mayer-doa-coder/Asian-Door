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
});
