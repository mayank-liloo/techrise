const jwt = require('jsonwebtoken');
const { db } = require('../config/firebase');
require('dotenv').config();

async function generateTokens() {
  if (!db) {
    console.error('Firebase DB instance is not loaded. Ensure serviceAccountKey.json is in your config/ directory.');
    process.exit(1);
  }

  try {
    // 1. Generate Customer Token
    const customerEmail = 'customer@test.com';
    const customerQuery = await db.collection('users').where('email', '==', customerEmail).limit(1).get();
    
    if (customerQuery.empty) {
      console.log(`User ${customerEmail} not found. Please register the user first.`);
    } else {
      const doc = customerQuery.docs[0];
      const data = doc.data();
      const payload = {
        userId: doc.id,
        email: data.email,
        role: data.role
      };
      const token = jwt.sign(payload, process.env.JWT_SECRET, { expiresIn: '1h' });
      console.log('\n==================================================================');
      console.log('CUSTOMER JWT TOKEN:');
      console.log(token);
      console.log('==================================================================');
    }

    // 2. Generate Admin Token
    const adminEmail = 'admin@test.com';
    const adminQuery = await db.collection('users').where('email', '==', adminEmail).limit(1).get();
    
    if (adminQuery.empty) {
      console.log(`User ${adminEmail} not found. Please register the user first.`);
    } else {
      const doc = adminQuery.docs[0];
      const data = doc.data();
      const payload = {
        userId: doc.id,
        email: data.email,
        role: data.role
      };
      const token = jwt.sign(payload, process.env.JWT_SECRET, { expiresIn: '1h' });
      console.log('\n==================================================================');
      console.log('ADMIN JWT TOKEN:');
      console.log(token);
      console.log('==================================================================\n');
    }

    process.exit(0);
  } catch (err) {
    console.error('Error generating tokens:', err.message);
    process.exit(1);
  }
}

generateTokens();
