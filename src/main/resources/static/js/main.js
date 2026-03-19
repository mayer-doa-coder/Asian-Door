/* =========================================
   Asian Door — Main JavaScript
   ========================================= */

document.addEventListener('DOMContentLoaded', function () {

    var contextMeta = document.querySelector('meta[name="app-context-path"]');
    var contextPath = contextMeta && contextMeta.getAttribute('content')
        ? contextMeta.getAttribute('content')
        : '/';
    if (!contextPath.startsWith('/')) {
        contextPath = '/' + contextPath;
    }
    if (contextPath.length > 1 && contextPath.endsWith('/')) {
        contextPath = contextPath.slice(0, -1);
    }

    function appPath(path) {
        var normalizedPath = path.startsWith('/') ? path : '/' + path;
        if (contextPath === '/') {
            return normalizedPath;
        }
        return contextPath + normalizedPath;
    }

    function resolveImageUrl(url) {
        if (!url || typeof url !== 'string') {
            return appPath('/images/country-house-door-pine-wood.jpg');
        }

        var trimmed = url.trim();
        if (trimmed === '') {
            return appPath('/images/country-house-door-pine-wood.jpg');
        }

        if (trimmed.startsWith('http://') || trimmed.startsWith('https://') || trimmed.startsWith('data:')) {
            return trimmed;
        }

        if (contextPath !== '/' && trimmed.startsWith(contextPath + '/')) {
            return trimmed;
        }

        return appPath(trimmed);
    }

    var CURRENCY = new Intl.NumberFormat('en-BD', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    function formatCurrency(value) {
        return 'Tk ' + CURRENCY.format(Number(value || 0));
    }

    function setCartBadgeCount(count) {
        document.querySelectorAll('.nav-cart-badge').forEach(function (badge) {
            badge.textContent = String(count || 0);
        });
    }

    function safeJsonResponse(response) {
        var contentType = (response.headers.get('content-type') || '').toLowerCase();
        if (!contentType.includes('application/json')) {
            return Promise.resolve(null);
        }
        return response.json();
    }

    function showAlert(host, type, message) {
        if (!host) {
            return;
        }
        host.innerHTML = [
            '<div class="alert alert-' + type + ' alert-dismissible fade show mb-0" role="alert">',
            message,
            '<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>',
            '</div>'
        ].join('');
    }

    function refreshCartBadge() {
        return fetch(appPath('/cart'), {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        })
            .then(function (response) {
                if (!response.ok) {
                    return null;
                }
                return safeJsonResponse(response);
            })
            .then(function (data) {
                if (!data || !Array.isArray(data.items)) {
                    return;
                }
                var totalQty = data.items.reduce(function (sum, item) {
                    return sum + Number(item.quantity || 0);
                }, 0);
                setCartBadgeCount(totalQty);
            })
            .catch(function () {
                // Ignore badge refresh failures for unauthenticated users.
            });
    }

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
    if (contextPath !== '/' && path.startsWith(contextPath)) {
        path = path.slice(contextPath.length) || '/';
    }
    document.querySelectorAll('[data-nav-path]').forEach(function (link) {
        var navPath = link.getAttribute('data-nav-path');
        var isActive = navPath === '/'
            ? path === '/'
            : path.startsWith(navPath);
        if (isActive) { link.classList.add('active'); }
    });

    // Product cards: add to cart.
    document.addEventListener('click', function (event) {
        var button = event.target.closest('.js-add-to-cart');
        if (!button) {
            return;
        }

        var productId = button.getAttribute('data-product-id');
        if (!productId) {
            return;
        }

        var quantity = 1;
        var quantityInputId = button.getAttribute('data-quantity-input');
        if (quantityInputId) {
            var quantityInput = document.getElementById(quantityInputId);
            if (quantityInput) {
                var parsedQuantity = Number(quantityInput.value);
                if (Number.isFinite(parsedQuantity) && parsedQuantity > 0) {
                    quantity = Math.floor(parsedQuantity);
                }
            }
        }

        var maxStock = Number(button.getAttribute('data-max-stock'));
        if (Number.isFinite(maxStock) && maxStock > 0 && quantity > maxStock) {
            quantity = maxStock;
        }

        var originalHtml = button.innerHTML;
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Adding...';

        fetch(appPath('/cart/add/' + productId + '?quantity=' + quantity), {
            method: 'POST',
            headers: { 'Accept': 'application/json' }
        })
            .then(function (response) {
                return safeJsonResponse(response).then(function (data) {
                    if (!response.ok || !data || data.success !== true) {
                        var msg = data && data.message ? data.message : 'Unable to add product to cart.';
                        throw new Error(msg);
                    }
                    return data;
                });
            })
            .then(function () {
                button.classList.remove('btn-outline-dark');
                button.classList.add('btn-success');
                button.innerHTML = '<i class="bi bi-check-circle me-1"></i>Added';

                refreshCartBadge();

                setTimeout(function () {
                    button.classList.remove('btn-success');
                    button.classList.add('btn-outline-dark');
                    button.innerHTML = originalHtml;
                    button.disabled = false;
                }, 1200);
            })
            .catch(function (error) {
                button.innerHTML = originalHtml;
                button.disabled = false;
                window.alert(error.message || 'Unable to add to cart.');
            });
    });

    // Product detail quantity controls.
    var detailQtyInput = document.getElementById('detailQty');
    var qtyDecreaseBtn = document.querySelector('.js-qty-decrease');
    var qtyIncreaseBtn = document.querySelector('.js-qty-increase');
    var detailAddButton = document.querySelector('.js-add-to-cart[data-quantity-input="detailQty"]');

    if (detailQtyInput && qtyDecreaseBtn && qtyIncreaseBtn) {
        var maxDetailStock = detailAddButton ? Number(detailAddButton.getAttribute('data-max-stock')) : NaN;
        if (!Number.isFinite(maxDetailStock) || maxDetailStock < 1) {
            maxDetailStock = 0;
        }

        function updateDetailQtyControls() {
            var currentQty = Number(detailQtyInput.value);
            if (!Number.isFinite(currentQty) || currentQty < 1) {
                currentQty = 1;
            }

            if (maxDetailStock > 0 && currentQty > maxDetailStock) {
                currentQty = maxDetailStock;
            }

            detailQtyInput.value = String(currentQty);

            var outOfStock = maxDetailStock <= 0;
            qtyDecreaseBtn.disabled = outOfStock || currentQty <= 1;
            qtyIncreaseBtn.disabled = outOfStock || currentQty >= maxDetailStock;
        }

        qtyDecreaseBtn.addEventListener('click', function () {
            var currentQty = Number(detailQtyInput.value) || 1;
            detailQtyInput.value = String(Math.max(1, currentQty - 1));
            updateDetailQtyControls();
        });

        qtyIncreaseBtn.addEventListener('click', function () {
            if (maxDetailStock <= 0) {
                return;
            }
            var currentQty = Number(detailQtyInput.value) || 1;
            detailQtyInput.value = String(Math.min(maxDetailStock, currentQty + 1));
            updateDetailQtyControls();
        });

        updateDetailQtyControls();
    }

    // Cart page logic.
    var cartPage = document.querySelector('[data-cart-page="true"]');
    if (cartPage) {
        var tbody = document.getElementById('cartTableBody');
        var emptyState = document.getElementById('cartEmptyState');
        var summaryItems = document.getElementById('cartSummaryItems');
        var summaryTotal = document.getElementById('cartSummaryTotal');
        var checkoutBtn = document.getElementById('checkoutBtn');
        var alertHost = document.getElementById('cartAlertHost');

        function renderCart(data) {
            var items = Array.isArray(data.items) ? data.items : [];
            var totalAmount = Number(data.totalAmount || 0);
            var itemQty = items.reduce(function (sum, item) {
                return sum + Number(item.quantity || 0);
            }, 0);

            summaryItems.textContent = String(itemQty);
            summaryTotal.textContent = formatCurrency(totalAmount);
            setCartBadgeCount(itemQty);
            checkoutBtn.disabled = items.length === 0;

            if (items.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">No items in cart yet.</td></tr>';
                emptyState.classList.remove('d-none');
                return;
            }

            emptyState.classList.add('d-none');
            tbody.innerHTML = items.map(function (item) {
                var fallbackImage = appPath('/images/country-house-door-pine-wood.jpg');
                var imageUrl = resolveImageUrl(item.imageUrl);
                return [
                    '<tr>',
                    '<td>',
                    '<div class="cart-product">',
                    '<img src="' + imageUrl + '" alt="' + (item.productName || 'Product') + '" onerror="this.onerror=null;this.src=\'' + fallbackImage + '\'">',
                    '<div>',
                    '<p class="cart-product-name">' + (item.productName || 'Product') + '</p>',
                    '<p class="cart-meta">Product ID: ' + (item.productId || '-') + '</p>',
                    '</div>',
                    '</div>',
                    '</td>',
                    '<td class="cart-price">' + formatCurrency(item.unitPrice) + '</td>',
                    '<td><span class="cart-qty-pill">&minus;&nbsp;&nbsp;' + Number(item.quantity || 0) + '&nbsp;&nbsp;+</span></td>',
                    '<td class="cart-price">' + formatCurrency(item.lineTotal) + '</td>',
                    '<td>',
                    '<button type="button" class="remove-btn js-remove-from-cart" data-product-id="' + item.productId + '">',
                    '<i class="bi bi-trash3"></i>',
                    '</button>',
                    '</td>',
                    '</tr>'
                ].join('');
            }).join('');
        }

        function loadCart() {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">Loading cart...</td></tr>';
            return fetch(appPath('/cart'), {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('Unable to load cart.');
                    }
                    return safeJsonResponse(response);
                })
                .then(function (data) {
                    if (!data) {
                        throw new Error('Cart data format is invalid.');
                    }
                    renderCart(data);
                })
                .catch(function (error) {
                    showAlert(alertHost, 'danger', error.message || 'Unable to load cart.');
                    tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">Failed to load cart.</td></tr>';
                });
        }

        tbody.addEventListener('click', function (event) {
            var button = event.target.closest('.js-remove-from-cart');
            if (!button) {
                return;
            }

            var productId = button.getAttribute('data-product-id');
            if (!productId) {
                return;
            }

            button.disabled = true;

            fetch(appPath('/cart/remove/' + productId), {
                method: 'POST',
                headers: { 'Accept': 'application/json' }
            })
                .then(function (response) {
                    return safeJsonResponse(response).then(function (data) {
                        if (!response.ok || !data || data.success !== true) {
                            var msg = data && data.message ? data.message : 'Unable to remove product from cart.';
                            throw new Error(msg);
                        }
                    });
                })
                .then(function () {
                    showAlert(alertHost, 'success', 'Item removed from cart.');
                    return loadCart();
                })
                .catch(function (error) {
                    showAlert(alertHost, 'danger', error.message || 'Unable to remove item.');
                })
                .finally(function () {
                    button.disabled = false;
                });
        });

        checkoutBtn.addEventListener('click', function () {
            window.location.href = appPath('/checkout');
        });

        loadCart();
    }

    // Checkout page logic.
    var checkoutPage = document.querySelector('[data-checkout-page="true"]');
    if (checkoutPage) {
        var checkoutItemsList = document.getElementById('checkoutItemsList');
        var checkoutItemCount = document.getElementById('checkoutItemCount');
        var checkoutTotalPrice = document.getElementById('checkoutTotalPrice');
        var checkoutEmptyState = document.getElementById('checkoutEmptyState');
        var confirmOrderBtn = document.getElementById('confirmOrderBtn');
        var checkoutAlertHost = document.getElementById('checkoutAlertHost');

        function renderCheckout(data) {
            var items = Array.isArray(data.items) ? data.items : [];
            var totalAmount = Number(data.totalAmount || 0);
            var itemQty = items.reduce(function (sum, item) {
                return sum + Number(item.quantity || 0);
            }, 0);

            checkoutItemCount.textContent = String(itemQty);
            checkoutTotalPrice.textContent = formatCurrency(totalAmount);

            if (items.length === 0) {
                confirmOrderBtn.disabled = true;
                checkoutEmptyState.classList.remove('d-none');
                checkoutItemsList.innerHTML = '<div class="list-group-item text-muted text-center py-4">Cart is empty.</div>';
                return;
            }

            confirmOrderBtn.disabled = false;
            checkoutEmptyState.classList.add('d-none');

            checkoutItemsList.innerHTML = items.map(function (item) {
                return [
                    '<div class="list-group-item">',
                    '<div class="d-flex justify-content-between align-items-start gap-3">',
                    '<div>',
                    '<p class="checkout-item-title mb-1">' + (item.productName || 'Product') + '</p>',
                    '<p class="checkout-item-meta mb-0">Qty: ' + Number(item.quantity || 0) + ' x ' + formatCurrency(item.unitPrice) + '</p>',
                    '</div>',
                    '<div class="fw-bold text-danger">' + formatCurrency(item.lineTotal) + '</div>',
                    '</div>',
                    '</div>'
                ].join('');
            }).join('');
        }

        function loadCheckout() {
            return fetch(appPath('/cart'), {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('Unable to load checkout summary.');
                    }
                    return safeJsonResponse(response);
                })
                .then(function (data) {
                    if (!data) {
                        throw new Error('Checkout data format is invalid.');
                    }
                    renderCheckout(data);
                })
                .catch(function (error) {
                    showAlert(checkoutAlertHost, 'danger', error.message || 'Unable to load checkout data.');
                });
        }

        confirmOrderBtn.addEventListener('click', function () {
            confirmOrderBtn.disabled = true;
            confirmOrderBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Confirming...';

            fetch(appPath('/orders'), {
                method: 'POST',
                headers: { 'Accept': 'application/json' }
            })
                .then(function (response) {
                    return safeJsonResponse(response).then(function (data) {
                        if (!response.ok || !data || data.success !== true) {
                            var msg = data && data.message ? data.message : 'Unable to place order.';
                            throw new Error(msg);
                        }
                        return data;
                    });
                })
                .then(function (data) {
                    window.location.href = appPath('/orders/history') + '?placed=' + encodeURIComponent(data.orderId);
                })
                .catch(function (error) {
                    showAlert(checkoutAlertHost, 'danger', error.message || 'Unable to place order.');
                    confirmOrderBtn.disabled = false;
                    confirmOrderBtn.innerHTML = '<i class="bi bi-check2-circle me-1"></i>Confirm Order';
                });
        });

        loadCheckout();
    }

    refreshCartBadge();

});
