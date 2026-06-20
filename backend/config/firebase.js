const admin = require('firebase-admin');
const path = require('path');
require('dotenv').config();

// Define service account credential path (defaults to config/serviceAccountKey.json)
const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_KEY 
  ? path.resolve(process.env.FIREBASE_SERVICE_ACCOUNT_KEY) 
  : path.join(__dirname, 'serviceAccountKey.json');

let db;

try {
  let credential;
  if (process.env.FIREBASE_CREDENTIALS) {
    try {
      credential = admin.credential.cert(JSON.parse(process.env.FIREBASE_CREDENTIALS));
    } catch (parseErr) {
      // Fallback if it points to a path instead of inline JSON
      credential = admin.credential.cert(require(path.resolve(process.env.FIREBASE_CREDENTIALS)));
    }
  } else {
    credential = admin.credential.cert(require(serviceAccountPath));
  }

  admin.initializeApp({ credential });
  db = admin.firestore();
  db.settings({ ignoreUndefinedProperties: true });
  console.log('Firebase Admin SDK initialized successfully.');
} catch (err) {
  console.error('\n==================================================================');
  console.error('WARNING: Firebase Service Account credentials missing or invalid.');
  console.error('Ensure either serviceAccountKey.json is present or FIREBASE_CREDENTIALS env is configured.');
  console.error('Error Details:', err.message);
  console.error('==================================================================\n');
}

module.exports = {
  admin,
  db
};
