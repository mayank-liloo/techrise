const express = require('express');
const router = express.Router();

const { createNews, getNews, deleteNews } = require('../controllers/newsController');
const { authenticateToken, requireRole } = require('../middleware/authMiddleware');

// Get all news (Available to both CUSTOMER and ADMIN roles)
router.get('/', authenticateToken, getNews);

// Create news (Restricted to ADMIN/Employee role only)
router.post('/', authenticateToken, requireRole(['ADMIN']), createNews);

// Delete news (Restricted to ADMIN/Employee role only)
router.delete('/:id', authenticateToken, requireRole(['ADMIN']), deleteNews);

module.exports = router;
