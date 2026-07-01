const bcrypt = require('bcrypt');
const { db } = require('../config/firebase');
require('dotenv').config();

async function resetAdminPassword() {
  if (!db) {
    console.error('Firebase DB instance is not loaded. Ensure serviceAccountKey.json is in your config/ directory.');
    process.exit(1);
  }

  const targetEmail = 'admin@gmail.com';
  const defaultPassword = 'admin123';

  try {
    console.log(`Checking database for user: ${targetEmail}...`);
    const userQuery = await db.collection('users').where('email', '==', targetEmail).limit(1).get();

    const saltRounds = 10;
    const newHash = await bcrypt.hash(defaultPassword, saltRounds);

    if (userQuery.empty) {
      console.log(`User ${targetEmail} not found. Creating a new ADMIN account...`);
      
      const rolePrefix = 'EMP';
      const snapshot = await db.collection('users').where('role', '==', 'ADMIN').get();
      const count = snapshot.size;
      const cleanId = `${rolePrefix}-${1001 + count}`;

      await db.collection('users').doc(cleanId).set({
        email: targetEmail,
        passwordHash: newHash,
        role: 'ADMIN',
        mobile: '+1234567890',
        name: 'Super Admin',
        createdAt: new Date(),
        updatedAt: new Date()
      });

      console.log(`Successfully created ADMIN account!`);
      console.log(`Email: ${targetEmail}`);
      console.log(`Password: ${defaultPassword}`);
    } else {
      const doc = userQuery.docs[0];
      console.log(`User ${targetEmail} found (ID: ${doc.id}). Resetting password to ${defaultPassword}...`);
      
      await db.collection('users').doc(doc.id).update({
        passwordHash: newHash,
        updatedAt: new Date()
      });

      console.log(`Successfully updated password for ${targetEmail}!`);
    }

    process.exit(0);
  } catch (err) {
    console.error('Error running script:', err.message);
    process.exit(1);
  }
}

resetAdminPassword();
