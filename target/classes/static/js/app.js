/* ============================================================
   RevPay - Application JavaScript
   ============================================================ */

// ---- Sidebar Mobile Toggle ----
document.addEventListener('DOMContentLoaded', function () {
    const sidebar = document.querySelector('.sidebar');
    const toggleBtn = document.getElementById('sidebar-toggle');
    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => sidebar.classList.toggle('open'));
    }

    // Close sidebar on outside click (mobile)
    document.addEventListener('click', (e) => {
        if (sidebar && sidebar.classList.contains('open') &&
            !sidebar.contains(e.target) && e.target !== toggleBtn) {
            sidebar.classList.remove('open');
        }
    });



    // Auto-hide alerts
    document.querySelectorAll('.alert[data-auto-hide]').forEach(alert => {
        setTimeout(() => {
            alert.style.opacity = '0';
            alert.style.transition = 'opacity 0.5s';
            setTimeout(() => alert.remove(), 500);
        }, 4000);
    });

    // Mark active nav link
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-link').forEach(link => {
        if (link.getAttribute('href') && currentPath.startsWith(link.getAttribute('href')) &&
            link.getAttribute('href') !== '/') {
            link.classList.add('active');
        }
    });

    // Amount input formatting
    document.querySelectorAll('input[data-currency]').forEach(input => {
        input.addEventListener('input', function () {
            let val = this.value.replace(/[^\d.]/g, '');
            const parts = val.split('.');
            if (parts.length > 2) val = parts[0] + '.' + parts.slice(1).join('');
            this.value = val;
        });
    });

    // Confirm delete modals
    document.querySelectorAll('[data-confirm]').forEach(btn => {
        btn.addEventListener('click', function (e) {
            if (!confirm(this.dataset.confirm || 'Are you sure?')) e.preventDefault();
        });
    });

    // Dynamic invoice items
    initInvoiceItems();

    // Chart.js initialization
    initCharts();
});

// ---- Invoice Items ----
function initInvoiceItems() {
    const addBtn = document.getElementById('add-item-btn');
    const container = document.getElementById('invoice-items');
    if (!addBtn || !container) return;

    addBtn.addEventListener('click', () => {
        const idx = container.children.length;
        const row = document.createElement('div');
        row.className = 'invoice-item-row flex gap-12 mt-8';
        row.innerHTML = `
            <input type="text" name="itemDesc" class="form-control" placeholder="Description" style="flex:3" required>
            <input type="number" name="itemQty" class="form-control" placeholder="Qty" step="0.01" min="1" value="1" style="flex:1" oninput="calcItemTotal(this)">
            <input type="number" name="itemPrice" class="form-control" placeholder="Unit Price" step="0.01" min="0" style="flex:2" oninput="calcItemTotal(this)">
            <input type="number" name="itemTax" class="form-control" placeholder="Tax%" step="0.01" min="0" value="0" style="flex:1" oninput="calcItemTotal(this)">
            <input type="text" class="form-control item-total" placeholder="Total" style="flex:2" readonly>
            <button type="button" class="btn btn-danger btn-sm" onclick="this.parentElement.remove(); updateInvoiceTotal()">✕</button>
        `;
        container.appendChild(row);
    });
}

function calcItemTotal(input) {
    const row = input.closest('.invoice-item-row');
    if (!row) return;
    const qty = parseFloat(row.querySelector('[name="itemQty"]')?.value || 0);
    const price = parseFloat(row.querySelector('[name="itemPrice"]')?.value || 0);
    const tax = parseFloat(row.querySelector('[name="itemTax"]')?.value || 0);
    const total = qty * price * (1 + tax / 100);
    const totalEl = row.querySelector('.item-total');
    if (totalEl) totalEl.value = '₹' + total.toFixed(2);
    updateInvoiceTotal();
}

function updateInvoiceTotal() {
    let subtotal = 0, taxTotal = 0;
    document.querySelectorAll('.invoice-item-row').forEach(row => {
        const qty = parseFloat(row.querySelector('[name="itemQty"]')?.value || 0);
        const price = parseFloat(row.querySelector('[name="itemPrice"]')?.value || 0);
        const tax = parseFloat(row.querySelector('[name="itemTax"]')?.value || 0);
        const base = qty * price;
        subtotal += base;
        taxTotal += base * tax / 100;
    });
    const el = (id) => document.getElementById(id);
    if (el('invoice-subtotal')) el('invoice-subtotal').textContent = '₹' + subtotal.toFixed(2);
    if (el('invoice-tax')) el('invoice-tax').textContent = '₹' + taxTotal.toFixed(2);
    if (el('invoice-total')) el('invoice-total').textContent = '₹' + (subtotal + taxTotal).toFixed(2);
}

// ---- Charts (Chart.js) ----
function initCharts() {
    // Revenue Chart
    const revenueCanvas = document.getElementById('revenueChart');
    if (revenueCanvas && typeof Chart !== 'undefined') {
        const labels = JSON.parse(revenueCanvas.dataset.labels || '[]');
        const data = JSON.parse(revenueCanvas.dataset.values || '[]');
        new Chart(revenueCanvas, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label: 'Revenue (₹)',
                    data,
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59,130,246,0.1)',
                    fill: true,
                    tension: 0.4,
                    pointBackgroundColor: '#3b82f6',
                    pointRadius: 4
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { labels: { color: '#94a3b8' } } },
                scales: {
                    x: { ticks: { color: '#64748b' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                    y: { ticks: { color: '#64748b' }, grid: { color: 'rgba(255,255,255,0.05)' } }
                }
            }
        });
    }

    // Invoice Status Doughnut
    const invoiceCanvas = document.getElementById('invoiceChart');
    if (invoiceCanvas && typeof Chart !== 'undefined') {
        const labels = JSON.parse(invoiceCanvas.dataset.labels || '[]');
        const data = JSON.parse(invoiceCanvas.dataset.values || '[]');
        new Chart(invoiceCanvas, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    data,
                    backgroundColor: ['#10b981', '#3b82f6', '#f59e0b', '#ef4444', '#64748b'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { labels: { color: '#94a3b8' } } },
                cutout: '65%'
            }
        });
    }
}

// ---- PIN toggle visibility ----
function togglePin(inputId) {
    const el = document.getElementById(inputId);
    if (el) el.type = el.type === 'password' ? 'text' : 'password';
}

// ---- Copy to clipboard ----
function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showToast('Copied to clipboard!', 'success');
    });
}

// ---- Toast Notifications ----
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `alert alert-${type}`;
    toast.style.cssText = 'position:fixed;top:20px;right:20px;z-index:9999;min-width:280px;animation:fadeIn 0.3s ease';
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}
