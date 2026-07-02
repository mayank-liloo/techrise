const jwt = require('jsonwebtoken');
const { db } = require('../config/firebase');

// Middleware to authenticate JWT token
const authenticateToken = async (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Extract token from "Bearer <token>"

  if (!token) {
    return res.status(401).json({ error: 'Access Denied: No authentication token provided.' });
  }

  try {
    const verified = jwt.verify(token, process.env.JWT_SECRET);
    req.user = verified; // Contains userId, email, role, and sessionId

    // Enforce single active session for ADMIN users
    if (verified.role === 'ADMIN') {
      const userDoc = await db.collection('users').doc(verified.userId).get();
      if (!userDoc.exists) {
        return res.status(401).json({ error: 'Access Denied: User profile not found.' });
      }
      const userData = userDoc.data();
      if (userData.activeSessionId && userData.activeSessionId !== verified.sessionId) {
        return res.status(401).json({ error: 'Session expired: You have logged in from another device/browser.' });
      }
    }

    next();
  } catch (err) {
    return res.status(403).json({ error: 'Access Denied: Invalid or expired authentication token.' });
  }
};

// Middleware to restrict access based on user roles
const requireRole = (allowedRoles) => {
  return (req, res, next) => {
    if (!req.user || !req.user.role) {
      return res.status(403).json({ error: 'Access Denied: No role assigned.' });
    }

    const hasRole = allowedRoles.map(r => r.toUpperCase()).includes(req.user.role.toUpperCase());
    if (!hasRole) {
      return res.status(403).json({ error: 'Access Denied: You do not have the required permissions.' });
    }

    next();
  };
};

module.exports = {
  authenticateToken,
  requireRole
};
