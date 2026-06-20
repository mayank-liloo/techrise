const express = require('express');
const router = express.Router();

const { register, login, getEmployees } = require('../controllers/authController');
const { validateRegistration, validateLogin } = require('../middleware/validation');
const { authenticateToken } = require('../middleware/authMiddleware');

// Registration Route
router.post('/register', validateRegistration, register);

// Login Route
router.post('/login', validateLogin, login);

// Get Employees Route (Admins only)
router.get('/employees', authenticateToken, getEmployees);

module.exports = router;
