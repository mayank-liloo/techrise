const express = require('express');
const router = express.Router();

const { register, login } = require('../controllers/authController');
const { validateRegistration, validateLogin } = require('../middleware/validation');

// Registration Route
router.post('/register', validateRegistration, register);

// Login Route
router.post('/login', validateLogin, login);

module.exports = router;
