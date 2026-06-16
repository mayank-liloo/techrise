const express = require('express');
const router = express.Router();

const { 
  createComplaint, 
  getComplaints, 
  getComplaintById, 
  updateComplaintStatus, 
  getComplaintLogs 
} = require('../controllers/complaintController');

const { authenticateToken, requireRole } = require('../middleware/authMiddleware');

// 1. Raise a Complaint (Customer only)
router.post('/', authenticateToken, requireRole(['CUSTOMER']), createComplaint);

// 2. View All/Own Complaints (Customer sees own, Admin sees all)
router.get('/', authenticateToken, getComplaints);

// 3. View Single Complaint Detail (Customer owner or Admin)
router.get('/:id', authenticateToken, getComplaintById);

// 4. Update Complaint Status (Admin only)
router.put('/:id/status', authenticateToken, requireRole(['ADMIN']), updateComplaintStatus);

// 5. Get Complaint Audit Logs (Customer owner or Admin)
router.get('/:id/logs', authenticateToken, getComplaintLogs);

module.exports = router;
