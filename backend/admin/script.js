// App State
let token = localStorage.getItem('crm_token') || null;
let userEmail = localStorage.getItem('crm_email') || null;
let complaints = [];
let news = [];
let employees = [];

// DOM Elements
const loginContainer = document.getElementById('login-container');
const dashboardContainer = document.getElementById('dashboard-container');
const loginForm = document.getElementById('login-form');
const btnLogin = document.getElementById('btn-login');
const userEmailEl = document.getElementById('user-email');
const btnLogout = document.getElementById('btn-logout');

const navItems = document.querySelectorAll('.nav-item');
const tabContents = document.querySelectorAll('.tab-content');
const tabTitle = document.getElementById('tab-title');
const tabSubtitle = document.getElementById('tab-subtitle');

// Complaints Elements
const complaintsList = document.getElementById('complaints-list');
const complaintsLoader = document.getElementById('complaints-loader');
const searchComplaints = document.getElementById('search-complaints');
const filterStatus = document.getElementById('filter-status');
const filterPriority = document.getElementById('filter-priority');

// Stats Elements
const statPending = document.getElementById('stat-pending');
const statProgress = document.getElementById('stat-progress');
const statResolved = document.getElementById('stat-resolved');

// News Elements
const newsForm = document.getElementById('news-form');
const newsList = document.getElementById('news-list');
const btnPublishNews = document.getElementById('btn-publish-news');

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
    if (token) {
        showDashboard();
    } else {
        showLogin();
    }

    // Setup Event Listeners
    loginForm.addEventListener('submit', handleLogin);
    btnLogout.addEventListener('click', handleLogout);
    
    // Tab switching
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });

    // Filters & Search
    searchComplaints.addEventListener('input', renderComplaints);
    filterStatus.addEventListener('change', renderComplaints);
    filterPriority.addEventListener('change', renderComplaints);

    // News Form
    newsForm.addEventListener('submit', handlePublishNews);

    // Employee Registration Form
    const employeeForm = document.getElementById('employee-register-form');
    if (employeeForm) {
        employeeForm.addEventListener('submit', handleRegisterEmployee);
    }
});

// Toast System
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    toast.innerHTML = `
        <span>${message}</span>
        <span class="toast-close" style="cursor:pointer; margin-left: 10px; font-weight: bold;">×</span>
    `;

    toast.querySelector('.toast-close').addEventListener('click', () => {
        toast.remove();
    });

    container.appendChild(toast);

    // Auto-remove after 4 seconds
    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Navigation & Screen States
function showLogin() {
    loginContainer.classList.remove('hidden');
    dashboardContainer.classList.add('hidden');
}

async function loadEmployees() {
    try {
        const response = await fetch('/api/auth/employees', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (response.ok) {
            employees = await response.json();
        }
    } catch (err) {
        console.error('Failed to load employees:', err);
    }
}

async function showDashboard() {
    loginContainer.classList.add('hidden');
    dashboardContainer.classList.remove('hidden');
    userEmailEl.textContent = userEmail;
    
    // Load initial data
    await loadEmployees();
    loadComplaints();
    loadNews();
}

async function switchTab(tabId) {
    // Nav Items Active state
    navItems.forEach(btn => {
        if (btn.getAttribute('data-tab') === tabId) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    // Content Display
    tabContents.forEach(content => {
        if (content.id === tabId) {
            content.classList.remove('hidden');
        } else {
            content.classList.add('hidden');
        }
    });

    // Header Titles
    if (tabId === 'tab-complaints') {
        tabTitle.textContent = "Complaints Manager";
        tabSubtitle.textContent = "Track, assign, and resolve customer complaints";
        await loadEmployees();
        loadComplaints();
    } else if (tabId === 'tab-news') {
        tabTitle.textContent = "Publish News Feed";
        tabSubtitle.textContent = "Broadcast announcement updates to all mobile CRM feeds";
        loadNews();
    } else if (tabId === 'tab-employee') {
        tabTitle.textContent = "Add Employee";
        tabSubtitle.textContent = "Register a new secure staff account authorized to use this portal";
    }
}

// Authentication Logic
async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    const spinner = btnLogin.querySelector('.spinner');
    const btnText = btnLogin.querySelector('span');

    spinner.classList.remove('hidden');
    btnText.classList.add('hidden');
    btnLogin.disabled = true;

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Login failed.');
        }

        // Verify it is an admin user
        if (data.user.role !== 'ADMIN') {
            throw new Error('Access denied. Only employee accounts are authorized to use this portal.');
        }

        // Save token and display
        token = data.token;
        userEmail = data.user.email;
        localStorage.setItem('crm_token', token);
        localStorage.setItem('crm_email', userEmail);

        showToast('Welcome to Tech Rise Portal!', 'success');
        showDashboard();

    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        spinner.classList.add('hidden');
        btnText.classList.remove('hidden');
        btnLogin.disabled = false;
    }
}

function handleLogout() {
    token = null;
    userEmail = null;
    localStorage.removeItem('crm_token');
    localStorage.removeItem('crm_email');
    showToast('Logged out successfully.');
    showLogin();
}

// Complaints Manager Logic
async function loadComplaints() {
    complaintsLoader.classList.remove('hidden');
    try {
        const response = await fetch('/api/complaints', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.status === 401 || response.status === 403) {
            handleLogout();
            throw new Error('Session expired. Please sign in again.');
        }

        if (!response.ok) {
            throw new Error('Failed to retrieve complaints.');
        }

        complaints = await response.json();
        calculateStats();
        renderComplaints();

    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        complaintsLoader.classList.add('hidden');
    }
}

function calculateStats() {
    let pending = 0;
    let progress = 0;
    let resolved = 0;

    complaints.forEach(c => {
        if (c.status === 'PENDING') pending++;
        else if (c.status === 'IN_PROGRESS') progress++;
        else if (c.status === 'RESOLVED') resolved++;
    });

    statPending.textContent = pending;
    statProgress.textContent = progress;
    statResolved.textContent = resolved;
}

function renderComplaints() {
    complaintsList.innerHTML = '';
    
    const query = searchComplaints.value.toLowerCase().trim();
    const statusVal = filterStatus.value;
    const priorityVal = filterPriority.value;

    const filtered = complaints.filter(c => {
        // Status filter
        if (statusVal !== 'ALL' && c.status !== statusVal) return false;
        
        // Priority filter
        if (priorityVal !== 'ALL' && c.priority !== priorityVal) return false;

        // Search text
        if (query) {
            const title = (c.title || '').toLowerCase();
            const desc = (c.description || '').toLowerCase();
            const email = (c.customerEmail || '').toLowerCase();
            const id = (c.id || '').toLowerCase();
            
            return title.includes(query) || desc.includes(query) || email.includes(query) || id.includes(query);
        }

        return true;
    });

    if (filtered.length === 0) {
        complaintsList.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--text-secondary);">
                No complaints found matching the criteria.
            </div>
        `;
        return;
    }

    filtered.forEach(c => {
        const card = document.createElement('div');
        card.className = 'complaint-card';

        // Date formatting helper
        let formattedDate = 'Just now';
        if (c.createdAt) {
            const timestamp = c.createdAt._seconds ? c.createdAt._seconds * 1000 : c.createdAt.seconds * 1000;
            formattedDate = new Date(timestamp).toLocaleString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        }

        const statusClass = `badge-${c.status.toLowerCase().replace('_', '-')}`;
        const priorityClass = `badge-priority-${c.priority.toLowerCase()}`;

        card.innerHTML = `
            <div class="card-row">
                <div class="card-title-group">
                    <h3>${escapeHtml(c.title)}</h3>
                    <span class="complaint-id">ID: ${c.id}</span>
                </div>
                <span class="badge ${statusClass}">${c.status}</span>
            </div>
            
            <p class="complaint-desc">${escapeHtml(c.description)}</p>
            
            ${c.imageBase64 ? `
                <div class="complaint-image-attachment" style="margin-top: 12px; margin-bottom: 12px; display: flex; justify-content: flex-start;">
                    <a href="${c.imageBase64}" target="_blank" title="Click to view full size">
                        <img src="${c.imageBase64}" alt="Complaint Attachment" style="max-width: 100%; max-height: 250px; border-radius: 8px; border: 1px solid var(--border-color); cursor: zoom-in;" />
                    </a>
                </div>
            ` : ''}
            
            <div class="metadata-list">
                <div class="meta-item">
                    <span class="meta-label">Customer:</span>
                    <span class="meta-value">${escapeHtml(c.customerEmail)}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">Priority:</span>
                    <span class="badge ${priorityClass}">${c.priority}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">Registered:</span>
                    <span class="meta-value">${formattedDate}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">Assigned To:</span>
                    <span class="meta-value">${c.assignedAdminEmail ? escapeHtml(c.assignedAdminEmail) : '<em style="color: var(--text-muted);">Unassigned</em>'}</span>
                </div>
            </div>

            <div class="card-actions">
                <div style="display: flex; flex-direction: column; gap: 8px;">
                    <div class="action-form-row">
                        <select id="status-select-${c.id}" style="flex: 1;">
                            <option value="PENDING" ${c.status === 'PENDING' ? 'selected' : ''}>Pending</option>
                            <option value="IN_PROGRESS" ${c.status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
                            <option value="RESOLVED" ${c.status === 'RESOLVED' ? 'selected' : ''}>Resolved</option>
                        </select>
                        <select id="employee-select-${c.id}" style="flex: 1;">
                            <option value="">-- Unassigned --</option>
                            ${employees.map(emp => `
                                <option value="${emp.id}" ${c.assignedAdminId === emp.id ? 'selected' : ''}>${escapeHtml(emp.email)}</option>
                            `).join('')}
                        </select>
                    </div>
                    <button class="btn-update" data-id="${c.id}" style="width: 100%;">Update Details</button>
                </div>
            </div>
        `;
        complaintsList.appendChild(card);

        // Bind update handler programmatically to comply with CSP script-src-attr 'none'
        const btn = card.querySelector(`button[data-id="${c.id}"]`);
        if (btn) {
            btn.addEventListener('click', () => {
                updateStatus(c.id);
            });
        }
    });
}

// Update Complaint Status API helper
async function updateStatus(complaintId) {
    const select = document.getElementById(`status-select-${complaintId}`);
    const newStatus = select.value;
    const empSelect = document.getElementById(`employee-select-${complaintId}`);
    const assignedAdminId = empSelect.value || null;

    try {
        const response = await fetch(`/api/complaints/${complaintId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ 
                status: newStatus,
                assignedAdminId: assignedAdminId
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Failed to update status.');
        }

        showToast('Complaint status updated successfully!', 'success');
        
        // Reload complaints list
        loadComplaints();

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// News Announcements Logic
async function loadNews() {
    try {
        const response = await fetch('/api/news', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            news = await response.json();
            renderNewsHistory();
        }
    } catch (err) {
        console.error('Failed to load news history:', err);
    }
}

function renderNewsHistory() {
    newsList.innerHTML = '';
    
    if (news.length === 0) {
        newsList.innerHTML = '<p style="color: var(--text-secondary); text-align: center; padding: 20px;">No announcement history.</p>';
        return;
    }

    news.forEach(item => {
        let formattedDate = 'Recent';
        if (item.createdAt) {
            const timestamp = item.createdAt._seconds ? item.createdAt._seconds * 1000 : item.createdAt.seconds * 1000;
            formattedDate = new Date(timestamp).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric'
            });
        }

        const card = document.createElement('div');
        card.className = 'news-item';
        card.innerHTML = `
            <div class="news-item-header">
                <h4>${escapeHtml(item.title)}</h4>
                <span class="news-item-date">${formattedDate}</span>
            </div>
            <p>${escapeHtml(item.content)}</p>
        `;
        newsList.appendChild(card);
    });
}

async function handlePublishNews(e) {
    e.preventDefault();
    const title = document.getElementById('news-title').value;
    const content = document.getElementById('news-content').value;

    btnPublishNews.disabled = true;
    btnPublishNews.textContent = 'Publishing...';

    try {
        const response = await fetch('/api/news', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ title, content })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Failed to publish news.');
        }

        showToast('Announcement published successfully!', 'success');
        newsForm.reset();
        loadNews();

    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btnPublishNews.disabled = false;
        btnPublishNews.textContent = 'Publish Announcement';
    }
}

// XSS prevention helper
function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Add Employee registration helper
async function handleRegisterEmployee(e) {
    e.preventDefault();
    const name = document.getElementById('emp-name').value;
    const email = document.getElementById('emp-email').value;
    const password = document.getElementById('emp-password').value;
    const mobile = document.getElementById('emp-mobile').value;
    const adminSecret = document.getElementById('emp-secret').value;
    const btnRegister = document.getElementById('btn-register-employee');

    btnRegister.disabled = true;
    btnRegister.textContent = 'Registering...';

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name: name.trim(),
                email: email.trim(),
                password: password,
                role: 'ADMIN', // Employee role
                mobile: mobile.trim(),
                adminSecret: adminSecret.trim()
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Registration failed.');
        }

        showToast('Employee account created successfully!', 'success');
        document.getElementById('employee-register-form').reset();

    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btnRegister.disabled = false;
        btnRegister.textContent = 'Create Employee Account';
    }
}
