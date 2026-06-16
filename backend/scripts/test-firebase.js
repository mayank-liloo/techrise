const { db } = require('../config/firebase');

async function testConnection() {
  if (!db) {
    console.error('Firebase DB instance is not loaded. Ensure serviceAccountKey.json is in your config/ directory.');
    process.exit(1);
  }

  try {
    console.log('Testing connection to Firebase Firestore...');
    // Query users collection
    const snapshot = await db.collection('users').limit(1).get();
    console.log('Successfully connected to Firestore!');
    console.log(`Found ${snapshot.size} existing users.`);
    process.exit(0);
  } catch (err) {
    console.error('Error connecting to Firestore database:');
    console.error(err.message);
    process.exit(1);
  }
}

testConnection();
