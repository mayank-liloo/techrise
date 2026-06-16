const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { admin, db } = require('../config/firebase');

// User Registration
const register = async (req, res) => {
  try {
    const { email, password, role } = req.body;
    const normalizedEmail = email.toLowerCase().trim();

    // 1. Check if user already exists
    const userQuery = await db.collection('users').where('email', '==', normalizedEmail).limit(1).get();
    if (!userQuery.empty) {
      return res.status(409).json({ error: 'A user with this email already exists.' });
    }

    // 2. Hash Password using bcrypt
    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // 3. Store new user in Firestore
    const userRef = await db.collection('users').add({
      email: normalizedEmail,
      passwordHash,
      role: role.toUpperCase(),
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(201).json({
      message: 'User registered successfully.',
      userId: userRef.id,
      email: normalizedEmail,
      role: role.toUpperCase()
    });

  } catch (err) {
    console.error('Registration Error:', err);
    return res.status(500).json({ error: 'Server error during registration.' });
  }
};

// User Login
const login = async (req, res) => {
  try {
    const { email, password } = req.body;
    const normalizedEmail = email.toLowerCase().trim();

    // 1. Fetch user from Firestore
    const userQuery = await db.collection('users').where('email', '==', normalizedEmail).limit(1).get();
    if (userQuery.empty) {
      // Security Tip: Generic error message to prevent email enumeration attacks
      return res.status(401).json({ error: 'Invalid email or password.' });
    }

    const userDoc = userQuery.docs[0];
    const userData = userDoc.data();

    // 2. Verify password
    const isMatch = await bcrypt.compare(password, userData.passwordHash);
    if (!isMatch) {
      return res.status(401).json({ error: 'Invalid email or password.' });
    }

    // 3. Generate Secure JWT Token
    const payload = {
      userId: userDoc.id,
      email: userData.email,
      role: userData.role
    };

    const token = jwt.sign(payload, process.env.JWT_SECRET, {
      expiresIn: process.env.JWT_EXPIRES_IN || '1h'
    });

    return res.status(200).json({
      message: 'Login successful.',
      token,
      user: {
        id: userDoc.id,
        email: userData.email,
        role: userData.role
      }
    });

  } catch (err) {
    console.error('Login Error:', err);
    return res.status(500).json({ error: 'Server error during login.' });
  }
};

module.exports = {
  register,
  login
};
