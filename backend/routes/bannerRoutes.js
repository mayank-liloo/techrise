const express = require('express');
const router = express.Router();

const { createBanner, getBanners, deleteBanner } = require('../controllers/bannerController');
const { authenticateToken, requireRole } = require('../middleware/authMiddleware');

// Get all banners (Available to both CUSTOMER and ADMIN roles)
router.get('/', authenticateToken, getBanners);

// Create banner (Restricted to ADMIN only)
router.post('/', authenticateToken, requireRole(['ADMIN']), createBanner);

// Delete banner (Restricted to ADMIN only)
router.delete('/:id', authenticateToken, requireRole(['ADMIN']), deleteBanner);

module.exports = router;
