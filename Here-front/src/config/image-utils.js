import { Client } from 'minio';
import fs from 'fs';
import path from 'path';
import { v4 as uuidv4 } from 'uuid';

// Initialize MinIO client
const minioClient = new Client({
  endPoint: process.env.MINIO_ENDPOINT || 'minio',
  port: parseInt(process.env.MINIO_PORT || '9000'),
  useSSL: process.env.MINIO_USE_SSL === 'true',
  accessKey: process.env.MINIO_ACCESS_KEY || 'minioadmin',
  secretKey: process.env.MINIO_SECRET_KEY || 'minioadmin'
});

const bucketName = process.env.MINIO_BUCKET || 'boutique-images';

// Local upload directory for fallback
const uploadDir = '/app/public/uploads';

// Ensure the upload directory exists
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

/**
 * Upload a file to MinIO and return the URL
 * @param {Object} file - The file to upload (from multer or similar)
 * @returns {Promise<string>} The URL of the uploaded file
 */
export async function uploadToMinio(file) {
  try {
    // Generate a unique filename
    const fileExtension = path.extname(file.originalname);
    const fileName = `${uuidv4()}${fileExtension}`;
    
    // Upload the file
    await minioClient.fPutObject(
      bucketName,
      fileName,
      file.path,
      { 'Content-Type': file.mimetype }
    );
    
    // Return the URL (assuming MinIO is accessible via the specified endpoint)
    return `http://${process.env.MINIO_ENDPOINT}:${process.env.MINIO_PORT}/${bucketName}/${fileName}`;
  } catch (error) {
    console.error('Error uploading to MinIO:', error);
    
    // Fallback to local storage
    return await saveLocally(file);
  }
}

/**
 * Save a file locally and return the URL
 * @param {Object} file - The file to save
 * @returns {Promise<string>} The URL of the saved file
 */
export async function saveLocally(file) {
  try {
    // Generate a unique filename
    const fileExtension = path.extname(file.originalname);
    const fileName = `${uuidv4()}${fileExtension}`;
    const filePath = path.join(uploadDir, fileName);
    
    // Copy the file to the uploads directory
    await fs.promises.copyFile(file.path, filePath);
    
    // Clean up the temporary file
    await fs.promises.unlink(file.path);
    
    // Return the URL (relative to the public folder)
    return `/uploads/${fileName}`;
  } catch (error) {
    console.error('Error saving file locally:', error);
    throw error;
  }
}

/**
 * Initialize the bucket if it doesn't exist
 */
export async function initializeBucket() {
  try {
    const exists = await minioClient.bucketExists(bucketName);
    if (!exists) {
      await minioClient.makeBucket(bucketName);
      // Make the bucket public for ease of access
      const policy = {
        Version: '2012-10-17',
        Statement: [
          {
            Effect: 'Allow',
            Principal: { AWS: ['*'] },
            Action: ['s3:GetObject'],
            Resource: [`arn:aws:s3:::${bucketName}/*`]
          }
        ]
      };
      await minioClient.setBucketPolicy(bucketName, JSON.stringify(policy));
      console.log(`Bucket ${bucketName} created with public access`);
    }
  } catch (error) {
    console.error('Error initializing MinIO bucket:', error);
    // Continue without MinIO if there's an error
  }
}

// Initialize the bucket when the module is imported
initializeBucket().catch(console.error);

export default {
  uploadToMinio,
  saveLocally,
  initializeBucket
};