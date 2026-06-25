const jwt = require('jsonwebtoken');

const validateRegistration = (req, res, next) => {
  const { email, password, role, adminSecret, name } = req.body;

  // Validate Name
  if (!name || !name.trim()) {
    return res.status(400).json({ error: 'Name is required.' });
  }

  // Validate Email
  if (!email || !email.trim()) {
    return res.status(400).json({ error: 'Email is required.' });
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return res.status(400).json({ error: 'Invalid email format.' });
  }

  // Validate Password
  if (!password || password.length < 6) {
    return res.status(400).json({ error: 'Password must be at least 6 characters long.' });
  }

  // Validate Role
  if (!role || !['CUSTOMER', 'ADMIN'].includes(role.toUpperCase())) {
    return res.status(400).json({ error: 'Role must be either CUSTOMER or ADMIN.' });
  }

  // Admin Security Check: If attempting to register as Admin, verify registration secret
  if (role.toUpperCase() === 'ADMIN') {
    let isAuthorizedAdmin = false;
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    
    if (token) {
      try {
        const verified = jwt.verify(token, process.env.JWT_SECRET || 'super_secure_crm_jwt_secret_key_2026_antigravity');
        if (verified && verified.role && verified.role.toUpperCase() === 'ADMIN') {
          isAuthorizedAdmin = true;
        }
      } catch (err) {
        // Token verification failed, fall back to adminSecret check
      }
    }

    if (!isAuthorizedAdmin) {
      const systemAdminSecret = process.env.ADMIN_REGISTRATION_SECRET || 'super_secret_admin_code_2026';
      if (!adminSecret || adminSecret !== systemAdminSecret) {
        return res.status(403).json({ error: 'Unauthorized: Invalid Admin registration secret code.' });
      }
    }
  }

  next();
};

const validateLogin = (req, res, next) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required.' });
  }

  next();
};

module.exports = {
  validateRegistration,
  validateLogin
};
