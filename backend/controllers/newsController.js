const { admin, db } = require('../config/firebase');

// Create a news article (Admin only)
const createNews = async (req, res) => {
  try {
    const { title, content } = req.body;
    const authorId = req.user.userId;

    if (!title || !title.trim()) {
      return res.status(400).json({ error: 'News title is required.' });
    }
    if (!content || !content.trim()) {
      return res.status(400).json({ error: 'News content is required.' });
    }

    const newsRef = await db.collection('news').add({
      title: title.trim(),
      content: content.trim(),
      authorId,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(201).json({
      message: 'News published successfully.',
      newsId: newsRef.id,
      title,
      authorId
    });

  } catch (err) {
    console.error('Create News Error:', err);
    return res.status(500).json({ error: 'Server error while publishing news.' });
  }
};

// Get all news articles
const getNews = async (req, res) => {
  try {
    const newsSnapshot = await db.collection('news').get();
    
    const newsList = [];
    newsSnapshot.forEach(doc => {
      const data = doc.data();
      newsList.push({
        id: doc.id,
        title: data.title,
        content: data.content,
        authorId: data.authorId,
        createdAt: data.createdAt
      });
    });

    // Sort in memory (descending order by date)
    newsList.sort((a, b) => {
      const secA = a.createdAt ? a.createdAt._seconds : 0;
      const secB = b.createdAt ? b.createdAt._seconds : 0;
      return secB - secA;
    });

    return res.status(200).json(newsList);

  } catch (err) {
    console.error('Get News Error:', err);
    return res.status(500).json({ error: 'Server error while fetching news.' });
  }
};

// Delete a news article (Admin only)
const deleteNews = async (req, res) => {
  try {
    const { id } = req.params;

    const newsDoc = await db.collection('news').doc(id).get();
    if (!newsDoc.exists) {
      return res.status(404).json({ error: 'News bulletin not found.' });
    }

    await db.collection('news').doc(id).delete();

    return res.status(200).json({ message: 'News bulletin deleted successfully.' });

  } catch (err) {
    console.error('Delete News Error:', err);
    return res.status(500).json({ error: 'Server error while deleting news.' });
  }
};

module.exports = {
  createNews,
  getNews,
  deleteNews
};
