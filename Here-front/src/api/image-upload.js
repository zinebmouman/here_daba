// Example API route for image uploads
// This would typically be in your API routes file or similar

import express from 'express';
import multer from 'multer';
import path from 'path';
import { uploadToMinio } from '../config/image-utils.js';
import { pool } from '../config/database.js';

const router = express.Router();

// Set up multer for handling multipart/form-data
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, '/tmp');  // Temporary storage before uploading to MinIO
  },
  filename: function (req, file, cb) {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, file.fieldname + '-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({ 
  storage: storage,
  limits: {
    fileSize: 5 * 1024 * 1024 // Limit file size to 5MB
  },
  fileFilter: function (req, file, cb) {
    // Accept images only
    if (!file.originalname.match(/\.(jpg|jpeg|png|gif)$/)) {
      return cb(new Error('Only image files are allowed!'), false);
    }
    cb(null, true);
  }
});

// Route for uploading product images
router.post('/products/:productId/images', upload.single('image'), async (req, res) => {
  try {
    const { productId } = req.params;
    
    if (!req.file) {
      return res.status(400).json({ error: 'No file uploaded' });
    }
    
    // Upload to MinIO and get the URL
    const imageUrl = await uploadToMinio(req.file);
    
    // Save the URL in the database
    const client = await pool.connect();
    try {
      // Insert the image record
      const result = await client.query(
        'INSERT INTO images (id_image, nom, url, ordre, id_produit) VALUES ($1, $2, $3, $4, $5) RETURNING *',
        [
          // Generate a unique ID (you could use uuid here)
          `img_${Date.now()}`,
          req.file.originalname,
          imageUrl,
          req.body.order || '1', // Default order
          productId
        ]
      );
      
      res.status(201).json({
        success: true,
        image: result.rows[0]
      });
    } finally {
      client.release();
    }
  } catch (error) {
    console.error('Error uploading image:', error);
    res.status(500).json({ error: 'Failed to upload image' });
  }
});

// Route for getting product images
router.get('/products/:productId/images', async (req, res) => {
  try {
    const { productId } = req.params;
    
    const client = await pool.connect();
    try {
      const result = await client.query(
        'SELECT * FROM images WHERE id_produit = $1 ORDER BY ordre',
        [productId]
      );
      
      res.json({
        success: true,
        images: result.rows
      });
    } finally {
      client.release();
    }
  } catch (error) {
    console.error('Error fetching images:', error);
    res.status(500).json({ error: 'Failed to fetch images' });
  }
});

export default router;