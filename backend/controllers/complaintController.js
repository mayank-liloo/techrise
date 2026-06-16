const { admin, db } = require('../config/firebase');

// 1. Create a new complaint (Customers only)
const createComplaint = async (req, res) => {
  try {
    const { title, description, priority } = req.body;
    const customerId = req.user.userId;

    if (!title || !title.trim() || !description || !description.trim()) {
      return res.status(400).json({ error: 'Title and description are required.' });
    }

    const validPriorities = ['LOW', 'MEDIUM', 'HIGH'];
    const selectedPriority = priority && validPriorities.includes(priority.toUpperCase()) 
      ? priority.toUpperCase() 
      : 'MEDIUM';

    // Create complaint document
    const complaintData = {
      title: title.trim(),
      description: description.trim(),
      status: 'PENDING',
      priority: selectedPriority,
      customerId: customerId,
      assignedAdminId: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };

    const docRef = await db.collection('complaints').add(complaintData);

    // Create audit log record
    await db.collection('complaintLogs').add({
      complaintId: docRef.id,
      actionBy: customerId,
      oldStatus: 'NONE',
      newStatus: 'PENDING',
      comment: 'Complaint raised by customer.',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(201).json({
      message: 'Complaint raised successfully.',
      complaintId: docRef.id,
      ...complaintData
    });

  } catch (err) {
    console.error('Create Complaint Error:', err);
    return res.status(500).json({ error: 'Server error while creating complaint.' });
  }
};

// 2. Get complaints list (Admins see all, Customers see only theirs)
const getComplaints = async (req, res) => {
  try {
    const { role, userId } = req.user;
    let query;

    if (role.toUpperCase() === 'ADMIN') {
      // Admins see all complaints
      query = db.collection('complaints');
    } else {
      // Customers only see their own complaints
      query = db.collection('complaints').where('customerId', '==', userId);
    }

    const snapshot = await query.get();
    const complaints = [];
    snapshot.forEach(doc => {
      complaints.push({ id: doc.id, ...doc.data() });
    });

    // Sort in-memory by createdAt descending
    complaints.sort((a, b) => {
      const timeA = a.createdAt ? (a.createdAt.seconds || 0) : 0;
      const timeB = b.createdAt ? (b.createdAt.seconds || 0) : 0;
      return timeB - timeA;
    });

    return res.status(200).json(complaints);
  } catch (err) {
    console.error('Get Complaints Error:', err);
    return res.status(500).json({ error: 'Server error while fetching complaints.' });
  }
};

// 3. Get single complaint details (Access restricted to owner customer or admin)
const getComplaintById = async (req, res) => {
  try {
    const { id } = req.params;
    const { role, userId } = req.user;

    const docRef = db.collection('complaints').doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return res.status(404).json({ error: 'Complaint not found.' });
    }

    const complaintData = doc.data();

    // Security Check: Customers can only view their own complaints
    if (role.toUpperCase() === 'CUSTOMER' && complaintData.customerId !== userId) {
      return res.status(403).json({ error: 'Access Denied: You cannot view this complaint.' });
    }

    return res.status(200).json({ id: doc.id, ...complaintData });
  } catch (err) {
    console.error('Get Complaint By ID Error:', err);
    return res.status(500).json({ error: 'Server error while fetching complaint details.' });
  }
};

// 4. Update complaint status (Admins only)
const updateComplaintStatus = async (req, res) => {
  try {
    const { id } = req.params;
    const { status, comment } = req.body;
    const adminId = req.user.userId;

    if (!status || !['PENDING', 'IN_PROGRESS', 'RESOLVED'].includes(status.toUpperCase())) {
      return res.status(400).json({ error: 'Invalid status. Choose PENDING, IN_PROGRESS, or RESOLVED.' });
    }

    const docRef = db.collection('complaints').doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return res.status(404).json({ error: 'Complaint not found.' });
    }

    const complaintData = doc.data();
    const oldStatus = complaintData.status;
    const newStatus = status.toUpperCase();

    // Update complaint record
    await docRef.update({
      status: newStatus,
      assignedAdminId: adminId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Write transition to logs collection
    await db.collection('complaintLogs').add({
      complaintId: id,
      actionBy: adminId,
      oldStatus: oldStatus,
      newStatus: newStatus,
      comment: comment || `Status updated to ${newStatus}.`,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(200).json({
      message: 'Complaint status updated successfully.',
      complaintId: id,
      oldStatus,
      newStatus,
      assignedAdminId: adminId
    });

  } catch (err) {
    console.error('Update Status Error:', err);
    return res.status(500).json({ error: 'Server error while updating complaint status.' });
  }
};

// 5. Get audit logs of a complaint (Owner customer or admin only)
const getComplaintLogs = async (req, res) => {
  try {
    const { id } = req.params;
    const { role, userId } = req.user;

    // Verify complaint existence and access permission first
    const complaintDoc = await db.collection('complaints').doc(id).get();
    if (!complaintDoc.exists) {
      return res.status(404).json({ error: 'Complaint not found.' });
    }

    const complaintData = complaintDoc.data();
    if (role.toUpperCase() === 'CUSTOMER' && complaintData.customerId !== userId) {
      return res.status(403).json({ error: 'Access Denied: You cannot view this audit trail.' });
    }

    const logsSnapshot = await db.collection('complaintLogs')
      .where('complaintId', '==', id)
      .get();

    const logs = [];
    logsSnapshot.forEach(doc => {
      logs.push({ id: doc.id, ...doc.data() });
    });

    // Sort in-memory by createdAt ascending
    logs.sort((a, b) => {
      const timeA = a.createdAt ? (a.createdAt.seconds || 0) : 0;
      const timeB = b.createdAt ? (b.createdAt.seconds || 0) : 0;
      return timeA - timeB;
    });

    return res.status(200).json(logs);
  } catch (err) {
    console.error('Get Logs Error:', err);
    return res.status(500).json({ error: 'Server error while fetching logs.' });
  }
};

module.exports = {
  createComplaint,
  getComplaints,
  getComplaintById,
  updateComplaintStatus,
  getComplaintLogs
};
