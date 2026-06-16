const admin = require('firebase-admin');
const path = require('path');
require('dotenv').config();

// Define service account credential path (defaults to config/serviceAccountKey.json)
const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_KEY 
  ? path.resolve(process.env.FIREBASE_SERVICE_ACCOUNT_KEY) 
  : path.join(__dirname, 'serviceAccountKey.json');

let db;

try {
  admin.initializeApp({
    credential: admin.credential.cert(require(serviceAccountPath))
  });
  db = admin.firestore();
  // Safe setting to ignore undefined fields (prevents crashes during document write)
  db.settings({ ignoreUndefinedProperties: true });
  console.log('Firebase Admin SDK initialized successfully.');
} catch (err) {
  console.error('\n==================================================================');
  console.error('WARNING: Firebase Service Account credentials missing or invalid.');
  console.error(`Expected JSON file at: ${serviceAccountPath}`);
  console.error('Please place your downloaded serviceAccountKey.json at that path.');
  console.error('Error Details:', err.message);
  console.error('==================================================================\n');
}

module.exports = {
  admin,
  db
};
